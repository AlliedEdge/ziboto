-- V14: Duplicate Detection System
-- Purpose: Detect and manage duplicate files by content hash
-- Author: Ziboto Team
-- Date: August 11, 2026

-- ============================================================================
-- TABLE: duplicate_groups
-- Purpose: Group files with identical content
-- ============================================================================

CREATE TABLE duplicate_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Hash identifier (SHA-256)
    content_hash VARCHAR(64) NOT NULL UNIQUE,
    
    -- Group metadata
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(255),
    duplicate_count INT NOT NULL DEFAULT 0,
    
    -- First file info (the "original")
    first_file_id UUID NOT NULL,
    first_file_name VARCHAR(255),
    first_uploaded_at TIMESTAMP NOT NULL,
    
    -- Storage savings
    potential_savings_bytes BIGINT DEFAULT 0,
    
    -- Status
    reviewed BOOLEAN DEFAULT FALSE,
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(100),
    
    -- Action taken
    action_taken VARCHAR(50), -- 'KEEP_ALL', 'DELETE_DUPLICATES', 'KEEP_ORIGINAL'
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES for Performance
-- ============================================================================

CREATE INDEX idx_duplicate_groups_hash ON duplicate_groups(content_hash);
CREATE INDEX idx_duplicate_groups_size ON duplicate_groups(file_size);
CREATE INDEX idx_duplicate_groups_reviewed ON duplicate_groups(reviewed);
CREATE INDEX idx_duplicate_groups_created_at ON duplicate_groups(created_at DESC);

-- ============================================================================
-- TABLE: duplicate_files
-- Purpose: Track individual files in duplicate groups
-- ============================================================================

CREATE TABLE duplicate_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Reference to duplicate group
    group_id UUID NOT NULL,
    
    -- File reference
    file_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    
    -- File details
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT,
    uploaded_at TIMESTAMP NOT NULL,
    
    -- Duplicate status
    is_original BOOLEAN DEFAULT FALSE, -- First file uploaded
    marked_for_deletion BOOLEAN DEFAULT FALSE,
    keep_reason TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    FOREIGN KEY (group_id) REFERENCES duplicate_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_duplicate_files_group ON duplicate_files(group_id);
CREATE INDEX idx_duplicate_files_file ON duplicate_files(file_id);
CREATE INDEX idx_duplicate_files_user ON duplicate_files(user_id);
CREATE INDEX idx_duplicate_files_marked ON duplicate_files(marked_for_deletion);

-- ============================================================================
-- VIEW: duplicate_summary
-- Purpose: Aggregate statistics on duplicates
-- ============================================================================

CREATE VIEW duplicate_summary AS
SELECT 
    dg.id as group_id,
    dg.content_hash,
    dg.file_size,
    dg.mime_type,
    dg.duplicate_count,
    dg.potential_savings_bytes,
    dg.reviewed,
    COUNT(df.id) as total_files,
    MIN(df.uploaded_at) as first_uploaded,
    MAX(df.uploaded_at) as last_uploaded,
    COUNT(DISTINCT df.user_id) as affected_users
FROM duplicate_groups dg
LEFT JOIN duplicate_files df ON dg.id = df.group_id
GROUP BY dg.id, dg.content_hash, dg.file_size, dg.mime_type, 
         dg.duplicate_count, dg.potential_savings_bytes, dg.reviewed;

-- ============================================================================
-- FUNCTION: Detect duplicates for a specific hash
-- ============================================================================

CREATE OR REPLACE FUNCTION detect_duplicates_by_hash(p_content_hash VARCHAR)
RETURNS UUID AS $$
DECLARE
    v_group_id UUID;
    v_file_count INT;
    v_first_file_id UUID;
    v_first_file_name VARCHAR(255);
    v_first_uploaded_at TIMESTAMP;
    v_file_size BIGINT;
    v_mime_type VARCHAR(255);
BEGIN
    -- Count files with this hash
    SELECT COUNT(*), MIN(id), MIN(file_name), MIN(created_at), MIN(file_size), MIN(mime_type)
    INTO v_file_count, v_first_file_id, v_first_file_name, v_first_uploaded_at, v_file_size, v_mime_type
    FROM file_metadata
    WHERE sha256_hash = p_content_hash
    AND deleted_at IS NULL;
    
    -- Only create group if there are duplicates
    IF v_file_count > 1 THEN
        -- Check if group exists
        SELECT id INTO v_group_id
        FROM duplicate_groups
        WHERE content_hash = p_content_hash;
        
        -- Create group if doesn't exist
        IF v_group_id IS NULL THEN
            INSERT INTO duplicate_groups (
                content_hash,
                file_size,
                mime_type,
                duplicate_count,
                first_file_id,
                first_file_name,
                first_uploaded_at,
                potential_savings_bytes
            ) VALUES (
                p_content_hash,
                v_file_size,
                v_mime_type,
                v_file_count - 1,
                v_first_file_id,
                v_first_file_name,
                v_first_uploaded_at,
                v_file_size * (v_file_count - 1)
            )
            RETURNING id INTO v_group_id;
        ELSE
            -- Update existing group
            UPDATE duplicate_groups
            SET duplicate_count = v_file_count - 1,
                potential_savings_bytes = v_file_size * (v_file_count - 1),
                updated_at = NOW()
            WHERE id = v_group_id;
        END IF;
        
        -- Delete existing entries for this group
        DELETE FROM duplicate_files WHERE group_id = v_group_id;
        
        -- Insert all files into duplicate_files
        INSERT INTO duplicate_files (
            group_id,
            file_id,
            user_id,
            file_name,
            file_path,
            uploaded_at,
            is_original
        )
        SELECT 
            v_group_id,
            fm.id,
            fm.user_id,
            fm.file_name,
            f.folder_path,
            fm.created_at,
            (fm.id = v_first_file_id) as is_original
        FROM file_metadata fm
        LEFT JOIN folders f ON fm.folder_id = f.id
        WHERE fm.sha256_hash = p_content_hash
        AND fm.deleted_at IS NULL;
        
        RETURN v_group_id;
    ELSE
        -- Remove group if it exists (file was deleted)
        DELETE FROM duplicate_groups WHERE content_hash = p_content_hash;
        RETURN NULL;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Scan all files for duplicates
