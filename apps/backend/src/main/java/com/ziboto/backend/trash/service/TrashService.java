package com.ziboto.backend.trash.service;

import com.ziboto.backend.activity.enums.ActivityType;
import com.ziboto.backend.activity.enums.EntityType;
import com.ziboto.backend.activity.service.ActivityService;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.file.entity.FileMetadata;
import com.ziboto.backend.file.repository.FileMetadataRepository;
import com.ziboto.backend.file.entity.Folder;
import com.ziboto.backend.file.repository.FolderRepository;
import com.ziboto.backend.file.service.S3StorageService;
import com.ziboto.backend.trash.dto.TrashItemResponse;
import com.ziboto.backend.trash.dto.TrashSummaryResponse;
import com.ziboto.backend.trash.enums.TrashItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing trash bin (soft delete).
 * 
 * @author Ziboto Team
 * @since V3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrashService {
    
    private final FileMetadataRepository fileMetadataRepository;
    private final FolderRepository folderRepository;
    private final S3StorageService storageService;
    private final ActivityService activityService;
    
    private static final int TRASH_RETENTION_DAYS = 30;
    
    /**
     * Move file to trash (soft delete).
     */
    @Transactional
    public void moveFileToTrash(UUID fileId, Long userId, String username) {
        log.info("Moving file to trash - fileId: {}, userId: {}", fileId, userId);
        
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // Verify ownership
        if (!file.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot delete another user's file");
        }
        
        file.setDeletedAt(LocalDateTime.now());
        file.setDeletedBy(username);
        fileMetadataRepository.save(file);
        
        // Log activity
        activityService.logActivity(
                userId,
                ActivityType.FILE_DELETED,
                EntityType.FILE,
                fileId,
                file.getFileName(),
                "moved to trash"
        );
        
        log.info("File moved to trash - fileId: {}", fileId);
    }
    
    /**
     * Move folder to trash (soft delete).
     */
    @Transactional
    public void moveFolderToTrash(UUID folderId, Long userId, String username) {
        log.info("Moving folder to trash - folderId: {}, userId: {}", folderId, userId);
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Folder not found"));
        
        // Verify ownership
        if (!folder.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot delete another user's folder");
        }
        
        folder.setDeletedAt(LocalDateTime.now());
        folder.setDeletedBy(username);
        folderRepository.save(folder);
        
        // Log activity
        activityService.logActivity(
                userId,
                ActivityType.FOLDER_DELETED,
                EntityType.FOLDER,
                folderId,
                folder.getFolderName(),
                "moved to trash"
        );
        
        log.info("Folder moved to trash - folderId: {}", folderId);
    }
    
    /**
     * Get trash items for user.
     */
    @Transactional(readOnly = true)
    public Page<TrashItemResponse> getUserTrash(Long userId, Pageable pageable) {
        List<TrashItemResponse> items = new ArrayList<>();
        
        // Get deleted files
        List<FileMetadata> deletedFiles = fileMetadataRepository
                .findByUserIdAndDeletedAtIsNotNull(userId);
        
        for (FileMetadata file : deletedFiles) {
            items.add(mapFileToTrashItem(file));
        }
        
        // Get deleted folders
        List<Folder> deletedFolders = folderRepository
                .findByUserIdAndDeletedAtIsNotNull(userId);
        
        for (Folder folder : deletedFolders) {
            items.add(mapFolderToTrashItem(folder));
        }
        
        // Sort by deleted date (most recent first)
        items.sort((a, b) -> b.getDeletedAt().compareTo(a.getDeletedAt()));
        
        // Paginate manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), items.size());
        
        List<TrashItemResponse> pageContent = items.subList(start, Math.min(end, items.size()));
        
        return new PageImpl<>(pageContent, pageable, items.size());
    }
    
    /**
     * Restore file from trash.
     */
    @Transactional
    public void restoreFile(UUID fileId, Long userId) {
        log.info("Restoring file from trash - fileId: {}, userId: {}", fileId, userId);
        
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // Verify ownership
        if (!file.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot restore another user's file");
        }
        
        // Verify it's in trash
        if (file.getDeletedAt() == null) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "File is not in trash");
        }
        
        file.setDeletedAt(null);
        file.setDeletedBy(null);
        fileMetadataRepository.save(file);
        
        // Log activity
        activityService.logActivity(
                userId,
                ActivityType.FILE_RESTORED,
                EntityType.FILE,
                fileId,
                file.getFileName(),
                "restored from trash"
        );
        
        log.info("File restored from trash - fileId: {}", fileId);
    }
    
    /**
     * Restore folder from trash.
     */
    @Transactional
    public void restoreFolder(UUID folderId, Long userId) {
        log.info("Restoring folder from trash - folderId: {}, userId: {}", folderId, userId);
        
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Folder not found"));
        
        // Verify ownership
        if (!folder.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot restore another user's folder");
        }
        
        // Verify it's in trash
        if (folder.getDeletedAt() == null) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "Folder is not in trash");
        }
        
        folder.setDeletedAt(null);
        folder.setDeletedBy(null);
        folderRepository.save(folder);
        
        log.info("Folder restored from trash - folderId: {}", folderId);
    }
    
    /**
     * Permanently delete file from trash.
     */
    @Transactional
    public void permanentlyDeleteFile(UUID fileId, Long userId) {
        log.info("Permanently deleting file - fileId: {}, userId: {}", fileId, userId);
        
        FileMetadata file = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "File not found"));
        
        // Verify ownership
        if (!file.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot delete another user's file");
        }
        
        // Verify it's in trash
        if (file.getDeletedAt() == null) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "File must be in trash before permanent deletion");
        }
        
        // Delete from storage
        storageService.deleteFile(file.getStorageKey());
        
        // Delete from database
        fileMetadataRepository.delete(file);
        
        log.info("File permanently deleted - fileId: {}", fileId);
    }
    
    /**
     * Empty entire trash for user.
     */
    @Transactional
    public void emptyTrash(Long userId) {
        log.info("Emptying trash - userId: {}", userId);
        
        // Delete all files in trash
        List<FileMetadata> deletedFiles = fileMetadataRepository
                .findByUserIdAndDeletedAtIsNotNull(userId);
        
        for (FileMetadata file : deletedFiles) {
            storageService.deleteFile(file.getStorageKey());
            fileMetadataRepository.delete(file);
        }
        
        // Delete all folders in trash
        List<Folder> deletedFolders = folderRepository
                .findByUserIdAndDeletedAtIsNotNull(userId);
        
        folderRepository.deleteAll(deletedFolders);
        
        log.info("Trash emptied - userId: {}, files: {}, folders: {}", 
                userId, deletedFiles.size(), deletedFolders.size());
    }
    
    /**
     * Get trash summary statistics.
     */
    @Transactional(readOnly = true)
    public TrashSummaryResponse getTrashSummary(Long userId) {
        List<FileMetadata> deletedFiles = fileMetadataRepository
                .findByUserIdAndDeletedAtIsNotNull(userId);
        
        List<Folder> deletedFolders = folderRepository
                .findByUserIdAndDeletedAtIsNotNull(userId);
        
        long totalSize = deletedFiles.stream()
                .mapToLong(FileMetadata::getFileSize)
                .sum();
        
        return TrashSummaryResponse.builder()
                .totalItems((long) (deletedFiles.size() + deletedFolders.size()))
                .totalSize(totalSize)
                .formattedTotalSize(formatBytes(totalSize))
                .fileCount((long) deletedFiles.size())
                .folderCount((long) deletedFolders.size())
                .build();
    }
    
    /**
     * Scheduled task to cleanup old trash items (runs daily).
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM every day
    @Transactional
    public void scheduledTrashCleanup() {
        log.info("Running scheduled trash cleanup");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(TRASH_RETENTION_DAYS);
        
        // Find and delete old files
        List<FileMetadata> oldFiles = fileMetadataRepository
                .findByDeletedAtBefore(cutoffDate);
        
        for (FileMetadata file : oldFiles) {
            storageService.deleteFile(file.getStorageKey());
            fileMetadataRepository.delete(file);
        }
        
        // Find and delete old folders
        List<Folder> oldFolders = folderRepository
                .findByDeletedAtBefore(cutoffDate);
        
        folderRepository.deleteAll(oldFolders);
        
        log.info("Scheduled trash cleanup complete - files: {}, folders: {}", 
                oldFiles.size(), oldFolders.size());
    }
    
    /**
     * Map FileMetadata to TrashItemResponse.
     */
    private TrashItemResponse mapFileToTrashItem(FileMetadata file) {
        LocalDateTime autoDeleteAt = file.getDeletedAt().plusDays(TRASH_RETENTION_DAYS);
        long daysUntilAutoDelete = ChronoUnit.DAYS.between(LocalDateTime.now(), autoDeleteAt);
        
        return TrashItemResponse.builder()
                .id(file.getId())
                .itemType(TrashItemType.FILE)
                .name(file.getFileName())
                .size(file.getFileSize())
                .formattedSize(formatBytes(file.getFileSize()))
                .deletedAt(file.getDeletedAt())
                .deletedBy(file.getDeletedBy())
                .autoDeleteAt(autoDeleteAt)
                .daysUntilAutoDelete((int) daysUntilAutoDelete)
                .timeInTrash(formatTimeAgo(file.getDeletedAt()))
                .build();
    }
    
    /**
     * Map Folder to TrashItemResponse.
     */
    private TrashItemResponse mapFolderToTrashItem(Folder folder) {
        LocalDateTime autoDeleteAt = folder.getDeletedAt().plusDays(TRASH_RETENTION_DAYS);
        long daysUntilAutoDelete = ChronoUnit.DAYS.between(LocalDateTime.now(), autoDeleteAt);
        
        return TrashItemResponse.builder()
                .id(folder.getId())
                .itemType(TrashItemType.FOLDER)
                .name(folder.getFolderName())
                .size(0L)
                .formattedSize("--")
                .deletedAt(folder.getDeletedAt())
                .deletedBy(folder.getDeletedBy())
                .autoDeleteAt(autoDeleteAt)
                .daysUntilAutoDelete((int) daysUntilAutoDelete)
                .timeInTrash(formatTimeAgo(folder.getDeletedAt()))
                .build();
    }
    
    /**
     * Format bytes to human-readable size.
     */
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Format time ago.
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long days = duration.toDays();
        
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }
}
