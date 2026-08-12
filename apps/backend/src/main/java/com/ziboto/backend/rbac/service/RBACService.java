package com.ziboto.backend.rbac.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ziboto.backend.common.constant.ErrorCode;
import com.ziboto.backend.exception.BaseException;
import com.ziboto.backend.rbac.entity.Permission;
import com.ziboto.backend.rbac.entity.Role;
import com.ziboto.backend.rbac.enums.RoleType;
import com.ziboto.backend.rbac.repository.PermissionRepository;
import com.ziboto.backend.rbac.repository.RoleRepository;
import com.ziboto.backend.user.entity.User;
import com.ziboto.backend.user.entity.UserRole;
import com.ziboto.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for Role-Based Access Control (RBAC).
 * 
 * <p>Handles:</p>
 * <ul>
 *   <li>Permission checking</li>
 *   <li>Role assignment</li>
 *   <li>Permission management</li>
 *   <li>Role hierarchy</li>
 * </ul>
 * 
 * @author Ziboto Team
 * @since V2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RBACService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    
    // -------------------------------------------------------------------------
    // Permission Checking
    // -------------------------------------------------------------------------
    
    /**
     * Check if user has specific permission.
     * 
     * @param userId User ID
     * @param permissionName Permission name (e.g., "files:create")
     * @return true if user has permission
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, String permissionName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        
        // Map old UserRole to new RoleType
        RoleType roleType = mapUserRoleToRoleType(user.getRole());
        
        // Get role with permissions
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        
        // Check if any of the role's permissions match the requested permission
        return role.getPermissions().stream()
                .anyMatch(p -> p.matches(permissionName));
    }
    
    /**
     * Check if user has all specified permissions.
     */
    @Transactional(readOnly = true)
    public boolean hasAllPermissions(Long userId, List<String> permissionNames) {
        return permissionNames.stream()
                .allMatch(permission -> hasPermission(userId, permission));
    }
    
    /**
     * Check if user has any of the specified permissions.
     */
    @Transactional(readOnly = true)
    public boolean hasAnyPermission(Long userId, List<String> permissionNames) {
        return permissionNames.stream()
                .anyMatch(permission -> hasPermission(userId, permission));
    }
    
    /**
     * Require user to have permission, throw exception if not.
     */
    public void requirePermission(Long userId, String permissionName) {
        if (!hasPermission(userId, permissionName)) {
            log.warn("Access denied: userId={}, permission={}", userId, permissionName);
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, 
                    "You don't have permission: " + permissionName);
        }
    }
    
    /**
     * Require user to have all permissions.
     */
    public void requireAllPermissions(Long userId, List<String> permissionNames) {
        List<String> missingPermissions = permissionNames.stream()
                .filter(permission -> !hasPermission(userId, permission))
                .collect(Collectors.toList());
        
        if (!missingPermissions.isEmpty()) {
            log.warn("Access denied: userId={}, missingPermissions={}", userId, missingPermissions);
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS, 
                    "Missing permissions: " + String.join(", ", missingPermissions));
        }
    }
    
    // -------------------------------------------------------------------------
    // User Permissions
    // -------------------------------------------------------------------------
    
    /**
     * Get all permissions for a user.
     */
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
        
        RoleType roleType = mapUserRoleToRoleType(user.getRole());
        
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
        
        return role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
    
    // -------------------------------------------------------------------------
    // Role Management
    // -------------------------------------------------------------------------
    
    /**
     * Get all available roles.
     */
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAllOrderedByLevel();
    }
    
    /**
     * Get role by type.
     */
    @Transactional(readOnly = true)
    public Role getRole(RoleType roleType) {
        return roleRepository.findByName(roleType)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
    }
    
    /**
     * Get roles that can be assigned by a user with specific role.
     * Rule: Can only assign roles with lower level.
     */
    @Transactional(readOnly = true)
    public List<Role> getAssignableRoles(RoleType currentRole) {
        Role role = getRole(currentRole);
        return roleRepository.findRolesWithLevelLessThanOrEqual(role.getLevel() - 1);
    }
    
    /**
     * Check if user can assign a specific role.
     */
    public boolean canAssignRole(RoleType assignerRole, RoleType targetRole) {
        return assignerRole.canAssignRole(targetRole);
    }
    
    // -------------------------------------------------------------------------
    // Permission Management
    // -------------------------------------------------------------------------
    
    /**
     * Get all permissions.
     */
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }
    
    /**
     * Get permissions by resource.
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionsByResource(String resource) {
        return permissionRepository.findByResourceIncludingWildcard(resource);
    }
    
    /**
     * Get permission by name.
     */
    @Transactional(readOnly = true)
    public Permission getPermission(String permissionName) {
        return permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, 
                        "Permission not found: " + permissionName));
    }
    
    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    /**
     * Map old UserRole enum to new RoleType enum.
     * This is a temporary bridge while we migrate from UserRole to RoleType.
     */
    private RoleType mapUserRoleToRoleType(UserRole userRole) {
        if (userRole == null) {
            return RoleType.USER; // Default
        }
        
        return switch (userRole) {
            case ROLE_USER -> RoleType.USER;
            case ROLE_ADMIN -> RoleType.ADMIN;
            case ROLE_SUPER_ADMIN -> RoleType.SUPER_ADMIN;
            default -> RoleType.USER;
        };
    }
    
    /**
     * Map RoleType to UserRole (for backward compatibility).
     */
    private UserRole mapRoleTypeToUserRole(RoleType roleType) {
        if (roleType == null) {
            return UserRole.ROLE_USER;
        }
        
        return switch (roleType) {
            case USER -> UserRole.ROLE_USER;
            case ADMIN -> UserRole.ROLE_ADMIN;
            case SUPER_ADMIN -> UserRole.ROLE_SUPER_ADMIN;
            case MANAGER, GUEST -> UserRole.ROLE_USER; // Map to USER for now
            default -> UserRole.ROLE_USER;
        };
    }
}
