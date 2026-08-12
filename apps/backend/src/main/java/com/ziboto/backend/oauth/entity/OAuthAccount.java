package com.ziboto.backend.oauth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.oauth.enums.OAuthProvider;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an OAuth account linked to a user.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "oauth_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OAuthProvider provider;
    
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "name", length = 255)
    private String name;
    
    @Column(name = "picture_url", length = 500)
    private String pictureUrl;
    
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;
    
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
    
    @Column(name = "id_token", columnDefinition = "TEXT")
    private String idToken;
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;
    
    @Column(name = "provider_data", columnDefinition = "JSONB")
    private String providerData;
    
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (linkedAt == null) {
            linkedAt = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(LocalDateTime.now());
    }
}
