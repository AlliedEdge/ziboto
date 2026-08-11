package com.ziboto.backend.duplicate.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an individual file in a duplicate group.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "duplicate_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Reference to duplicate group.
     */
    @Column(name = "group_id", nullable = false)
    private UUID groupId;
    
    /**
     * Reference to actual file.
     */
    @Column(name = "file_id", nullable = false)
    private UUID fileId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    // -------------------------------------------------------------------------
    // File Details
    // -------------------------------------------------------------------------
    
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    // -------------------------------------------------------------------------
    // Duplicate Status
    // -------------------------------------------------------------------------
    
    /**
     * TRUE if this is the first file uploaded (chronologically).
     */
    @Column(name = "is_original")
    @Builder.Default
    private Boolean isOriginal = false;
    
    /**
     * TRUE if user marked this file for deletion.
     */
    @Column(name = "marked_for_deletion")
    @Builder.Default
    private Boolean markedForDeletion = false;
    
    /**
     * Reason to keep this file (if not marked for deletion).
     */
    @Column(name = "keep_reason", columnDefinition = "TEXT")
    private String keepReason;
    
    // -------------------------------------------------------------------------
    // Timestamps
    // -------------------------------------------------------------------------
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    /**
     * Check if this file is marked for deletion.
     */
    public boolean isMarkedForDeletion() {
        return markedForDeletion != null && markedForDeletion;
    }
    
    /**
     * Check if this is the original file.
     */
    public boolean isOriginal() {
        return isOriginal != null && isOriginal;
    }
}
