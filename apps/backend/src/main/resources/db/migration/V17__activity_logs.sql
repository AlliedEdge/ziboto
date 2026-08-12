-- =====================================================
-- ZIBOTO V3: ACTIVITY LOGS
-- Migration: V17
-- Description: Activity feed and user history tracking
-- Author: Ziboto Team
-- Created: 2026-08-11
-- =====================================================

-- Create activity_type enum
CREATE TYPE activity_type AS ENUM (
    'FILE_UPLOADED',
    'FILE_DOWNLOADED',
    'FILE_DELETED',
    'FILE_UPDATED',
    'FILE_SHARED',
    'FILE_UNSHARED',
    'FILE_RESTORED',
    'FOLDER_CREATED',
    'FOLDER_DELETED',
    'FOLDER_RENAMED',
    'FOLDER_MOVED',
    'USER_LOGIN',
    'USER_LOGOUT',
    'USER_REGISTERED',
    'USER_UPDATED',
    'SHARE_ACCEPTED',
    'SHARE_DECLINED',
    'VERSION_CREATED',
    'VERSION_RESTORED',
    'COMMENT_ADDED',
    'COMMENT_DELETED',
    'DUPLICATE_DETECTED',
    'DUPLICATE_REMOVED'
);

-- Create entity_type enum
CREATE TYPE entity_type AS ENUM (
    'FILE',
    'FOLDER',
    'USER',
    'SHARE',
    'VERSION',
    'COMMENT',
    'DUPLICATE',
    'SYSTEM'
);

-- Create activity_logs table
CREATE TABLE activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_type activity_type NOT NULL,
    entity_type entity_type NOT NULL,
    entity_id UUID, -- Can be null for system-level activities
    entity_name VARCHAR(500), -- For display purposes
    action VARCHAR(100) NOT NULL, -- created, updated, deleted, etc.
    description TEXT, -- Human-readable description
    metadata JSONB DEFAULT '{}', -- Additional context (file size, share recipient, etc.)
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for common queries
    CONSTRAINT activity_logs_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for efficient querying
CREATE INDEX idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_activity_type ON activity_logs(activity_type);
CREATE INDEX idx_activity_logs_entity_type ON activity_logs(entity_type);
CREATE INDEX idx_activity_logs_entity_id ON activity_logs(entity_id);
CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at DESC);
CREATE INDEX idx_activity_logs_user_created ON activity_logs(user_id, created_at DESC);

-- Composite index for entity-specific queries
CREATE INDEX idx_activity_logs_entity ON activity_logs(entity_type, entity_id, created_at DESC);

