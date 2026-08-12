package com.ziboto.backend.rbac.enums;

/**
 * System role types with hierarchical structure.
 * 
 * <p>Role Hierarchy (ascending):</p>
 * <pre>
 * GUEST (1) → USER (2) → MANAGER (3) → ADMIN (4) → SUPER_ADMIN (5)
 * </pre>
 * 
 * @author Ziboto Team
 * @since V2
 */
public enum RoleType {
    /**
     * Read-only access to shared files.
     * Level: 1
     */
    GUEST(1),
    
    /**
     * Regular user with full file management.
     * Level: 2
     */
    USER(2),
    
    /**
     * Team manager with user management capabilities.
     * Level: 3
     */
    MANAGER(3),
    
    /**
     * Organization admin with system configuration.
     * Level: 4
     */
    ADMIN(4),
    
    /**
     * Full system access.
     * Level: 5
     */
    SUPER_ADMIN(5);
    
    private final int level;
    
    RoleType(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * Check if this role has higher or equal level than another role.
     */
    public boolean hasHigherOrEqualLevel(RoleType other) {
        return this.level >= other.level;
    }
    
    /**
     * Check if this role can assign another role to users.
     * Rule: Can only assign roles with lower level.
     */
    public boolean canAssignRole(RoleType targetRole) {
        return this.level > targetRole.level;
    }
}
