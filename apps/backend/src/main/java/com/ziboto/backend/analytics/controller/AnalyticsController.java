package com.ziboto.backend.analytics.controller;

import com.ziboto.backend.analytics.dto.StorageAnalyticsResponse;
import com.ziboto.backend.analytics.service.AnalyticsService;
import com.ziboto.backend.common.dto.ApiResponse;
import com.ziboto.backend.exception.ResourceNotFoundException;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for storage analytics.
 * 
 * @author Ziboto Team
 * @since V3
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Storage usage analytics and insights")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    
    /**
     * Get storage analytics for current user.
     * 
     * GET /api/v1/analytics/storage?days=30
     */
    @GetMapping("/storage")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get storage analytics", description = "Get comprehensive storage analytics and insights")
    public ResponseEntity<ApiResponse<StorageAnalyticsResponse>> getStorageAnalytics(
            Authentication authentication,
            @RequestParam(defaultValue = "30") int days) {
        
        Long userId = getUserId(authentication);
        log.info("Get storage analytics - userId: {}, days: {}", userId, days);
        
        StorageAnalyticsResponse analytics = analyticsService.getStorageAnalytics(userId, days);
        
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
    
    /**
     * Record storage snapshot for current user.
     * 
     * POST /api/v1/analytics/snapshot
     */
    @PostMapping("/snapshot")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Record snapshot", description = "Manually record a storage usage snapshot")
    public ResponseEntity<ApiResponse<Void>> recordSnapshot(Authentication authentication) {
        
        Long userId = getUserId(authentication);
        log.info("Record storage snapshot - userId: {}", userId);
        
        analyticsService.recordStorageSnapshot(userId);
        
        return ResponseEntity.ok(ApiResponse.success("Snapshot recorded successfully", null));
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