-- ============================================================================

CREATE OR REPLACE FUNCTION scan_all_duplicates()
RETURNS TABLE(group_id UUID, duplicate_count INT) AS $$
BEGIN
    RETURN QUERY
    WITH duplicate_hashes AS (
        SELECT sha256_hash, COUNT(*) as cnt
        FROM file_metadata
        WHERE deleted_at IS NULL
        GROUP BY sha256_hash
        HAVING COUNT(*) > 1
    )
    SELECT 
        detect_duplicates_by_hash(dh.sha256_hash) as group_id,
        dh.cnt::INT as duplicate_count
    FROM duplicate_hashes dh;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Get duplicate statistics for a user
-- ============================================================================

CREATE OR REPLACE FUNCTION get_user_duplicate_stats(p_user_id BIGINT)
RETURNS TABLE(
    total_duplicates INT,
    total_savings_bytes BIGINT,
    duplicate_groups INT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(DISTINCT df.file_id)::INT as total_duplicates,
        SUM(dg.file_size)::BIGINT as total_savings_bytes,
        COUNT(DISTINCT dg.id)::INT as duplicate_groups
    FROM duplicate_files df
    JOIN duplicate_groups dg ON df.group_id = dg.id
    WHERE df.user_id = p_user_id
    AND df.is_original = FALSE;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TRIGGER: Auto-detect duplicates on file upload
-- ============================================================================

CREATE OR REPLACE FUNCTION trigger_detect_duplicates()
RETURNS TRIGGER AS $$
BEGIN
    -- Detect duplicates for this file's hash
    PERFORM detect_duplicates_by_hash(NEW.sha256_hash);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_file_upload_duplicate_check
AFTER INSERT ON file_metadata
FOR EACH ROW
EXECUTE FUNCTION trigger_detect_duplicates();

-- ============================================================================
-- TRIGGER: Update duplicate groups on file deletion
-- ============================================================================

CREATE OR REPLACE FUNCTION trigger_update_duplicates_on_delete()
RETURNS TRIGGER AS $$
BEGIN
    -- Re-detect duplicates for this hash (will remove group if needed)
    PERFORM detect_duplicates_by_hash(OLD.sha256_hash);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_file_delete_duplicate_update
AFTER DELETE ON file_metadata
FOR EACH ROW
EXECUTE FUNCTION trigger_update_duplicates_on_delete();

-- ============================================================================
-- TRIGGER: Update timestamps
-- ============================================================================

CREATE OR REPLACE FUNCTION update_duplicate_groups_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_duplicate_groups_updated_at
BEFORE UPDATE ON duplicate_groups
FOR EACH ROW
EXECUTE FUNCTION update_duplicate_groups_timestamp();

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE duplicate_groups IS 'Groups of files with identical content (by SHA-256 hash)';
COMMENT ON TABLE duplicate_files IS 'Individual files belonging to duplicate groups';
COMMENT ON VIEW duplicate_summary IS 'Aggregate statistics on duplicate file groups';

COMMENT ON COLUMN duplicate_groups.content_hash IS 'SHA-256 hash of file content';
COMMENT ON COLUMN duplicate_groups.potential_savings_bytes IS 'Storage that could be saved by deleting duplicates';
COMMENT ON COLUMN duplicate_files.is_original IS 'TRUE if this is the first file uploaded (chronologically)';
COMMENT ON COLUMN duplicate_files.marked_for_deletion IS 'TRUE if user marked this file for deletion';

-- ============================================================================
-- Sample Queries
-- ============================================================================

-- Find all duplicate groups
-- SELECT * FROM duplicate_summary ORDER BY potential_savings_bytes DESC;

-- Find duplicates for a specific user
-- SELECT * FROM duplicate_files WHERE user_id = 1;

-- Get user's duplicate statistics
-- SELECT * FROM get_user_duplicate_stats(1);

-- Scan for duplicates
-- SELECT * FROM scan_all_duplicates();

-- Mark duplicates for deletion (keep original)
-- UPDATE duplicate_files 
-- SET marked_for_deletion = TRUE 
-- WHERE group_id = ? AND is_original = FALSE;