-- Index for activity type filtering
CREATE INDEX idx_activity_logs_user_type ON activity_logs(user_id, activity_type, created_at DESC);

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Function to get recent user activities
CREATE OR REPLACE FUNCTION get_user_activities(
    p_user_id BIGINT,
    p_limit INT DEFAULT 50,
    p_offset INT DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    activity_type activity_type,
    entity_type entity_type,
    entity_id UUID,
    entity_name VARCHAR,
    action VARCHAR,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        al.id,
        al.activity_type,
        al.entity_type,
        al.entity_id,
        al.entity_name,
        al.action,
        al.description,
        al.metadata,
        al.created_at
    FROM activity_logs al
    WHERE al.user_id = p_user_id
    ORDER BY al.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Function to get global activities (admin only)
CREATE OR REPLACE FUNCTION get_global_activities(
    p_limit INT DEFAULT 100,
    p_offset INT DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    user_id BIGINT,
    username VARCHAR,
    activity_type activity_type,
    entity_type entity_type,
    entity_id UUID,
    entity_name VARCHAR,
    action VARCHAR,
    description TEXT,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        al.id,
        al.user_id,
        u.username,
        al.activity_type,
        al.entity_type,
        al.entity_id,
        al.entity_name,
        al.action,
        al.description,
        al.created_at
    FROM activity_logs al
    INNER JOIN users u ON al.user_id = u.id
    ORDER BY al.created_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Function to get entity-specific activities
CREATE OR REPLACE FUNCTION get_entity_activities(
    p_entity_id UUID,
    p_entity_type entity_type,
    p_limit INT DEFAULT 50
)
RETURNS TABLE (
    id UUID,
    user_id BIGINT,
    username VARCHAR,
    activity_type activity_type,
    action VARCHAR,
    description TEXT,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        al.id,
        al.user_id,
        u.username,
        al.activity_type,
        al.action,
        al.description,
        al.created_at
    FROM activity_logs al
    INNER JOIN users u ON al.user_id = u.id
    WHERE al.entity_id = p_entity_id
      AND al.entity_type = p_entity_type
    ORDER BY al.created_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Function to get activity summary for user
CREATE OR REPLACE FUNCTION get_user_activity_summary(
    p_user_id BIGINT,
    p_days INT DEFAULT 30
)
RETURNS TABLE (
    activity_type activity_type,
    count BIGINT,
    last_activity TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        al.activity_type,
        COUNT(*)::BIGINT as count,
        MAX(al.created_at) as last_activity
    FROM activity_logs al
    WHERE al.user_id = p_user_id
      AND al.created_at >= CURRENT_TIMESTAMP - (p_days || ' days')::INTERVAL
    GROUP BY al.activity_type
    ORDER BY count DESC;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup old activity logs (retention policy)
CREATE OR REPLACE FUNCTION cleanup_old_activities(
    p_days_to_keep INT DEFAULT 365
)
RETURNS INT AS $$
DECLARE
    deleted_count INT;
BEGIN
    DELETE FROM activity_logs
    WHERE created_at < CURRENT_TIMESTAMP - (p_days_to_keep || ' days')::INTERVAL;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE activity_logs IS 'Tracks all user activities for audit and history';
COMMENT ON COLUMN activity_logs.id IS 'Primary key';
COMMENT ON COLUMN activity_logs.user_id IS 'User who performed the activity';
COMMENT ON COLUMN activity_logs.activity_type IS 'Type of activity (upload, download, etc.)';
COMMENT ON COLUMN activity_logs.entity_type IS 'Type of entity affected (file, folder, etc.)';
COMMENT ON COLUMN activity_logs.entity_id IS 'ID of the affected entity';
COMMENT ON COLUMN activity_logs.entity_name IS 'Name of the entity for display';
COMMENT ON COLUMN activity_logs.action IS 'Action performed (created, updated, deleted)';
COMMENT ON COLUMN activity_logs.description IS 'Human-readable description of the activity';
COMMENT ON COLUMN activity_logs.metadata IS 'Additional context stored as JSON';
COMMENT ON COLUMN activity_logs.ip_address IS 'IP address of the user';
COMMENT ON COLUMN activity_logs.user_agent IS 'Browser/client user agent';
COMMENT ON COLUMN activity_logs.created_at IS 'When the activity occurred';

COMMENT ON FUNCTION get_user_activities IS 'Retrieves paginated activity logs for a specific user';
COMMENT ON FUNCTION get_global_activities IS 'Retrieves paginated activity logs for all users (admin)';
COMMENT ON FUNCTION get_entity_activities IS 'Retrieves activity logs for a specific entity';
COMMENT ON FUNCTION get_user_activity_summary IS 'Returns activity summary statistics for a user';
COMMENT ON FUNCTION cleanup_old_activities IS 'Deletes activity logs older than specified days';

-- =====================================================
-- GRANT PERMISSIONS
-- =====================================================

-- Grant permissions to application user (if needed)
-- GRANT ALL ON TABLE activity_logs TO ziboto_app_user;
-- GRANT EXECUTE ON FUNCTION get_user_activities TO ziboto_app_user;
-- GRANT EXECUTE ON FUNCTION get_global_activities TO ziboto_app_user;
-- GRANT EXECUTE ON FUNCTION get_entity_activities TO ziboto_app_user;
-- GRANT EXECUTE ON FUNCTION get_user_activity_summary TO ziboto_app_user;
-- GRANT EXECUTE ON FUNCTION cleanup_old_activities TO ziboto_app_user;
