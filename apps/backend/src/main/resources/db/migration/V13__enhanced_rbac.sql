-- V13: Enhanced RBAC (Role-Based Access Control)
-- Purpose: Fine-grained permissions and role hierarchy
-- Author: Ziboto Team
-- Date: August 11, 2026

-- ============================================================================
-- ENUM: Role Types
-- ============================================================================

CREATE TYPE role_type AS ENUM (
    'SUPER_ADMIN',  -- Full system access
    'ADMIN',        -- Organization admin
    'MANAGER',      -- Team manager
    'USER',         -- Regular user (default)
    'GUEST'         -- Read-only access
);

-- ============================================================================
-- TABLE: roles
-- Purpose: Define available roles in the system
-- ============================================================================

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name role_type NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Hierarchy (for role inheritance)
    level INT NOT NULL, -- Higher level = more permissions
    parent_role role_type,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Insert default roles
INSERT INTO roles (name, display_name, description, level) VALUES
('GUEST', 'Guest', 'Read-only access to shared files', 1),
('USER', 'User', 'Regular user with full file management', 2),
('MANAGER', 'Manager', 'Team manager with user management', 3),
('ADMIN', 'Admin', 'Organization admin with system configuration', 4),
('SUPER_ADMIN', 'Super Admin', 'Full system access', 5);

-- Set parent roles for hierarchy
UPDATE roles SET parent_role = NULL WHERE name = 'GUEST';
UPDATE roles SET parent_role = 'GUEST' WHERE name = 'USER';
UPDATE roles SET parent_role = 'USER' WHERE name = 'MANAGER';
UPDATE roles SET parent_role = 'MANAGER' WHERE name = 'ADMIN';
UPDATE roles SET parent_role = 'ADMIN' WHERE name = 'SUPER_ADMIN';

-- ============================================================================
-- TABLE: permissions
-- Purpose: Define granular permissions
-- ============================================================================

CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE, -- e.g., "files:create", "users:delete"
    resource VARCHAR(50) NOT NULL,     -- e.g., "files", "users", "folders"
    action VARCHAR(50) NOT NULL,       -- e.g., "create", "read", "update", "delete"
    description TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create index for fast permission lookup
CREATE INDEX idx_permissions_name ON permissions(name);
CREATE INDEX idx_permissions_resource ON permissions(resource);

-- Insert default permissions
-- File permissions
INSERT INTO permissions (name, resource, action, description) VALUES
('files:create', 'files', 'create', 'Upload new files'),
('files:read', 'files', 'read', 'View and download files'),
('files:update', 'files', 'update', 'Update file metadata'),
('files:delete', 'files', 'delete', 'Delete files'),
('files:share', 'files', 'share', 'Share files with others'),
('files:*', 'files', '*', 'All file operations');

-- Folder permissions
INSERT INTO permissions (name, resource, action, description) VALUES
('folders:create', 'folders', 'create', 'Create new folders'),
('folders:read', 'folders', 'read', 'View folders'),
('folders:update', 'folders', 'update', 'Rename folders'),
('folders:delete', 'folders', 'delete', 'Delete folders'),
('folders:*', 'folders', '*', 'All folder operations');

-- User permissions
INSERT INTO permissions (name, resource, action, description) VALUES
('users:create', 'users', 'create', 'Create new users'),
('users:read', 'users', 'read', 'View user profiles'),
('users:update', 'users', 'update', 'Update user profiles'),
('users:delete', 'users', 'delete', 'Delete users'),
('users:*', 'users', '*', 'All user operations');

-- Role permissions
INSERT INTO permissions (name, resource, action, description) VALUES
('roles:assign', 'roles', 'assign', 'Assign roles to users'),
('roles:read', 'roles', 'read', 'View roles'),
('roles:*', 'roles', '*', 'All role operations');

-- System permissions
INSERT INTO permissions (name, resource, action, description) VALUES
('system:config', 'system', 'config', 'Modify system configuration'),
('system:audit', 'system', 'audit', 'View audit logs'),
('system:*', 'system', '*', 'All system operations');

-- ============================================================================
-- TABLE: role_permissions
-- Purpose: Map roles to permissions
-- ============================================================================

