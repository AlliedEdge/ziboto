package com.ziboto.backend.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event for in-app notifications.
 * 
 * <p>Delivers real-time notifications to users via WebSocket
 * and stores them in the database for persistence.</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    
    /**
     * Event unique identifier.
     */
    private UUID eventId;
    
    /**
     * Target user ID.
     */
    private Long userId;
    
    /**
     * Notification type.
     */
    private NotificationType type;
    
    /**
     * Notification title.
     */
    private String title;
    
    /**
     * Notification message body.
     */
    private String message;
    
    /**
     * Related entity ID (file, folder, user, etc).
     */
    private UUID relatedEntityId;
    
    /**
     * Related entity type.
     */
    private String relatedEntityType;
    
    /**
     * Action URL (optional).
     */
    private String actionUrl;
    
    /**
     * Notification priority.
     */
    private Priority priority;
    
    /**
     * Event timestamp.
     */
    private LocalDateTime timestamp;
    
    /**
     * Expiration date (optional).
     */
    private LocalDateTime expiresAt;
    
    /**
     * Notification types.
     */
    public enum NotificationType {
        FILE_SHARED,
        FILE_COMMENT,
        FILE_MENTION,
        STORAGE_WARNING,
        UPLOAD_COMPLETE,
        PROCESSING_COMPLETE,
        SHARE_LINK_ACCESSED,
        FOLDER_SHARED,
        SYSTEM_ANNOUNCEMENT
    }
    
    /**
     * Notification priorities.
     */
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}
