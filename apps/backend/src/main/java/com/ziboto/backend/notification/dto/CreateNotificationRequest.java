package com.ziboto.backend.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ziboto.backend.notification.enums.NotificationPriority;
import com.ziboto.backend.notification.enums.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating notification.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Type is required")
    private NotificationType type;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private NotificationPriority priority;
    
    private String relatedEntityType;
    private UUID relatedEntityId;
    
    private String actionUrl;
    private String actionLabel;
    
    private LocalDateTime expiresAt;
}
