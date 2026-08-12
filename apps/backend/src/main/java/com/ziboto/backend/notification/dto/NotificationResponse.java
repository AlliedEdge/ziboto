package com.ziboto.backend.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.notification.enums.NotificationPriority;
import com.ziboto.backend.notification.enums.NotificationStatus;
import com.ziboto.backend.notification.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for notification.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationPriority priority;
    
    private String relatedEntityType;
    private UUID relatedEntityId;
    
    private String actionUrl;
    private String actionLabel;
    
    private NotificationStatus status;
    private LocalDateTime readAt;
    
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
