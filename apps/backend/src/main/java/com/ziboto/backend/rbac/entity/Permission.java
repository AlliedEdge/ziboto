package com.ziboto.backend.rbac.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a system permission.
 * 
 * <p>Permissions are in format "resource:action" (e.g., "files:create")</p>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Permission identifier (e.g., "files:create", "users:delete").
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
    
    /**
     * Resource type (e.g., "files", "users", "folders").
     */
    @Column(name = "resource", nullable = false, length = 50)
    private String resource;
    
    /**
     * Action type (e.g., "create", "read", "update", "delete", "*").
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this permission matches a requested permission.
     * Handles wildcard matching (e.g., "files:*" matches "files:create").
     */
    public boolean matches(String requestedPermission) {
        if (name.equals(requestedPermission)) {
            return true;
        }
        
        // Check wildcard (e.g., "files:*" matches "files:create")
        if (name.endsWith(":*")) {
            String resourcePart = name.substring(0, name.lastIndexOf(':'));
            return requestedPermission.startsWith(resourcePart + ":");
        }
        
        return false;
    }
}
