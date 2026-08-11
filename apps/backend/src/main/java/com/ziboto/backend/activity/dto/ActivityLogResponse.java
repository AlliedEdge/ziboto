package com.ziboto.backend.activity.dto;

import com.ziboto.backend.activity.enums.ActivityType;
import com.ziboto.backend.activity.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for activity log.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    
    private UUID id;
    private Long userId;
    private String username;
    private ActivityType activityType;
    private EntityType entityType;
    private UUID entityId;
    private String entityName;
    private String action;
    private String description;
    private Map<String, Object> metadata;
    private String ipAddress;
    private LocalDateTime createdAt;
    private String timeAgo; // Human-readable time (e.g., "2 hours ago")
}
