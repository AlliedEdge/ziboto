package com.ziboto.backend.activity.repository;

import com.ziboto.backend.activity.entity.ActivityLog;
import com.ziboto.backend.activity.enums.ActivityType;
import com.ziboto.backend.activity.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ActivityLog entity.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    
    /**
     * Find activities by user ID with pagination.
     */
    Page<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find activities by activity type with pagination.
     */
    Page<ActivityLog> findByActivityTypeOrderByCreatedAtDesc(ActivityType activityType, Pageable pageable);
    
    /**
     * Find activities by user and activity type.
     */
    Page<ActivityLog> findByUserIdAndActivityTypeOrderByCreatedAtDesc(
            Long userId, ActivityType activityType, Pageable pageable);
    
    /**
     * Find activities for a specific entity.
     */
    Page<ActivityLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            EntityType entityType, UUID entityId, Pageable pageable);
    
    /**
     * Find recent activities across all users (admin view).
     */
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Count activities by user.
     */
    long countByUserId(Long userId);
    
    /**
     * Count activities by user and type.
     */
    long countByUserIdAndActivityType(Long userId, ActivityType activityType);
    
    /**
     * Find activities within date range.
     */
    @Query("SELECT a FROM ActivityLog a WHERE a.userId = :userId " +
           "AND a.createdAt >= :startDate AND a.createdAt <= :endDate " +
           "ORDER BY a.createdAt DESC")
    Page<ActivityLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    /**
     * Get activity summary (count by type).
     */
    @Query("SELECT a.activityType, COUNT(a) FROM ActivityLog a " +
           "WHERE a.userId = :userId " +
           "AND a.createdAt >= :since " +
           "GROUP BY a.activityType " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> getActivitySummary(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);
    
    /**
     * Delete activities by user ID.
     */
    @Modifying
    @Query("DELETE FROM ActivityLog a WHERE a.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    /**
     * Delete activities older than specified date.
     */
    @Modifying
    @Query("DELETE FROM ActivityLog a WHERE a.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Get most recent activity for user.
     */
    ActivityLog findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Check if user has any activity of a specific type.
     */
    boolean existsByUserIdAndActivityType(Long userId, ActivityType activityType);
}
