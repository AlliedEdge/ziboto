package com.ziboto.backend.notification.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.notification.dto.CreateNotificationRequest;
import com.ziboto.backend.notification.dto.NotificationResponse;
import com.ziboto.backend.notification.entity.Notification;
import com.ziboto.backend.notification.enums.NotificationPriority;
import com.ziboto.backend.notification.enums.NotificationStatus;
import com.ziboto.backend.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing notifications.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    /**
     * Create a new notification.
     */
    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        log.info("Creating notification: userId={}, type={}", request.getUserId(), request.getType());
        
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.NORMAL)
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .actionUrl(request.getActionUrl())
                .actionLabel(request.getActionLabel())
                .expiresAt(request.getExpiresAt())
                .build();
        
        notification = notificationRepository.save(notification);
        log.info("Notification created: id={}", notification.getId());
        
        // TODO: Send via WebSocket
        // TODO: Send via Email (based on user preferences)
        
        return buildResponse(notification);
    }
    
    /**
     * Get notifications for user.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);
        return notifications.map(this::buildResponse);
    }
    
    /**
     * Get unread notifications for user.
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findUnreadByUserId(userId, pageable);
        return notifications.map(this::buildResponse);
    }
    
    /**
     * Get specific notification.
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Not authorized to access this notification");
        }
        
        return buildResponse(notification);
    }
    
    /**
     * Mark notification as read.
     */
    @Transactional
    public void markAsRead(UUID notificationId, Long userId) {
        log.info("Marking notification as read: id={}, userId={}", notificationId, userId);
        
        int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        
        if (updated == 0) {
            throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found or already read");
        }
        
        log.info("Notification marked as read: id={}", notificationId);
    }
    
    /**
     * Mark all notifications as read for user.
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        log.info("Marking all notifications as read: userId={}", userId);
        
        int updated = notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        
        log.info("Marked {} notifications as read for user {}", updated, userId);
        return updated;
    }
    
    /**
     * Delete notification.
     */
    @Transactional
    public void deleteNotification(UUID notificationId, Long userId) {
        log.info("Deleting notification: id={}, userId={}", notificationId, userId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, "Not authorized to delete this notification");
        }
        
        notificationRepository.delete(notification);
        log.info("Notification deleted: id={}", notificationId);
    }
    
    /**
     * Get unread count for user.
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    /**
     * Get urgent unread count for user.
     */
    @Transactional(readOnly = true)
    public Long getUrgentUnreadCount(Long userId) {
        return notificationRepository.countUrgentUnreadByUserId(userId);
    }
    
    /**
     * Delete expired notifications (scheduled task).
     */
    @Transactional
    public int deleteExpiredNotifications() {
        log.info("Deleting expired notifications");
        
        int deleted = notificationRepository.deleteExpired(LocalDateTime.now());
        
        log.info("Deleted {} expired notifications", deleted);
        return deleted;
    }
    
    /**
     * Build notification response DTO.
     */
    private NotificationResponse buildResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .status(notification.getStatus())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }
}
