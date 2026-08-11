package com.ziboto.backend.notification.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.ziboto.backend.notification.dto.CreateNotificationRequest;
import com.ziboto.backend.notification.dto.NotificationResponse;
import com.ziboto.backend.notification.service.NotificationService;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for notifications.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /api/v1/notifications - Get user's notifications</li>
 *   <li>GET /api/v1/notifications/unread - Get unread notifications</li>
 *   <li>GET /api/v1/notifications/count - Get unread count</li>
 *   <li>GET /api/v1/notifications/{id} - Get specific notification</li>
 *   <li>POST /api/v1/notifications - Create notification</li>
 *   <li>PUT /api/v1/notifications/{id}/read - Mark as read</li>
 *   <li>PUT /api/v1/notifications/read-all - Mark all as read</li>
 *   <li>DELETE /api/v1/notifications/{id} - Delete notification</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    /**
     * Get user's notifications.
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(user.getId(), pageable);
        
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get unread notifications.
     */
    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponse>> getUnreadNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        Page<NotificationResponse> notifications = notificationService.getUnreadNotifications(user.getId(), pageable);
        
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get unread count.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        
        Long unreadCount = notificationService.getUnreadCount(user.getId());
        Long urgentCount = notificationService.getUrgentUnreadCount(user.getId());
        
        return ResponseEntity.ok(Map.of(
                "unread", unreadCount,
                "urgent", urgentCount
        ));
    }
    
    /**
     * Get specific notification.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        NotificationResponse notification = notificationService.getNotification(id, user.getId());
        
        return ResponseEntity.ok(notification);
    }
    
    /**
     * Create notification (admin/system use).
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        NotificationResponse notification = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }
    
    /**
     * Mark notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        notificationService.markAsRead(id, user.getId());
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mark all notifications as read.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserFromDetails(userDetails);
        int marked = notificationService.markAllAsRead(user.getId());
        
        return ResponseEntity.ok(Map.of("marked", marked));
    }
    
    /**
     * Delete notification.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getUserFromDetails(userDetails);
        notificationService.deleteNotification(id, user.getId());
        
        return ResponseEntity.noContent().build();
    }
    
    private User getUserFromDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
