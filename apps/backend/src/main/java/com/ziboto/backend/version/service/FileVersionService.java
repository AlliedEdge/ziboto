package com.ziboto.backend.version.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.file.service.StorageService;
import com.ziboto.backend.version.dto.CreateVersionRequest;
import com.ziboto.backend.version.dto.FileVersionResponse;
import com.ziboto.backend.version.dto.VersionCompareResponse;
import com.ziboto.backend.version.entity.FileVersion;
import com.ziboto.backend.version.repository.FileVersionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing file versions.
 * 
 * <p>Handles:</p>
 * <ul>
 *   <li>Creating new versions when files are updated</li>
 *   <li>Retrieving version history</li>
 *   <li>Restoring previous versions</li>
 *   <li>Comparing versions</li>
 *   <li>Version retention policies</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileVersionService {
    
    private final FileVersionRepository versionRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final StorageService storageService;
    
    private static final int DEFAULT_MAX_VERSIONS = 50; // Keep last 50 versions
    private static final int DEFAULT_RETENTION_DAYS = 90; // Keep versions for 90 days
    
    // -------------------------------------------------------------------------
    // Version Creation
    // -------------------------------------------------------------------------
    
    /**
     * Create a new version by uploading a new file.
     * Called when user updates an existing file.
     */
    @Transactional
    public FileVersionResponse createNewVersion(
            UUID fileId,
            Long userId,
            MultipartFile file,
            CreateVersionRequest request,
            String username) {
        
        log.info("Creating new version: fileId={}, userId={}", fileId, userId);
        
        // Verify file ownership
        FileMetadata currentFile = fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // Get next version number
        Long versionCount = versionRepository.countVersionsByFileId(fileId);
        int nextVersionNumber = versionCount.intValue() + 1;
        
        try {
            // Upload new version to storage (using version-specific key)
            String versionStorageKey = storageService.uploadFile(userId, fileId, file);
            
            // Calculate SHA-256 hash
            String sha256Hash = calculateSHA256(file);
            
            // Create version record
            FileVersion version = FileVersion.builder()
                    .fileId(fileId)
                    .userId(userId)
                    .versionNumber(nextVersionNumber)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .sha256Hash(sha256Hash)
                    .storageKey(versionStorageKey)
                    .storageLocation("S3")
                    .changeDescription(request != null ? request.getChangeDescription() : null)
                    .versionTag(request != null ? request.getVersionTag() : null)
                    .createdBy(username)
                    .build();
            
            version = versionRepository.save(version);
            log.info("New version created: versionId={}, versionNumber={}", version.getId(), nextVersionNumber);
            
            // Update current file metadata to point to latest version
            currentFile.setFileName(file.getOriginalFilename());
            currentFile.setFileSize(file.getSize());
            currentFile.setMimeType(file.getContentType());
            currentFile.setSha256Hash(sha256Hash);
            currentFile.setStorageKey(versionStorageKey);
            currentFile.setLastModifiedBy(username);
            fileMetadataRepository.save(currentFile);
            
            return buildVersionResponse(version, true, false);
            
        } catch (Exception e) {
            log.error("Failed to create new version", e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to create new version: " + e.getMessage());
        }
    }
    
    /**
     * Create version snapshot from existing file (manual versioning).
     */
    @Transactional
    public FileVersionResponse createVersionSnapshot(
            UUID fileId,
            Long userId,
            CreateVersionRequest request,
            String username) {
        
        log.info("Creating version snapshot: fileId={}, userId={}", fileId, userId);
        
        // Get current file
        FileMetadata currentFile = fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // Get next version number
        Long versionCount = versionRepository.countVersionsByFileId(fileId);
        int nextVersionNumber = versionCount.intValue() + 1;
        
        // Create version from current file state
        FileVersion version = FileVersion.builder()
                .fileId(fileId)
                .userId(userId)
                .versionNumber(nextVersionNumber)
                .fileName(currentFile.getFileName())
                .fileSize(currentFile.getFileSize())
                .mimeType(currentFile.getMimeType())
                .sha256Hash(currentFile.getSha256Hash())
                .storageKey(currentFile.getStorageKey())
                .storageLocation("S3")
                .changeDescription(request != null ? request.getChangeDescription() : "Manual snapshot")
                .versionTag(request != null ? request.getVersionTag() : null)
                .createdBy(username)
                .build();
        
        version = versionRepository.save(version);
        log.info("Version snapshot created: versionId={}, versionNumber={}", version.getId(), nextVersionNumber);
        
        return buildVersionResponse(version, true, false);
    }
    
    // -------------------------------------------------------------------------
    // Version Retrieval
    // -------------------------------------------------------------------------
    
    /**
     * Get version history for a file.
     */
    @Transactional(readOnly = true)
    public Page<FileVersionResponse> getVersionHistory(UUID fileId, Long userId, Pageable pageable) {
        // Verify file ownership
        fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        Page<FileVersion> versions = versionRepository.findByFileId(fileId, pageable);
        
        // Mark latest version
        Optional<FileVersion> latestVersion = versionRepository.findLatestVersionByFileId(fileId);
        UUID latestVersionId = latestVersion.map(FileVersion::getId).orElse(null);
        
        return versions.map(v -> buildVersionResponse(v, v.getId().equals(latestVersionId), v.isInitialVersion()));
    }
    
    /**
     * Get specific version.
     */
    @Transactional(readOnly = true)
    public FileVersionResponse getVersion(UUID fileId, Integer versionNumber, Long userId) {
        // Verify file ownership
        fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        FileVersion version = versionRepository.findByFileIdAndVersionNumber(fileId, versionNumber)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Version not found"));
        
        Optional<FileVersion> latestVersion = versionRepository.findLatestVersionByFileId(fileId);
        boolean isLatest = latestVersion.map(v -> v.getId().equals(version.getId())).orElse(false);
        
        return buildVersionResponse(version, isLatest, version.isInitialVersion());
    }
    
    /**
     * Get latest version.
     */
    @Transactional(readOnly = true)
    public FileVersionResponse getLatestVersion(UUID fileId, Long userId) {
        // Verify file ownership
        fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        FileVersion version = versionRepository.findLatestVersionByFileId(fileId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "No versions found"));
        
        return buildVersionResponse(version, true, version.isInitialVersion());
    }
    
    // -------------------------------------------------------------------------
    // Version Restoration
    // -------------------------------------------------------------------------
    
    /**
     * Restore a previous version (creates new version with old content).
     */
    @Transactional
    public FileVersionResponse restoreVersion(UUID fileId, Integer versionNumber, Long userId, String username) {
        log.info("Restoring version: fileId={}, versionNumber={}, userId={}", fileId, versionNumber, userId);
        
        // Get current file
        FileMetadata currentFile = fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // Get version to restore
        FileVersion versionToRestore = versionRepository.findByFileIdAndVersionNumber(fileId, versionNumber)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Version not found"));
        
        // Get next version number
        Long versionCount = versionRepository.countVersionsByFileId(fileId);
        int nextVersionNumber = versionCount.intValue() + 1;
        
        // Create new version with restored content
        FileVersion restoredVersion = FileVersion.builder()
                .fileId(fileId)
                .userId(userId)
                .versionNumber(nextVersionNumber)
                .fileName(versionToRestore.getFileName())
                .fileSize(versionToRestore.getFileSize())
                .mimeType(versionToRestore.getMimeType())
                .sha256Hash(versionToRestore.getSha256Hash())
                .storageKey(versionToRestore.getStorageKey()) // Reuse same storage key (deduplication)
                .storageLocation(versionToRestore.getStorageLocation())
                .changeDescription("Restored from version " + versionNumber)
                .versionTag("restored-v" + versionNumber)
                .createdBy(username)
                .build();
        
        restoredVersion = versionRepository.save(restoredVersion);
        
        // Update current file to point to restored version
        currentFile.setFileName(versionToRestore.getFileName());
        currentFile.setFileSize(versionToRestore.getFileSize());
        currentFile.setMimeType(versionToRestore.getMimeType());
        currentFile.setSha256Hash(versionToRestore.getSha256Hash());
        currentFile.setStorageKey(versionToRestore.getStorageKey());
        currentFile.setLastModifiedBy(username);
        fileMetadataRepository.save(currentFile);
        
        log.info("Version restored: versionId={}, restoredFrom={}", restoredVersion.getId(), versionNumber);
        
        return buildVersionResponse(restoredVersion, true, false);
    }
    
    // -------------------------------------------------------------------------
    // Version Comparison
    // -------------------------------------------------------------------------
    
    /**
     * Compare two versions.
     */
    @Transactional(readOnly = true)
    public VersionCompareResponse compareVersions(
            UUID fileId,
            Integer oldVersionNumber,
            Integer newVersionNumber,
            Long userId) {
        
        // Verify file ownership
        fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // Get versions
        FileVersion oldVersion = versionRepository.findByFileIdAndVersionNumber(fileId, oldVersionNumber)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Old version not found"));
        
        FileVersion newVersion = versionRepository.findByFileIdAndVersionNumber(fileId, newVersionNumber)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "New version not found"));
        
        // Compare
        boolean contentChanged = !oldVersion.getSha256Hash().equals(newVersion.getSha256Hash());
        boolean nameChanged = !oldVersion.getFileName().equals(newVersion.getFileName());
        boolean sizeChanged = !oldVersion.getFileSize().equals(newVersion.getFileSize());
        
        long sizeDiff = newVersion.getFileSize() - oldVersion.getFileSize();
        long daysBetween = ChronoUnit.DAYS.between(oldVersion.getCreatedAt(), newVersion.getCreatedAt());
        
        return VersionCompareResponse.builder()
                .oldVersion(buildVersionInfo(oldVersion))
                .newVersion(buildVersionInfo(newVersion))
                .contentChanged(contentChanged)
                .nameChanged(nameChanged)
                .sizeChanged(sizeChanged)
                .sizeDifference(sizeDiff)
                .daysBetween(daysBetween)
                .build();
    }
    
    // -------------------------------------------------------------------------
    // Version Management
    // -------------------------------------------------------------------------
    
    /**
     * Apply retention policy to file versions.
     * Keeps last N versions and deletes older ones.
     */
    @Transactional
    public void applyRetentionPolicy(UUID fileId, Long userId, int maxVersionsToKeep) {
        log.info("Applying retention policy: fileId={}, maxVersions={}", fileId, maxVersionsToKeep);
        
        // Verify file ownership
        fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // Get versions to delete
        List<FileVersion> versionsToDelete = versionRepository.findVersionsToDelete(fileId, maxVersionsToKeep);
        
        if (!versionsToDelete.isEmpty()) {
            // Delete old versions from storage
            for (FileVersion version : versionsToDelete) {
                try {
                    storageService.deleteFile(version.getStorageKey());
                } catch (Exception e) {
                    log.error("Failed to delete version from storage: {}", version.getStorageKey(), e);
                }
            }
            
            // Delete version records
            versionRepository.deleteAll(versionsToDelete);
            log.info("Deleted {} old versions for fileId={}", versionsToDelete.size(), fileId);
        }
    }
    
    /**
     * Delete old versions based on age.
     */
    @Transactional
    public void deleteOldVersions(UUID fileId, Long userId, int daysToKeep) {
        log.info("Deleting old versions: fileId={}, daysToKeep={}", fileId, daysToKeep);
        
        // Verify file ownership
        fileMetadataRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<FileVersion> oldVersions = versionRepository.findOldVersionsByFileId(fileId, cutoffDate);
        
        if (!oldVersions.isEmpty()) {
            // Always keep at least the latest version
            Optional<FileVersion> latestVersion = versionRepository.findLatestVersionByFileId(fileId);
            oldVersions.removeIf(v -> latestVersion.isPresent() && v.getId().equals(latestVersion.get().getId()));
            
            // Delete from storage and database
            for (FileVersion version : oldVersions) {
                try {
                    storageService.deleteFile(version.getStorageKey());
                } catch (Exception e) {
                    log.error("Failed to delete version from storage: {}", version.getStorageKey(), e);
                }
            }
            
            versionRepository.deleteAll(oldVersions);
            log.info("Deleted {} old versions for fileId={}", oldVersions.size(), fileId);
        }
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    private FileVersionResponse buildVersionResponse(FileVersion version, boolean isLatest, boolean isInitial) {
        return FileVersionResponse.builder()
                .id(version.getId())
                .fileId(version.getFileId())
                .versionNumber(version.getVersionNumber())
                .versionLabel(version.getVersionLabel())
                .fileName(version.getFileName())
                .fileSize(version.getFileSize())
                .formattedFileSize(version.getFormattedFileSize())
                .mimeType(version.getMimeType())
                .fileExtension(version.getFileExtension())
                .sha256Hash(version.getSha256Hash())
                .changeDescription(version.getChangeDescription())
                .versionTag(version.getVersionTag())
                .storageKey(version.getStorageKey())
                .storageLocation(version.getStorageLocation())
                .createdAt(version.getCreatedAt())
                .createdBy(version.getCreatedBy())
                .isLatest(isLatest)
                .isInitialVersion(isInitial)
                .build();
    }
    
    private VersionCompareResponse.VersionInfo buildVersionInfo(FileVersion version) {
        return VersionCompareResponse.VersionInfo.builder()
                .versionNumber(version.getVersionNumber())
                .versionLabel(version.getVersionLabel())
                .fileName(version.getFileName())
                .fileSize(version.getFileSize())
                .formattedFileSize(version.getFormattedFileSize())
                .sha256Hash(version.getSha256Hash())
                .createdAt(version.getCreatedAt())
                .changeDescription(version.getChangeDescription())
                .build();
    }
    
    private String calculateSHA256(MultipartFile file) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            try (java.io.InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error calculating SHA-256", e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to calculate file hash");
        }
    }
}
