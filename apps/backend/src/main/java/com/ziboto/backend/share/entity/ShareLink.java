package com.ziboto.backend.share.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.share.enums.ShareLinkPermission;
import com.ziboto.backend.share.enums.ShareLinkStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a public share link for anonymous file access.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Token-based access (no authentication required)</li>
 *   <li>Optional password protection</li>
 *   <li>Download tracking and limits</li>
 *   <li>Expiration dates</li>
 *   <li>Activity logging</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "share_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareLink {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "file_id", nullable = false)
    private UUID fileId;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    
    @Column(nullable = false, unique = true, length = 100)
    private String token;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShareLinkPermission permission = ShareLinkPermission.VIEW;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShareLinkStatus status = ShareLinkStatus.ACTIVE;
    
    @Column(name = "password_hash", length = 255)
    private String passwordHash;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "max_downloads")
    private Integer maxDownloads;
    
    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "last_accessed_ip", length = 45)
    private String lastAccessedIp;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if link has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if max downloads limit reached.
     */
    public boolean isDownloadLimitReached() {
        return maxDownloads != null && downloadCount >= maxDownloads;
    }
    
    /**
     * Check if link is active and usable.
     */
    public boolean isActive() {
        return status == ShareLinkStatus.ACTIVE 
               && !isExpired() 
               && !isDownloadLimitReached();
    }
    
    /**
     * Check if password protection is enabled.
     */
    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isEmpty();
    }
    
    /**
     * Increment download count.
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
    }
    
    /**
     * Update last accessed info.
     */
    public void updateLastAccessed(String ipAddress) {
        this.lastAccessedAt = LocalDateTime.now();
        this.lastAccessedIp = ipAddress;
    }
}
