-- V15: Notifications System
-- Purpose: Real-time and email notifications for users
-- Author: Ziboto Team
-- Date: August 11, 2026

-- ============================================================================
-- ENUM: Notification Type
-- ============================================================================

CREATE TYPE notification_type AS ENUM (
    'FILE_SHARED',
    'FILE_UPLOADED',
    'FILE_DELETED',
    'SHARE_ACCEPTED',
    'SHARE_DECLINED',
    'STORAGE_QUOTA_WARNING',
    'STORAGE_QUOTA_EXCEEDED',
    'DUPLICATE_DETECTED',
    'VERSION_CREATED',
    'SYSTEM_ANNOUNCEMENT',
    'SECURITY_ALERT'
);

-- ============================================================================
-- ENUM: Notification Status
-- ============================================================================

CREATE TYPE notification_status AS ENUM (
    'UNREAD',
    'READ',
    'ARCHIVED'
);

-- ============================================================================
-- ENUM: Notification Priority
-- ============================================================================

CREATE TYPE notification_priority AS ENUM (
    'LOW',
    'NORMAL',
    'HIGH',
    'URGENT'
);

-- ============================================================================
-- TABLE: notifications
-- Purpose: Store all user notifications
-- ============================================================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Recipient
    user_id BIGINT NOT NULL,
    
    -- Notification details
    type notification_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    priority notification_priority DEFAULT 'NORMAL',
    
    -- Related entities
    related_entity_type VARCHAR(50), -- 'FILE', 'FOLDER', 'USER', 'SHARE'
    related_entity_id UUID,
    
    -- Action link (optional)
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    
    -- Status
    status notification_status DEFAULT 'UNREAD',
    read_at TIMESTAMP,
    
    -- Delivery channels
    sent_via_websocket BOOLEAN DEFAULT FALSE,
    sent_via_email BOOLEAN DEFAULT FALSE,
    email_sent_at TIMESTAMP,
    
    -- Metadata
    metadata JSONB, -- Additional data for notification
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP, -- Auto-delete after expiration
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES for Performance
-- ============================================================================

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_priority ON notifications(priority);
CREATE INDEX idx_notifications_expires_at ON notifications(expires_at) WHERE expires_at IS NOT NULL;

-- ============================================================================
-- TABLE: notification_preferences
-- Purpose: User notification preferences
-- ============================================================================

CREATE TABLE notification_preferences (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    
    -- Channel preferences
    enable_websocket BOOLEAN DEFAULT TRUE,
    enable_email BOOLEAN DEFAULT TRUE,
    enable_push BOOLEAN DEFAULT FALSE,
    
    -- Notification type preferences (JSONB for flexibility)
    preferences JSONB DEFAULT '{
        "FILE_SHARED": {"email": true, "websocket": true},
        "FILE_UPLOADED": {"email": false, "websocket": true},
        "FILE_DELETED": {"email": false, "websocket": true},
        "SHARE_ACCEPTED": {"email": true, "websocket": true},
        "SHARE_DECLINED": {"email": true, "websocket": true},
        "STORAGE_QUOTA_WARNING": {"email": true, "websocket": true},
        "STORAGE_QUOTA_EXCEEDED": {"email": true, "websocket": true},
        "DUPLICATE_DETECTED": {"email": false, "websocket": true},
        "VERSION_CREATED": {"email": false, "websocket": true},
        "SYSTEM_ANNOUNCEMENT": {"email": true, "websocket": true},
        "SECURITY_ALERT": {"email": true, "websocket": true}
    }'::jsonb,
    
    -- Email preferences
    digest_mode BOOLEAN DEFAULT FALSE, -- Send daily/weekly digest instead of instant
    digest_frequency VARCHAR(20) DEFAULT 'DAILY', -- DAILY, WEEKLY, MONTHLY
    
    -- Quiet hours
    quiet_hours_enabled BOOLEAN DEFAULT FALSE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_prefs_user ON notification_preferences(user_id);

-- ============================================================================
-- VIEW: unread_notification_counts
-- Purpose: Quick lookup for unread counts per user
-- ============================================================================

CREATE VIEW unread_notification_counts AS
SELECT 
    user_id,
    COUNT(*) as unread_count,
    COUNT(*) FILTER (WHERE priority = 'URGENT') as urgent_count,
    COUNT(*) FILTER (WHERE priority = 'HIGH') as high_priority_count
FROM notifications
WHERE status = 'UNREAD'
GROUP BY user_id;

-- ============================================================================
-- FUNCTION: Get unread notifications for user
-- ============================================================================

