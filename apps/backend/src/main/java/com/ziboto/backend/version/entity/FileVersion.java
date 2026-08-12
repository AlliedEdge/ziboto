package com.ziboto.backend.version.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a file version.
 * 
 * <p>Stores complete snapshot of file metadata at a specific point in time.</p>
 * <p>Enables version history tracking and rollback capabilities.</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "file_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Reference to the current file.
     */
    @Column(name = "file_id", nullable = false)
    private UUID fileId;
    
    /**
     * User who owns this version.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * Sequential version number (1, 2, 3, ...).
     */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    
    // -------------------------------------------------------------------------
    // File Metadata Snapshot
    // -------------------------------------------------------------------------
    
    /**
     * File name at this version.
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    /**
     * File size in bytes.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    /**
     * MIME type.
     */
    @Column(name = "mime_type", length = 255)
    private String mimeType;
    
    /**
     * SHA-256 hash for content verification and deduplication.
     */
    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;
    
    // -------------------------------------------------------------------------
    // Storage Information
    // -------------------------------------------------------------------------
    
    /**
     * S3 object key or storage path.
     */
    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;
    
    /**
     * Storage location (S3, LOCAL, etc.).
     */
    @Column(name = "storage_location", length = 100)
    @Builder.Default
    private String storageLocation = "S3";
    
    // -------------------------------------------------------------------------
    // Version Metadata
    // -------------------------------------------------------------------------
    
    /**
     * User-provided description of what changed.
     */
    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;
    
    /**
     * Optional version tag (e.g., "v1.0", "final", "draft").
     */
    @Column(name = "version_tag", length = 100)
    private String versionTag;
    
    // -------------------------------------------------------------------------
    // Audit Fields
    // -------------------------------------------------------------------------
    
    /**
     * When this version was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Who created this version.
     */
    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;
    
    // -------------------------------------------------------------------------
    // Lifecycle Callbacks
    // -------------------------------------------------------------------------
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    /**
     * Format file size to human-readable format.
     */
    public String getFormattedFileSize() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }
    
    /**
     * Get file extension.
     */
    public String getFileExtension() {
        if (fileName == null) {
            return null;
        }
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return null;
    }
    
    /**
     * Check if this is the initial version.
     */
    public boolean isInitialVersion() {
        return versionNumber != null && versionNumber == 1;
    }
    
    /**
     * Get version label (tag or number).
     */
    public String getVersionLabel() {
        if (versionTag != null && !versionTag.isEmpty()) {
            return versionTag;
        }
        return "v" + versionNumber;
    }
}
