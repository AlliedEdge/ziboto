package com.ziboto.backend.duplicate.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.duplicate.dto.DuplicateGroupResponse;
import com.ziboto.backend.duplicate.dto.DuplicateStatsResponse;
import com.ziboto.backend.duplicate.entity.DuplicateFile;
import com.ziboto.backend.duplicate.entity.DuplicateGroup;
import com.ziboto.backend.duplicate.repository.DuplicateFileRepository;
import com.ziboto.backend.duplicate.repository.DuplicateGroupRepository;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.file.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for duplicate file detection and management.
 * 
 * <p>Handles:</p>
 * <ul>
 *   <li>Scanning for duplicate files</li>
 *   <li>Grouping duplicates by content hash</li>
 *   <li>Managing duplicate files</li>
 *   <li>Calculating storage savings</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {
    
    private final DuplicateGroupRepository groupRepository;
    private final DuplicateFileRepository fileRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileService fileService;
    private final JdbcTemplate jdbcTemplate;
    
    // -------------------------------------------------------------------------
    // Duplicate Detection
    // -------------------------------------------------------------------------
    
    /**
     * Scan all files for duplicates.
     * Uses database function for efficiency.
     */
    @Transactional
    public void scanAllDuplicates() {
        log.info("Starting full duplicate scan");
        
        try {
            jdbcTemplate.execute("SELECT * FROM scan_all_duplicates()");
            log.info("Full duplicate scan completed successfully");
        } catch (Exception e) {
            log.error("Failed to scan duplicates", e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Failed to scan for duplicates: " + e.getMessage());
        }
    }
    
    /**
     * Detect duplicates for specific content hash.
     */
    @Transactional
    public UUID detectDuplicatesByHash(String contentHash) {
        log.info("Detecting duplicates for hash: {}", contentHash);
        
        try {
            UUID groupId = jdbcTemplate.queryForObject(
                    "SELECT detect_duplicates_by_hash(?)",
                    UUID.class,
                    contentHash
            );
            
            log.info("Duplicate detection completed: hash={}, groupId={}", contentHash, groupId);
            return groupId;
        } catch (Exception e) {
            log.error("Failed to detect duplicates for hash: {}", contentHash, e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, 
                    "Failed to detect duplicates: " + e.getMessage());
        }
    }
    
    // -------------------------------------------------------------------------
    // Duplicate Groups
    // -------------------------------------------------------------------------
    
    /**
     * Get all duplicate groups.
     */
    @Transactional(readOnly = true)
    public Page<DuplicateGroupResponse> getAllDuplicateGroups(Pageable pageable) {
        Page<DuplicateGroup> groups = groupRepository.findAllOrderedBySavings(pageable);
        return groups.map(this::buildGroupResponse);
    }
    
    /**
     * Get unreviewed duplicate groups.
     */
    @Transactional(readOnly = true)
    public Page<DuplicateGroupResponse> getUnreviewedGroups(Pageable pageable) {
        Page<DuplicateGroup> groups = groupRepository.findUnreviewed(pageable);
        return groups.map(this::buildGroupResponse);
    }
    
    /**
     * Get specific duplicate group.
     */
    @Transactional(readOnly = true)
    public DuplicateGroupResponse getDuplicateGroup(UUID groupId) {
        DuplicateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Duplicate group not found"));
        
        return buildGroupResponse(group);
    }
    
    /**
     * Get duplicate groups for a user.
     */
    @Transactional(readOnly = true)
    public List<DuplicateGroupResponse> getUserDuplicateGroups(Long userId) {
        List<DuplicateFile> userFiles = fileRepository.findByUserId(userId);
        
        // Get unique group IDs
        List<UUID> groupIds = userFiles.stream()
                .map(DuplicateFile::getGroupId)
                .distinct()
                .collect(Collectors.toList());
        
        // Get groups
        return groupIds.stream()
                .map(groupId -> groupRepository.findById(groupId).orElse(null))
                .filter(group -> group != null)
                .map(this::buildGroupResponse)
                .collect(Collectors.toList());
    }
    
    // -------------------------------------------------------------------------
    // Duplicate Management
    // -------------------------------------------------------------------------
    
    /**
     * Mark duplicates for deletion (keep original).
     */
    @Transactional
    public void markDuplicatesForDeletion(UUID groupId, String username) {
        log.info("Marking duplicates for deletion: groupId={}, user={}", groupId, username);
        
        // Verify group exists
        DuplicateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Duplicate group not found"));
        
        // Mark duplicates (excluding original)
        int marked = fileRepository.markDuplicatesForDeletion(groupId);
        
        // Update group review status
        group.setReviewed(true);
        group.setReviewedAt(LocalDateTime.now());
        group.setReviewedBy(username);
        group.setActionTaken("DELETE_DUPLICATES");
        groupRepository.save(group);
        
        log.info("Marked {} duplicates for deletion in group {}", marked, groupId);
    }
    
    /**
     * Delete marked duplicate files.
     */
    @Transactional
    public int deleteMarkedDuplicates(UUID groupId, Long userId) {
        log.info("Deleting marked duplicates: groupId={}, userId={}", groupId, userId);
        
        // Get files marked for deletion
        List<DuplicateFile> markedFiles = fileRepository.findMarkedForDeletionByGroupId(groupId);
        
        int deleted = 0;
        for (DuplicateFile duplicateFile : markedFiles) {
            // Verify user owns the file
            if (duplicateFile.getUserId().equals(userId)) {
                try {
                    // Delete actual file
                    fileService.deleteFile(duplicateFile.getFileId(), userId);
                    deleted++;
                } catch (Exception e) {
                    log.error("Failed to delete file: {}", duplicateFile.getFileId(), e);
                }
            }
        }
        
        log.info("Deleted {} duplicate files in group {}", deleted, groupId);
        return deleted;
    }
    
    /**
     * Keep all duplicates (mark as reviewed without deletion).
     */
    @Transactional
    public void keepAllDuplicates(UUID groupId, String username) {
        log.info("Keeping all duplicates: groupId={}, user={}", groupId, username);
        
        DuplicateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Duplicate group not found"));
        
        // Unmark all files
        fileRepository.unmarkAllInGroup(groupId);
        
        // Update group
        group.setReviewed(true);
        group.setReviewedAt(LocalDateTime.now());
        group.setReviewedBy(username);
        group.setActionTaken("KEEP_ALL");
        groupRepository.save(group);
        
        log.info("Kept all duplicates in group {}", groupId);
    }
    
    /**
     * Mark specific file to keep and delete others.
     */
    @Transactional
    public void keepSpecificFile(UUID groupId, UUID fileId, String username) {
        log.info("Keeping specific file: groupId={}, fileId={}, user={}", groupId, fileId, username);
        
        DuplicateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Duplicate group not found"));
        
        List<DuplicateFile> files = fileRepository.findByGroupId(groupId);
        
        // Mark all files except the one to keep
        for (DuplicateFile file : files) {
            file.setMarkedForDeletion(!file.getFileId().equals(fileId));
            fileRepository.save(file);
        }
        
        // Update group
        group.setReviewed(true);
        group.setReviewedAt(LocalDateTime.now());
        group.setReviewedBy(username);
        group.setActionTaken("KEEP_SPECIFIC");
        groupRepository.save(group);
        
        log.info("Marked specific file to keep in group {}", groupId);
    }
    
    // -------------------------------------------------------------------------
    // Statistics
    // -------------------------------------------------------------------------
    
    /**
     * Get duplicate statistics.
     */
    @Transactional(readOnly = true)
    public DuplicateStatsResponse getDuplicateStats() {
        Long totalGroups = groupRepository.count();
        Long unreviewedGroups = groupRepository.countUnreviewed();
        Long potentialSavings = groupRepository.calculateTotalPotentialSavings();
        
        return DuplicateStatsResponse.builder()
                .totalDuplicateGroups(totalGroups)
                .unreviewedGroups(unreviewedGroups)
                .potentialSavingsBytes(potentialSavings)
                .formattedSavings(formatBytes(potentialSavings))
                .build();
    }
    
    /**
     * Get duplicate statistics for a user.
     */
    @Transactional(readOnly = true)
    public DuplicateStatsResponse getUserDuplicateStats(Long userId) {
        // Use database function
        try {
            List<Object[]> results = jdbcTemplate.query(
                    "SELECT * FROM get_user_duplicate_stats(?)",
                    new Object[]{userId},
                    (rs, rowNum) -> new Object[]{
                            rs.getInt("total_duplicates"),
                            rs.getLong("total_savings_bytes"),
                            rs.getInt("duplicate_groups")
                    }
            );
            
            if (results.isEmpty()) {
                return DuplicateStatsResponse.builder()
                        .userDuplicateFiles(0L)
                        .userPotentialSavingsBytes(0L)
                        .userFormattedSavings("0 B")
                        .build();
            }
            
            Object[] row = results.get(0);
            Integer totalDuplicates = (Integer) row[0];
            Long totalSavings = (Long) row[1];
            Integer groups = (Integer) row[2];
            
            return DuplicateStatsResponse.builder()
                    .userDuplicateFiles(totalDuplicates.longValue())
                    .userPotentialSavingsBytes(totalSavings)
                    .userFormattedSavings(formatBytes(totalSavings))
                    .totalDuplicateGroups(groups.longValue())
                    .build();
        } catch (Exception e) {
            log.error("Failed to get user duplicate stats", e);
            return DuplicateStatsResponse.builder()
                    .userDuplicateFiles(0L)
                    .userPotentialSavingsBytes(0L)
                    .userFormattedSavings("0 B")
                    .build();
        }
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    private DuplicateGroupResponse buildGroupResponse(DuplicateGroup group) {
        // Get files in group
        List<DuplicateFile> files = fileRepository.findByGroupId(group.getId());
        
        List<DuplicateGroupResponse.DuplicateFileInfo> fileInfos = files.stream()
                .map(f -> DuplicateGroupResponse.DuplicateFileInfo.builder()
                        .id(f.getId())
                        .fileId(f.getFileId())
                        .userId(f.getUserId())
                        .fileName(f.getFileName())
                        .filePath(f.getFilePath())
                        .uploadedAt(f.getUploadedAt())
                        .isOriginal(f.getIsOriginal())
                        .markedForDeletion(f.getMarkedForDeletion())
                        .keepReason(f.getKeepReason())
                        .build())
                .collect(Collectors.toList());
        
        return DuplicateGroupResponse.builder()
                .id(group.getId())
                .contentHash(group.getContentHash())
                .fileSize(group.getFileSize())
                .formattedFileSize(formatBytes(group.getFileSize()))
                .mimeType(group.getMimeType())
                .duplicateCount(group.getDuplicateCount())
                .totalFileCount(group.getTotalFileCount())
                .firstFileId(group.getFirstFileId())
                .firstFileName(group.getFirstFileName())
                .firstUploadedAt(group.getFirstUploadedAt())
                .potentialSavingsBytes(group.getPotentialSavingsBytes())
                .formattedSavings(group.getFormattedSavings())
                .reviewed(group.getReviewed())
                .reviewedAt(group.getReviewedAt())
                .reviewedBy(group.getReviewedBy())
                .actionTaken(group.getActionTaken())
                .files(fileInfos)
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
    
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }
        
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
