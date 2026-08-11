-- =====================================================
-- ZIBOTO V3: TRASH BIN
-- Migration: V19
-- Description: Soft delete with recovery option
-- Author: Ziboto Team
-- Created: 2026-08-11
-- =====================================================

-- Add deleted_at column to file_metadata for soft delete
ALTER TABLE file_metadata 
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);

-- Create index for trash queries
CREATE INDEX idx_file_metadata_deleted_at ON file_metadata(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_file_metadata_user_deleted ON file_metadata(user_id, deleted_at) WHERE deleted_at IS NOT NULL;

-- Add deleted_at column to folders for soft delete
ALTER TABLE folders 
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);

-- Create index for trash queries
CREATE INDEX idx_folders_deleted_at ON folders(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_folders_user_deleted ON folders(user_id, deleted_at) WHERE deleted_at IS NOT NULL;

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Get trash items for user
CREATE OR REPLACE FUNCTION get_user_trash(
    p_user_id BIGINT,
    p_limit INT DEFAULT 100,
    p_offset INT DEFAULT 0
)
RETURNS TABLE (
    id UUID,
    item_type VARCHAR,
    name VARCHAR,
    size BIGINT,
    deleted_at TIMESTAMP,
    deleted_by VARCHAR,
    auto_delete_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    -- Files in trash
    SELECT 
        fm.id,
        'FILE'::VARCHAR as item_type,
        fm.file_name as name,
        fm.file_size as size,
        fm.deleted_at,
        fm.deleted_by,
        (fm.deleted_at + INTERVAL '30 days') as auto_delete_at
    FROM file_metadata fm
    WHERE fm.user_id = p_user_id
      AND fm.deleted_at IS NOT NULL
    
    UNION ALL
    
    -- Folders in trash
    SELECT 
        f.id,
        'FOLDER'::VARCHAR as item_type,
        f.folder_name as name,
        0::BIGINT as size,
        f.deleted_at,
        f.deleted_by,
        (f.deleted_at + INTERVAL '30 days') as auto_delete_at
    FROM folders f
    WHERE f.user_id = p_user_id
      AND f.deleted_at IS NOT NULL
    
    ORDER BY deleted_at DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Count trash items for user
CREATE OR REPLACE FUNCTION count_user_trash(p_user_id BIGINT)
RETURNS TABLE (
    total_items BIGINT,
    total_size BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        (SELECT COUNT(*) FROM file_metadata WHERE user_id = p_user_id AND deleted_at IS NOT NULL) +
        (SELECT COUNT(*) FROM folders WHERE user_id = p_user_id AND deleted_at IS NOT NULL) as total_items,
        COALESCE((SELECT SUM(file_size) FROM file_metadata WHERE user_id = p_user_id AND deleted_at IS NOT NULL), 0) as total_size;
END;
$$ LANGUAGE plpgsql;

-- Restore file from trash
CREATE OR REPLACE FUNCTION restore_file_from_trash(
    p_file_id UUID,
    p_user_id BIGINT
)
RETURNS BOOLEAN AS $$
DECLARE
    v_count INT;
BEGIN
    UPDATE file_metadata
    SET deleted_at = NULL,
        deleted_by = NULL
    WHERE id = p_file_id
      AND user_id = p_user_id
      AND deleted_at IS NOT NULL;
    
    GET DIAGNOSTICS v_count = ROW_COUNT;
    
    RETURN v_count > 0;
END;
$$ LANGUAGE plpgsql;

-- Restore folder from trash
CREATE OR REPLACE FUNCTION restore_folder_from_trash(
    p_folder_id UUID,
    p_user_id BIGINT
)
RETURNS BOOLEAN AS $$
DECLARE
    v_count INT;
BEGIN
    UPDATE folders
    SET deleted_at = NULL,
        deleted_by = NULL
    WHERE id = p_folder_id
      AND user_id = p_user_id
      AND deleted_at IS NOT NULL;
    
    GET DIAGNOSTICS v_count = ROW_COUNT;
    
    RETURN v_count > 0;
END;
$$ LANGUAGE plpgsql;

-- Permanently delete old trash items (retention policy)
CREATE OR REPLACE FUNCTION cleanup_old_trash(p_days_to_keep INT DEFAULT 30)
RETURNS TABLE (
    files_deleted INT,
    folders_deleted INT
) AS $$
DECLARE
    v_cutoff_date TIMESTAMP;
    v_files_deleted INT;
    v_folders_deleted INT;
BEGIN
    v_cutoff_date := CURRENT_TIMESTAMP - (p_days_to_keep || ' days')::INTERVAL;
    
    -- Delete files
    DELETE FROM file_metadata
    WHERE deleted_at IS NOT NULL
      AND deleted_at < v_cutoff_date;
    
    GET DIAGNOSTICS v_files_deleted = ROW_COUNT;
    
    -- Delete folders
    DELETE FROM folders
    WHERE deleted_at IS NOT NULL
      AND deleted_at < v_cutoff_date;
    
    GET DIAGNOSTICS v_folders_deleted = ROW_COUNT;
    
    RETURN QUERY SELECT v_files_deleted, v_folders_deleted;
END;
$$ LANGUAGE plpgsql;

-- Empty trash for user (permanent delete all)
CREATE OR REPLACE FUNCTION empty_user_trash(p_user_id BIGINT)
RETURNS TABLE (
    files_deleted INT,
    folders_deleted INT
) AS $$
DECLARE
    v_files_deleted INT;
    v_folders_deleted INT;
BEGIN
    -- Delete files
    DELETE FROM file_metadata
    WHERE user_id = p_user_id
      AND deleted_at IS NOT NULL;
    
    GET DIAGNOSTICS v_files_deleted = ROW_COUNT;
    
    -- Delete folders
    DELETE FROM folders
    WHERE user_id = p_user_id
      AND deleted_at IS NOT NULL;
    
    GET DIAGNOSTICS v_folders_deleted = ROW_COUNT;
    
    RETURN QUERY SELECT v_files_deleted, v_folders_deleted;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON COLUMN file_metadata.deleted_at IS 'Timestamp when file was moved to trash (soft delete)';
COMMENT ON COLUMN file_metadata.deleted_by IS 'Username who deleted the file';
COMMENT ON COLUMN folders.deleted_at IS 'Timestamp when folder was moved to trash (soft delete)';
COMMENT ON COLUMN folders.deleted_by IS 'Username who deleted the folder';

COMMENT ON FUNCTION get_user_trash IS 'Retrieves all items in user trash bin';
COMMENT ON FUNCTION count_user_trash IS 'Returns count and total size of trash items';
COMMENT ON FUNCTION restore_file_from_trash IS 'Restores a file from trash';
COMMENT ON FUNCTION restore_folder_from_trash IS 'Restores a folder from trash';
COMMENT ON FUNCTION cleanup_old_trash IS 'Permanently deletes trash items older than specified days';
COMMENT ON FUNCTION empty_user_trash IS 'Empties entire trash bin for a user';
