package com.ziboto.backend.activity.service;

import com.ziboto.backend.activity.dto.ActivityLogResponse;
import com.ziboto.backend.activity.dto.ActivitySummaryResponse;
import com.ziboto.backend.activity.entity.ActivityLog;
import com.ziboto.backend.activity.enums.ActivityType;
import com.ziboto.backend.activity.enums.EntityType;
import com.ziboto.backend.activity.repository.ActivityLogRepository;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing activity logs.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {
    
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    
    /**
     * Log an activity asynchronously.
     * Non-blocking to avoid slowing down main operations.
     */
    @Async
    @Transactional
    public void logActivity(
            Long userId,
            ActivityType activityType,
            EntityType entityType,
            UUID entityId,
            String entityName,
            String action,
            Map<String, Object> metadata,
            HttpServletRequest request) {
        
        try {
            String ipAddress = getClientIp(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            
            String description = buildDescription(activityType, entityName, action);
            
            ActivityLog activityLog = ActivityLog.builder()
                    .userId(userId)
                    .activityType(activityType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .action(action)
                    .description(description)
                    .metadata(metadata != null ? metadata : new HashMap<>())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            activityLogRepository.save(activityLog);
            
            log.debug("Activity logged - user: {}, type: {}, entity: {}", 
                     userId, activityType, entityName);
            
        } catch (Exception e) {
            log.error("Failed to log activity - user: {}, type: {}", userId, activityType, e);
            // Don't throw exception - logging failure shouldn't break main operation
        }
    }
    
    /**
     * Log activity without HTTP request context.
     */
    @Async
    @Transactional
    public void logActivity(
            Long userId,
            ActivityType activityType,
            EntityType entityType,
            UUID entityId,
            String entityName,
            String action) {
        
        logActivity(userId, activityType, entityType, entityId, entityName, action, null, null);
    }
    
    /**
     * Get user's activity history.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getUserActivities(Long userId, Pageable pageable) {
        Page<ActivityLog> activities = activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return activities.map(this::mapToResponse);
    }
    
    /**
     * Get global activities (admin only).
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getGlobalActivities(Pageable pageable) {
        Page<ActivityLog> activities = activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return activities.map(this::mapToResponseWithUsername);
    }
    
    /**
     * Get activities for a specific entity.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLogResponse> getEntityActivities(
            EntityType entityType, 
            UUID entityId, 
            Pageable pageable) {
        
        Page<ActivityLog> activities = activityLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
        
        return activities.map(this::mapToResponseWithUsername);
    }
    
    /**
     * Get activity summary for user.
     */
    @Transactional(readOnly = true)
    public ActivitySummaryResponse getUserActivitySummary(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        
        long totalActivities = activityLogRepository.countByUserId(userId);
        
        // Get activity counts by type
        var summary = activityLogRepository.getActivitySummary(userId, since);
        Map<ActivityType, Long> activityCounts = new HashMap<>();
        for (Object[] row : summary) {
            activityCounts.put((ActivityType) row[0], (Long) row[1]);
        }
        
        // Get most recent and oldest activity
        ActivityLog mostRecent = activityLogRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
        
        LocalDateTime mostRecentTime = mostRecent != null ? mostRecent.getCreatedAt() : null;
        
        // Calculate days active and average
        Integer daysActive = days;
        Double averagePerDay = totalActivities > 0 ? (double) totalActivities / days : 0.0;
        
        return ActivitySummaryResponse.builder()
                .totalActivities(totalActivities)
                .activityCounts(activityCounts)
                .mostRecentActivity(mostRecentTime)
                .oldestActivity(since)
                .daysActive(daysActive)
                .averageActivitiesPerDay(averagePerDay)
                .build();
    }
    
    /**
     * Get activity count for user.
     */
    @Transactional(readOnly = true)
    public long getUserActivityCount(Long userId) {
        return activityLogRepository.countByUserId(userId);
    }
    
    /**
     * Clear all activities for user.
     */
    @Transactional
    public void clearUserActivities(Long userId) {
        log.info("Clearing activities for user: {}", userId);
        activityLogRepository.deleteByUserId(userId);
    }
    
    /**
     * Delete a specific activity.
     */
    @Transactional
    public void deleteActivity(UUID activityId, Long userId) {
        ActivityLog activity = activityLogRepository.findById(activityId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Activity not found"));
        
        // Verify ownership
        if (!activity.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Cannot delete another user's activity");
        }
        
        activityLogRepository.delete(activity);
        log.info("Activity deleted - id: {}, user: {}", activityId, userId);
    }
    
    /**
     * Cleanup old activities (retention policy).
     * Should be run periodically via scheduled task.
     */
    @Transactional
    public int cleanupOldActivities(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        int deleted = activityLogRepository.deleteOlderThan(cutoffDate);
        
        log.info("Cleaned up {} old activities (older than {} days)", deleted, daysToKeep);
        return deleted;
    }
    
    /**
     * Map ActivityLog to response DTO.
     */
    private ActivityLogResponse mapToResponse(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(null) // Not fetching user for performance
                .activityType(log.getActivityType())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityName(log.getEntityName())
                .action(log.getAction())
                .description(log.getDescription())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .timeAgo(formatTimeAgo(log.getCreatedAt()))
                .build();
    }
    
    /**
     * Map ActivityLog to response DTO with username.
     */
    private ActivityLogResponse mapToResponseWithUsername(ActivityLog log) {
        String username = userRepository.findById(log.getUserId())
                .map(User::getUsername)
                .orElse("Unknown");
        
        return ActivityLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(username)
                .activityType(log.getActivityType())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityName(log.getEntityName())
                .action(log.getAction())
                .description(log.getDescription())
                .metadata(log.getMetadata())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .timeAgo(formatTimeAgo(log.getCreatedAt()))
                .build();
    }
    
    /**
     * Build human-readable description.
     */
    private String buildDescription(ActivityType activityType, String entityName, String action) {
        return switch (activityType) {
            case FILE_UPLOADED -> "Uploaded file: " + entityName;
            case FILE_DOWNLOADED -> "Downloaded file: " + entityName;
            case FILE_DELETED -> "Deleted file: " + entityName;
            case FILE_UPDATED -> "Updated file: " + entityName;
            case FILE_SHARED -> "Shared file: " + entityName;
            case FOLDER_CREATED -> "Created folder: " + entityName;
            case USER_LOGIN -> "Logged in";
            case USER_LOGOUT -> "Logged out";
            case USER_REGISTERED -> "Registered account";
            default -> activityType.name().toLowerCase().replace("_", " ") + ": " + entityName;
        };
    }
    
    /**
     * Format time ago (e.g., "2 hours ago").
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown";
        }
        
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();
        
        if (seconds < 60) {
            return seconds + " seconds ago";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (seconds < 2592000) {
            long days = seconds / 86400;
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (seconds < 31536000) {
            long months = seconds / 2592000;
            return months + " month" + (months > 1 ? "s" : "") + " ago";
        } else {
            long years = seconds / 31536000;
            return years + " year" + (years > 1 ? "s" : "") + " ago";
        }
    }
    
    /**
     * Get client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Handle multiple IPs (take first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
}