CREATE TABLE role_permissions (
    id SERIAL PRIMARY KEY,
    role_name role_type NOT NULL,
    permission_id INT NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE (role_name, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_name);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- Assign permissions to roles
-- GUEST: Read-only
INSERT INTO role_permissions (role_name, permission_id)
SELECT 'GUEST', id FROM permissions WHERE name IN ('files:read', 'folders:read');

-- USER: Full file/folder management
INSERT INTO role_permissions (role_name, permission_id)
SELECT 'USER', id FROM permissions WHERE name IN (
    'files:*', 'folders:*', 'users:read'
);

-- MANAGER: User management + USER permissions
INSERT INTO role_permissions (role_name, permission_id)
SELECT 'MANAGER', id FROM permissions WHERE name IN (
    'files:*', 'folders:*', 'users:read', 'users:update', 'roles:read'
);

-- ADMIN: System config + MANAGER permissions
INSERT INTO role_permissions (role_name, permission_id)
SELECT 'ADMIN', id FROM permissions WHERE name IN (
    'files:*', 'folders:*', 'users:*', 'roles:*', 'system:config', 'system:audit'
);

-- SUPER_ADMIN: Everything
INSERT INTO role_permissions (role_name, permission_id)
SELECT 'SUPER_ADMIN', id FROM permissions;

-- ============================================================================
-- TABLE: user_roles (modify existing users table)
-- ============================================================================

-- Add role column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS role role_type NOT NULL DEFAULT 'USER';

-- Create index for role-based queries
CREATE INDEX idx_users_role ON users(role);

-- Update existing users to USER role (if not already set)
UPDATE users SET role = 'USER' WHERE role IS NULL;

-- ============================================================================
-- FUNCTION: Check if user has permission
-- ============================================================================

CREATE OR REPLACE FUNCTION user_has_permission(p_user_id BIGINT, p_permission VARCHAR)
RETURNS BOOLEAN AS $$
DECLARE
    user_role role_type;
    has_permission BOOLEAN;
BEGIN
    -- Get user's role
    SELECT role INTO user_role FROM users WHERE id = p_user_id;
    
    IF user_role IS NULL THEN
        RETURN FALSE;
    END IF;
    
    -- Check if role has the permission (including wildcard *)
    SELECT EXISTS (
        SELECT 1
        FROM role_permissions rp
        JOIN permissions p ON rp.permission_id = p.id
        WHERE rp.role_name = user_role
        AND (p.name = p_permission OR p.name LIKE SPLIT_PART(p_permission, ':', 1) || ':*')
    ) INTO has_permission;
    
    RETURN has_permission;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Get all user permissions (including inherited from role hierarchy)
-- ============================================================================

CREATE OR REPLACE FUNCTION get_user_permissions(p_user_id BIGINT)
RETURNS TABLE(permission_name VARCHAR, permission_description TEXT) AS $$
DECLARE
    user_role role_type;
BEGIN
    -- Get user's role
    SELECT role INTO user_role FROM users WHERE id = p_user_id;
    
    IF user_role IS NULL THEN
        RETURN;
    END IF;
    
    -- Return all permissions for this role
    RETURN QUERY
    SELECT p.name, p.description
    FROM role_permissions rp
    JOIN permissions p ON rp.permission_id = p.id
    WHERE rp.role_name = user_role
    ORDER BY p.resource, p.action;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TABLE: permission_overrides (optional - for user-specific permissions)
-- ============================================================================

CREATE TABLE permission_overrides (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id INT NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT TRUE, -- TRUE = grant, FALSE = revoke
    reason TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    expires_at TIMESTAMP, -- Optional expiration
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE (user_id, permission_id)
);

CREATE INDEX idx_permission_overrides_user ON permission_overrides(user_id);

-- ============================================================================
-- TRIGGER: Update timestamps
-- ============================================================================

CREATE OR REPLACE FUNCTION update_rbac_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_roles_updated_at
BEFORE UPDATE ON roles
FOR EACH ROW
EXECUTE FUNCTION update_rbac_updated_at();

CREATE TRIGGER trigger_permissions_updated_at
BEFORE UPDATE ON permissions
FOR EACH ROW
EXECUTE FUNCTION update_rbac_updated_at();

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE roles IS 'System roles with hierarchical structure';
COMMENT ON TABLE permissions IS 'Granular permissions for resources and actions';
COMMENT ON TABLE role_permissions IS 'Maps roles to permissions';
COMMENT ON TABLE permission_overrides IS 'User-specific permission grants or revocations';

COMMENT ON COLUMN roles.level IS 'Role hierarchy level (higher = more permissions)';
COMMENT ON COLUMN roles.parent_role IS 'Parent role for permission inheritance';
COMMENT ON COLUMN permissions.name IS 'Permission identifier in format resource:action';
COMMENT ON COLUMN permission_overrides.granted IS 'TRUE to grant, FALSE to revoke permission';

-- ============================================================================
-- Sample Queries
-- ============================================================================

-- Check if user has permission
-- SELECT user_has_permission(1, 'files:delete');

-- Get all user permissions
-- SELECT * FROM get_user_permissions(1);

-- Get users by role
-- SELECT * FROM users WHERE role = 'ADMIN';

-- Get all permissions for a role
-- SELECT p.* FROM role_permissions rp
-- JOIN permissions p ON rp.permission_id = p.id
-- WHERE rp.role_name = 'MANAGER';
