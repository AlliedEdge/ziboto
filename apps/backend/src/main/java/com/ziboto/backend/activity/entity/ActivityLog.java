package com.ziboto.backend.activity.entity;

import com.ziboto.backend.activity.enums.ActivityType;
import com.ziboto.backend.activity.enums.EntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a user activity log entry.
 * Tracks all user actions for audit and history.
 * 
 * @author Ziboto Team
 * @since V3
 */
@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_activity_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_logs_activity_type", columnList = "activity_type"),
    @Index(name = "idx_activity_logs_entity_type", columnList = "entity_type"),
    @Index(name = "idx_activity_logs_entity_id", columnList = "entity_id"),
    @Index(name = "idx_activity_logs_created_at", columnList = "created_at"),
    @Index(name = "idx_activity_logs_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_activity_logs_entity", columnList = "entity_type, entity_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType;
    
    @Column(name = "entity_id")
    private UUID entityId;
    
    @Column(name = "entity_name", length = 500)
    private String entityName;
    
    @Column(name = "action", nullable = false, length = 100)
    private String action;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
