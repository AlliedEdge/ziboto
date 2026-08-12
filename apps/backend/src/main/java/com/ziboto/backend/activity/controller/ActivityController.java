package com.ziboto.backend.activity.controller;

import com.ziboto.backend.activity.dto.ActivityLogResponse;
import com.ziboto.backend.activity.dto.ActivitySummaryResponse;
import com.ziboto.backend.activity.enums.EntityType;
import com.ziboto.backend.activity.service.ActivityService;
import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for activity logs and history.
 * 
 * @author Ziboto Team
 * @since V3
 */
@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Activity Logs", description = "User activity history and audit logs")
public class ActivityController {
    
    private final ActivityService activityService;
    private final UserRepository userRepository;
    
    /**
     * Get current user's activity history.
     * 
     * GET /api/v1/activities
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get user activities", description = "Get paginated list of current user's activities")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getUserActivities(
            Authentication authentication,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        
        Long userId = getUserId(authentication);
        log.info("Get activities request - user: {}", userId);
        
        Page<ActivityLogResponse> activities = activityService.getUserActivities(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
    
    /**
     * Get global activities (admin only).
     * 
     * GET /api/v1/activities/global
     */
    @GetMapping("/global")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all activities (Admin)", description = "Get paginated list of all user activities")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getGlobalActivities(
            Authentication authentication,
            @PageableDefault(size = 100, sort = "createdAt") Pageable pageable) {
        
        Long adminId = getUserId(authentication);
        log.info("Get global activities request - admin: {}", adminId);
        
        Page<ActivityLogResponse> activities = activityService.getGlobalActivities(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
    
    /**
     * Get activities for a specific file.
     * 
     * GET /api/v1/activities/file/{fileId}
     */
    @GetMapping("/file/{fileId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get file activities", description = "Get activity history for a specific file")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getFileActivities(
            @PathVariable UUID fileId,
            Authentication authentication,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Long userId = getUserId(authentication);
        log.info("Get file activities - fileId: {}, user: {}", fileId, userId);
        
        Page<ActivityLogResponse> activities = activityService.getEntityActivities(
                EntityType.FILE, fileId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
    
    /**
     * Get activities for a specific user (admin only).
     * 
     * GET /api/v1/activities/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user activities (Admin)", description = "Get activity history for a specific user")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getUserActivitiesByAdmin(
            @PathVariable Long userId,
            Authentication authentication,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Long adminId = getUserId(authentication);
        log.info("Get user activities - targetUser: {}, admin: {}", userId, adminId);
        
        Page<ActivityLogResponse> activities = activityService.getUserActivities(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
    
    /**
     * Get activity summary for current user.
     * 
     * GET /api/v1/activities/summary?days=30
     */
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get activity summary", description = "Get activity statistics for current user")
    public ResponseEntity<ApiResponse<ActivitySummaryResponse>> getActivitySummary(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {
        
        Long userId = getUserId(authentication);
        log.info("Get activity summary - user: {}, days: {}", userId, days);
        
        ActivitySummaryResponse summary = activityService.getUserActivitySummary(userId, days);
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    /**
     * Delete a specific activity log.
     * 
     * DELETE /api/v1/activities/{activityId}
     */
    @DeleteMapping("/{activityId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete activity", description = "Delete a specific activity log entry")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(
            @PathVariable UUID activityId,
            Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Delete activity - activityId: {}, user: {}", activityId, userId);
        
        activityService.deleteActivity(activityId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Activity deleted successfully", null));
    }
    
    /**
     * Clear all activities for current user.
     * 
     * DELETE /api/v1/activities/clear
     */
    @DeleteMapping("/clear")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clear user activities", description = "Delete all activity logs for current user")
    public ResponseEntity<ApiResponse<Void>> clearUserActivities(Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Clear activities request - user: {}", userId);
        
        activityService.clearUserActivities(userId);
        
        return ResponseEntity.ok(ApiResponse.success("Activities cleared successfully", null));
    }
    
    /**
     * Get activity count for current user.
     * 
     * GET /api/v1/activities/count
     */
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get activity count", description = "Get total number of activities for current user")
    public ResponseEntity<ApiResponse<Long>> getActivityCount(Authentication authentication) {
        
        Long userId = getUserId(authentication);
        long count = activityService.getUserActivityCount(userId);
        
        return ResponseEntity.ok(ApiResponse.success(count));
    }
    
    /**
     * Extract user ID from authentication.
     */
    private Long getUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with username: " + username
                ));
        
        return user.getId();
    }
}
