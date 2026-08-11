package com.ziboto.backend.rbac.entity;

import java.time.LocalDateTime;
import java.util.Set;

import com.ziboto.backend.rbac.enums.RoleType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a system role.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true)
    private RoleType name;
    
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * Role hierarchy level (higher = more permissions).
     */
    @Column(name = "level", nullable = false)
    private Integer level;
    
    /**
     * Parent role for permission inheritance.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "parent_role")
    private RoleType parentRole;
    
    /**
     * Permissions assigned to this role.
     */
    @ManyToMany
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_name", referencedColumnName = "name"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;
    
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
}
