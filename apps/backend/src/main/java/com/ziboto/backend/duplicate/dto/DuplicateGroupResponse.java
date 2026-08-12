package com.ziboto.backend.duplicate.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for duplicate group information.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateGroupResponse {
    
    private UUID id;
    private String contentHash;
    
    // File metadata
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
    
    // Duplicate info
    private Integer duplicateCount;
    private Integer totalFileCount;
    
    // Original file
    private UUID firstFileId;
    private String firstFileName;
    private LocalDateTime firstUploadedAt;
    
    // Savings
    private Long potentialSavingsBytes;
    private String formattedSavings;
    
    // Review status
    private Boolean reviewed;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String actionTaken;
    
    // Files in group
    private List<DuplicateFileInfo> files;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DuplicateFileInfo {
        private UUID id;
        private UUID fileId;
        private Long userId;
        private String fileName;
        private String filePath;
        private LocalDateTime uploadedAt;
        private Boolean isOriginal;
        private Boolean markedForDeletion;
        private String keepReason;
    }
}