CREATE OR REPLACE FUNCTION get_unread_notifications(p_user_id BIGINT, p_limit INT DEFAULT 50)
RETURNS TABLE(
    id UUID,
    type notification_type,
    title VARCHAR,
    message TEXT,
    priority notification_priority,
    related_entity_type VARCHAR,
    related_entity_id UUID,
    action_url VARCHAR,
    action_label VARCHAR,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        n.id,
        n.type,
        n.title,
        n.message,
        n.priority,
        n.related_entity_type,
        n.related_entity_id,
        n.action_url,
        n.action_label,
        n.created_at
    FROM notifications n
    WHERE n.user_id = p_user_id
    AND n.status = 'UNREAD'
    ORDER BY 
        CASE n.priority
            WHEN 'URGENT' THEN 1
            WHEN 'HIGH' THEN 2
            WHEN 'NORMAL' THEN 3
            WHEN 'LOW' THEN 4
        END,
        n.created_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Mark notification as read
-- ============================================================================

CREATE OR REPLACE FUNCTION mark_notification_read(p_notification_id UUID, p_user_id BIGINT)
RETURNS BOOLEAN AS $$
DECLARE
    rows_affected INT;
BEGIN
    UPDATE notifications
    SET status = 'READ',
        read_at = NOW()
    WHERE id = p_notification_id
    AND user_id = p_user_id
    AND status = 'UNREAD';
    
    GET DIAGNOSTICS rows_affected = ROW_COUNT;
    RETURN rows_affected > 0;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Mark all notifications as read for user
-- ============================================================================

CREATE OR REPLACE FUNCTION mark_all_notifications_read(p_user_id BIGINT)
RETURNS INT AS $$
DECLARE
    rows_affected INT;
BEGIN
    UPDATE notifications
    SET status = 'READ',
        read_at = NOW()
    WHERE user_id = p_user_id
    AND status = 'UNREAD';
    
    GET DIAGNOSTICS rows_affected = ROW_COUNT;
    RETURN rows_affected;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Delete expired notifications
-- ============================================================================

CREATE OR REPLACE FUNCTION delete_expired_notifications()
RETURNS INT AS $$
DECLARE
    rows_deleted INT;
BEGIN
    DELETE FROM notifications
    WHERE expires_at IS NOT NULL
    AND expires_at < NOW();
    
    GET DIAGNOSTICS rows_deleted = ROW_COUNT;
    RETURN rows_deleted;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Create notification preferences for new user
-- ============================================================================

CREATE OR REPLACE FUNCTION create_default_notification_preferences()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO notification_preferences (user_id)
    VALUES (NEW.id);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_create_notification_preferences
AFTER INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION create_default_notification_preferences();

-- ============================================================================
-- TRIGGER: Update timestamps
-- ============================================================================

CREATE OR REPLACE FUNCTION update_notification_prefs_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_notification_prefs_updated_at
BEFORE UPDATE ON notification_preferences
FOR EACH ROW
EXECUTE FUNCTION update_notification_prefs_timestamp();

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE notifications IS 'User notifications for various events';
COMMENT ON TABLE notification_preferences IS 'User preferences for notification delivery';
COMMENT ON VIEW unread_notification_counts IS 'Quick lookup for unread notification counts';

COMMENT ON COLUMN notifications.type IS 'Type of notification event';
COMMENT ON COLUMN notifications.priority IS 'Urgency level of notification';
COMMENT ON COLUMN notifications.related_entity_id IS 'ID of related file, folder, share, etc.';
COMMENT ON COLUMN notifications.metadata IS 'Additional JSON data for notification';
COMMENT ON COLUMN notifications.expires_at IS 'Auto-delete notification after this date';

COMMENT ON COLUMN notification_preferences.preferences IS 'JSONB map of notification type to channel preferences';
COMMENT ON COLUMN notification_preferences.digest_mode IS 'Send notifications as daily/weekly digest';
COMMENT ON COLUMN notification_preferences.quiet_hours_enabled IS 'Do not send notifications during quiet hours';

-- ============================================================================
-- Sample Queries
-- ============================================================================

-- Get unread notifications
-- SELECT * FROM get_unread_notifications(1);

-- Mark notification as read
-- SELECT mark_notification_read('notification-id', 1);

-- Mark all as read
-- SELECT mark_all_notifications_read(1);

-- Get unread counts
-- SELECT * FROM unread_notification_counts WHERE user_id = 1;

-- Delete expired notifications (run as cron job)
-- SELECT delete_expired_notifications();
