package com.ziboto.backend.gallery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a public gallery.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Entity
@Table(name = "galleries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gallery {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "slug", unique = true, nullable = false)
    private String slug;
    
    @Column(name = "is_public")
    private Boolean isPublic;
    
    @Column(name = "password_protected")
    private Boolean passwordProtected;
    
    @Column(name = "password_hash")
    private String passwordHash;
    
    @Column(name = "theme")
    private String theme;
    
    @Column(name = "layout")
    private String layout;
    
    @Column(name = "view_count")
    private Long viewCount;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isPublic == null) {
            isPublic = true;
        }
        if (passwordProtected == null) {
            passwordProtected = false;
        }
        if (viewCount == null) {
            viewCount = 0L;
        }
        if (theme == null) {
            theme = "default";
        }
        if (layout == null) {
            layout = "grid";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
