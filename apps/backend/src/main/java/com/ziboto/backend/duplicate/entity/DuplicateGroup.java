package com.ziboto.backend.duplicate.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a group of duplicate files.
 * 
 * <p>Files are considered duplicates if they have the same SHA-256 hash.</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "duplicate_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateGroup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * SHA-256 hash of file content.
     */
    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    private String contentHash;
    
    /**
     * File size in bytes.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "mime_type", length = 255)
    private String mimeType;
    
    /**
     * Number of duplicate files (excluding original).
     */
    @Column(name = "duplicate_count", nullable = false)
    @Builder.Default
    private Integer duplicateCount = 0;
    
    // -------------------------------------------------------------------------
    // First File (Original)
    // -------------------------------------------------------------------------
    
    @Column(name = "first_file_id", nullable = false)
    private UUID firstFileId;
    
    @Column(name = "first_file_name", length = 255)
    private String firstFileName;
    
    @Column(name = "first_uploaded_at", nullable = false)
    private LocalDateTime firstUploadedAt;
    
    // -------------------------------------------------------------------------
    // Storage Savings
    // -------------------------------------------------------------------------
    
    /**
     * Potential storage savings if duplicates are deleted.
     */
    @Column(name = "potential_savings_bytes")
    @Builder.Default
    private Long potentialSavingsBytes = 0L;
    
    // -------------------------------------------------------------------------
    // Review Status
    // -------------------------------------------------------------------------
    
    @Column(name = "reviewed")
    @Builder.Default
    private Boolean reviewed = false;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;
    
    /**
     * Action taken on this duplicate group.
     */
    @Column(name = "action_taken", length = 50)
    private String actionTaken; // KEEP_ALL, DELETE_DUPLICATES, KEEP_ORIGINAL
    
    // -------------------------------------------------------------------------
    // Timestamps
    // -------------------------------------------------------------------------
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    /**
     * Get total number of files in this group (including original).
     */
    public int getTotalFileCount() {
        return duplicateCount + 1;
    }
    
    /**
     * Format potential savings to human-readable format.
     */
    public String getFormattedSavings() {
        if (potentialSavingsBytes == null || potentialSavingsBytes == 0) {
            return "0 B";
        }
        
        if (potentialSavingsBytes < 1024) {
            return potentialSavingsBytes + " B";
        }
        
        int exp = (int) (Math.log(potentialSavingsBytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", potentialSavingsBytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Check if group has been reviewed.
     */
    public boolean isReviewed() {
        return reviewed != null && reviewed;
    }
}
