package com.ziboto.backend.share.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.share.enums.SharePermission;
import com.ziboto.backend.share.enums.ShareStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a file shared directly with a specific user.
 * 
 * <p>Workflow:</p>
 * <ol>
 *   <li>Owner shares file with user (status: PENDING)</li>
 *   <li>Recipient receives notification</li>
 *   <li>Recipient accepts/declines (status: ACCEPTED/DECLINED)</li>
 *   <li>Owner can revoke anytime (status: REVOKED)</li>
 * </ol>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "file_shares")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "file_id", nullable = false)
    private UUID fileId;
    
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    
    @Column(name = "shared_with_user_id", nullable = false)
    private Long sharedWithUserId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SharePermission permission;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShareStatus status = ShareStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
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
     * Check if share has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if share is active (accepted and not expired/revoked).
     */
    public boolean isActive() {
        return status == ShareStatus.ACCEPTED && !isExpired();
    }
}
