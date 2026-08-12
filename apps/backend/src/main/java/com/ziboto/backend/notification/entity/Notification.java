package com.ziboto.backend.notification.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.notification.enums.NotificationPriority;
import com.ziboto.backend.notification.enums.NotificationStatus;
import com.ziboto.backend.notification.enums.NotificationType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a user notification.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;
    
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;
    
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;
    
    @Column(name = "action_url", length = 500)
    private String actionUrl;
    
    @Column(name = "action_label", length = 100)
    private String actionLabel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "sent_via_websocket")
    @Builder.Default
    private Boolean sentViaWebsocket = false;
    
    @Column(name = "sent_via_email")
    @Builder.Default
    private Boolean sentViaEmail = false;
    
    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;
    
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public boolean isUnread() {
        return status == NotificationStatus.UNREAD;
    }
    
    public void markAsRead() {
        this.status = NotificationStatus.UNREAD;
        this.readAt = LocalDateTime.now();
    }
}
