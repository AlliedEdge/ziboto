-- =====================================================
-- ZIBOTO V3: STORAGE ANALYTICS
-- Migration: V20
-- Description: Usage charts and insights
-- Author: Ziboto Team
-- Created: 2026-08-11
-- =====================================================

-- Create storage_usage_history table for tracking over time
CREATE TABLE storage_usage_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_files BIGINT NOT NULL DEFAULT 0,
    total_size BIGINT NOT NULL DEFAULT 0,
    storage_used BIGINT NOT NULL DEFAULT 0,
    storage_quota BIGINT NOT NULL DEFAULT 0,
    usage_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT storage_usage_history_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_storage_usage_user_id ON storage_usage_history(user_id);
CREATE INDEX idx_storage_usage_recorded_at ON storage_usage_history(recorded_at DESC);
CREATE INDEX idx_storage_usage_user_recorded ON storage_usage_history(user_id, recorded_at DESC);

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Get storage usage by file type
CREATE OR REPLACE FUNCTION get_storage_by_file_type(p_user_id BIGINT)
RETURNS TABLE (
    file_extension VARCHAR,
    file_count BIGINT,
    total_size BIGINT,
    percentage NUMERIC(5,2)
) AS $$
DECLARE
    total_storage BIGINT;
BEGIN
    -- Get total storage
    SELECT COALESCE(SUM(file_size), 0) INTO total_storage
    FROM file_metadata
    WHERE user_id = p_user_id
      AND deleted_at IS NULL;
    
    -- Return breakdown by file type
    RETURN QUERY
    SELECT 
        COALESCE(fm.file_extension, 'No Extension') as file_extension,
        COUNT(*)::BIGINT as file_count,
        SUM(fm.file_size)::BIGINT as total_size,
        CASE 
            WHEN total_storage > 0 THEN (SUM(fm.file_size)::NUMERIC / total_storage * 100)::NUMERIC(5,2)
            ELSE 0::NUMERIC(5,2)
        END as percentage
    FROM file_metadata fm
    WHERE fm.user_id = p_user_id
      AND fm.deleted_at IS NULL
    GROUP BY fm.file_extension
    ORDER BY total_size DESC;
END;
$$ LANGUAGE plpgsql;

-- Get storage usage trend (last N days)
CREATE OR REPLACE FUNCTION get_storage_usage_trend(
    p_user_id BIGINT,
    p_days INT DEFAULT 30
)
RETURNS TABLE (
    date DATE,
    storage_used BIGINT,
    file_count BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        suh.recorded_at::DATE as date,
        suh.storage_used,
        suh.total_files as file_count
    FROM storage_usage_history suh
    WHERE suh.user_id = p_user_id
      AND suh.recorded_at >= CURRENT_DATE - p_days
    ORDER BY date ASC;
END;
$$ LANGUAGE plpgsql;

-- Get most accessed files
CREATE OR REPLACE FUNCTION get_most_accessed_files(
    p_user_id BIGINT,
    p_limit INT DEFAULT 10
)
RETURNS TABLE (
    file_id UUID,
    file_name VARCHAR,
    download_count INT,
    file_size BIGINT,
    last_downloaded TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        fm.id as file_id,
        fm.file_name,
        fm.download_count,
        fm.file_size,
        fm.updated_at as last_downloaded
    FROM file_metadata fm
    WHERE fm.user_id = p_user_id
      AND fm.deleted_at IS NULL
    ORDER BY fm.download_count DESC, fm.updated_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- Get upload/download activity by day
CREATE OR REPLACE FUNCTION get_activity_by_day(
    p_user_id BIGINT,
    p_days INT DEFAULT 30
)
RETURNS TABLE (
    date DATE,
    uploads BIGINT,
    downloads BIGINT
) AS $$
BEGIN
    RETURN QUERY
    WITH date_series AS (
        SELECT generate_series(
            CURRENT_DATE - p_days,
            CURRENT_DATE,
            '1 day'::interval
        )::DATE as date
    )
    SELECT 
        ds.date,
        COUNT(DISTINCT CASE WHEN al.activity_type = 'FILE_UPLOADED' THEN al.id END)::BIGINT as uploads,
        COUNT(DISTINCT CASE WHEN al.activity_type = 'FILE_DOWNLOADED' THEN al.id END)::BIGINT as downloads
    FROM date_series ds
    LEFT JOIN activity_logs al ON al.created_at::DATE = ds.date AND al.user_id = p_user_id
    GROUP BY ds.date
    ORDER BY ds.date ASC;
END;
$$ LANGUAGE plpgsql;

-- Record current storage usage snapshot
CREATE OR REPLACE FUNCTION record_storage_snapshot(p_user_id BIGINT)
RETURNS VOID AS $$
DECLARE
    v_total_files BIGINT;
    v_total_size BIGINT;
    v_storage_used BIGINT;
    v_storage_quota BIGINT;
    v_usage_percentage NUMERIC(5,2);
BEGIN
    -- Get file count and total size
    SELECT 
        COUNT(*),
        COALESCE(SUM(file_size), 0)
    INTO v_total_files, v_total_size
    FROM file_metadata
    WHERE user_id = p_user_id
      AND deleted_at IS NULL;
    
    -- Get user storage info
    SELECT storage_used, storage_quota
    INTO v_storage_used, v_storage_quota
    FROM users
    WHERE id = p_user_id;
    
    -- Calculate percentage
    IF v_storage_quota > 0 THEN
        v_usage_percentage := (v_storage_used::NUMERIC / v_storage_quota * 100)::NUMERIC(5,2);
    ELSE
        v_usage_percentage := 0;
    END IF;
    
    -- Insert snapshot
    INSERT INTO storage_usage_history (
        user_id,
        total_files,
        total_size,
        storage_used,
        storage_quota,
        usage_percentage,
        recorded_at
    ) VALUES (
        p_user_id,
        v_total_files,
        v_total_size,
        v_storage_used,
        v_storage_quota,
        v_usage_percentage,
        CURRENT_TIMESTAMP
    );
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- SCHEDULED SNAPSHOT (Trigger)
-- =====================================================

-- Function to record daily snapshots for all users
CREATE OR REPLACE FUNCTION record_all_user_snapshots()
RETURNS INT AS $$
DECLARE
    v_user_id BIGINT;
    v_count INT := 0;
BEGIN
    FOR v_user_id IN SELECT id FROM users LOOP
        PERFORM record_storage_snapshot(v_user_id);
        v_count := v_count + 1;
    END LOOP;
    
    RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE storage_usage_history IS 'Historical storage usage data for analytics';
COMMENT ON COLUMN storage_usage_history.user_id IS 'User whose storage is being tracked';
COMMENT ON COLUMN storage_usage_history.total_files IS 'Total number of files at this point';
COMMENT ON COLUMN storage_usage_history.total_size IS 'Total size of all files';
COMMENT ON COLUMN storage_usage_history.storage_used IS 'Storage used from user quota';
COMMENT ON COLUMN storage_usage_history.storage_quota IS 'User storage quota';
COMMENT ON COLUMN storage_usage_history.usage_percentage IS 'Percentage of quota used';

COMMENT ON FUNCTION get_storage_by_file_type IS 'Returns storage breakdown by file extension';
COMMENT ON FUNCTION get_storage_usage_trend IS 'Returns storage usage over time';
COMMENT ON FUNCTION get_most_accessed_files IS 'Returns most frequently downloaded files';
COMMENT ON FUNCTION get_activity_by_day IS 'Returns upload/download activity by day';
COMMENT ON FUNCTION record_storage_snapshot IS 'Records current storage usage snapshot';
