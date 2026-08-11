package com.ziboto.backend.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ziboto.backend.notification.entity.Notification;
import com.ziboto.backend.notification.enums.NotificationStatus;
import com.ziboto.backend.notification.enums.NotificationType;

/**
 * Repository for Notification entity.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    /**
     * Find notifications for user.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find unread notifications for user.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status = 'UNREAD' ORDER BY n.priority DESC, n.createdAt DESC")
    Page<Notification> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find notifications by status.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status = :status ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") NotificationStatus status, Pageable pageable);
    
    /**
     * Find notifications by type.
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndType(@Param("userId") Long userId, @Param("type") NotificationType type);
    
    /**
     * Count unread notifications for user.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = 'UNREAD'")
    Long countUnreadByUserId(@Param("userId") Long userId);
    
    /**
     * Count urgent unread notifications.
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = 'UNREAD' AND n.priority = 'URGENT'")
    Long countUrgentUnreadByUserId(@Param("userId") Long userId);
    
    /**
     * Mark notification as read.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.id = :notificationId AND n.userId = :userId")
    int markAsRead(@Param("notificationId") UUID notificationId, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
    
    /**
     * Mark all notifications as read for user.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.userId = :userId AND n.status = 'UNREAD'")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
    
    /**
     * Delete expired notifications.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
    
    /**
     * Find notifications expiring soon.
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt BETWEEN :now AND :threshold")
    List<Notification> findExpiringSoon(@Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);
}
