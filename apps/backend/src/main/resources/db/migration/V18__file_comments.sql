-- =====================================================
-- ZIBOTO V3: FILE COMMENTS
-- Migration: V18
-- Description: Commenting system for files
-- Author: Ziboto Team
-- Created: 2026-08-11
-- =====================================================

-- Create file_comments table
CREATE TABLE file_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES file_metadata(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES file_comments(id) ON DELETE CASCADE, -- For threaded comments
    content TEXT NOT NULL,
    mentions JSONB DEFAULT '[]', -- Array of mentioned user IDs
    is_edited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT file_comments_file_id_fk FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE CASCADE,
    CONSTRAINT file_comments_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT file_comments_parent_id_fk FOREIGN KEY (parent_id) REFERENCES file_comments(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_file_comments_file_id ON file_comments(file_id);
CREATE INDEX idx_file_comments_user_id ON file_comments(user_id);
CREATE INDEX idx_file_comments_parent_id ON file_comments(parent_id);
CREATE INDEX idx_file_comments_created_at ON file_comments(created_at DESC);
CREATE INDEX idx_file_comments_file_created ON file_comments(file_id, created_at DESC);

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Get comments for a file (with threading)
CREATE OR REPLACE FUNCTION get_file_comments(
    p_file_id UUID,
    p_limit INT DEFAULT 100
)
RETURNS TABLE (
    id UUID,
    file_id UUID,
    user_id BIGINT,
    username VARCHAR,
    parent_id UUID,
    content TEXT,
    mentions JSONB,
    is_edited BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    reply_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE comment_tree AS (
        -- Root comments (no parent)
        SELECT 
            c.id,
            c.file_id,
            c.user_id,
            u.username,
            c.parent_id,
            c.content,
            c.mentions,
            c.is_edited,
            c.created_at,
            c.updated_at,
            0 as depth,
            c.created_at as root_created_at
        FROM file_comments c
        INNER JOIN users u ON c.user_id = u.id
        WHERE c.file_id = p_file_id
          AND c.parent_id IS NULL
        
        UNION ALL
        
        -- Child comments
        SELECT 
            c.id,
            c.file_id,
            c.user_id,
            u.username,
            c.parent_id,
            c.content,
            c.mentions,
            c.is_edited,
            c.created_at,
            c.updated_at,
            ct.depth + 1,
            ct.root_created_at
        FROM file_comments c
        INNER JOIN users u ON c.user_id = u.id
        INNER JOIN comment_tree ct ON c.parent_id = ct.id
        WHERE ct.depth < 10  -- Prevent infinite recursion
    )
    SELECT 
        ct.id,
        ct.file_id,
        ct.user_id,
        ct.username,
        ct.parent_id,
        ct.content,
        ct.mentions,
        ct.is_edited,
        ct.created_at,
        ct.updated_at,
        (SELECT COUNT(*) FROM file_comments WHERE parent_id = ct.id)::BIGINT as reply_count
    FROM comment_tree ct
    ORDER BY ct.root_created_at DESC, ct.depth ASC, ct.created_at ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Get comment count for file
CREATE OR REPLACE FUNCTION get_comment_count(p_file_id UUID)
RETURNS BIGINT AS $$
BEGIN
    RETURN (SELECT COUNT(*) FROM file_comments WHERE file_id = p_file_id);
END;
$$ LANGUAGE plpgsql;

-- Trigger to update updated_at
CREATE OR REPLACE FUNCTION update_comment_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER file_comments_update_timestamp
BEFORE UPDATE ON file_comments
FOR EACH ROW
EXECUTE FUNCTION update_comment_timestamp();

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE file_comments IS 'Stores comments on files with threading support';
COMMENT ON COLUMN file_comments.id IS 'Primary key';
COMMENT ON COLUMN file_comments.file_id IS 'File being commented on';
COMMENT ON COLUMN file_comments.user_id IS 'User who wrote the comment';
COMMENT ON COLUMN file_comments.parent_id IS 'Parent comment for threaded replies';
COMMENT ON COLUMN file_comments.content IS 'Comment text content';
COMMENT ON COLUMN file_comments.mentions IS 'JSON array of mentioned user IDs';
COMMENT ON COLUMN file_comments.is_edited IS 'Whether comment has been edited';

COMMENT ON FUNCTION get_file_comments IS 'Retrieves threaded comments for a file';
COMMENT ON FUNCTION get_comment_count IS 'Returns total comment count for a file';
