-- =====================================================
-- ZIBOTO V3: FILE PREVIEWS
-- Migration: V22
-- Description: File preview generation and caching
-- Author: Ziboto Team
-- Created: 2026-08-12
-- =====================================================

-- Add preview-related columns to file_metadata if not exists
ALTER TABLE file_metadata 
ADD COLUMN IF NOT EXISTS preview_status VARCHAR(20) DEFAULT 'PENDING',
ADD COLUMN IF NOT EXISTS preview_error TEXT,
ADD COLUMN IF NOT EXISTS preview_generated_at TIMESTAMP;

-- Create file_previews table for caching
CREATE TABLE IF NOT EXISTS file_previews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES file_metadata(id) ON DELETE CASCADE,
    preview_type VARCHAR(50) NOT NULL, -- THUMBNAIL, IMAGE, PDF, VIDEO, AUDIO, DOCUMENT, CODE
    preview_data BYTEA, -- Small previews stored inline
    preview_url VARCHAR(1000), -- Large previews stored in S3
    width INT,
    height INT,
    duration INT, -- For video/audio in seconds
    page_count INT, -- For documents
    file_size BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    
    CONSTRAINT file_previews_file_id_fk FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE CASCADE,
    CONSTRAINT unique_file_preview_type UNIQUE (file_id, preview_type)
);

-- Create indexes
CREATE INDEX idx_file_previews_file_id ON file_previews(file_id);
CREATE INDEX idx_file_previews_type ON file_previews(preview_type);
CREATE INDEX idx_file_previews_expires_at ON file_previews(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_file_metadata_preview_status ON file_metadata(preview_status);

-- =====================================================
-- FUNCTIONS
-- =====================================================

-- Get preview by file and type
CREATE OR REPLACE FUNCTION get_file_preview(
    p_file_id UUID,
    p_preview_type VARCHAR
)
RETURNS TABLE (
    id UUID,
    preview_type VARCHAR,
    preview_url VARCHAR,
    width INT,
    height INT,
    duration INT,
    page_count INT,
    file_size BIGINT,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        fp.id,
        fp.preview_type,
        fp.preview_url,
        fp.width,
        fp.height,
        fp.duration,
        fp.page_count,
        fp.file_size,
        fp.created_at
    FROM file_previews fp
    WHERE fp.file_id = p_file_id
      AND fp.preview_type = p_preview_type
      AND (fp.expires_at IS NULL OR fp.expires_at > CURRENT_TIMESTAMP);
END;
$$ LANGUAGE plpgsql;

-- Cleanup expired previews
CREATE OR REPLACE FUNCTION cleanup_expired_previews()
RETURNS INT AS $$
DECLARE
    v_deleted_count INT;
BEGIN
    DELETE FROM file_previews
    WHERE expires_at IS NOT NULL
      AND expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Get preview statistics
CREATE OR REPLACE FUNCTION get_preview_stats()
RETURNS TABLE (
    preview_type VARCHAR,
    count BIGINT,
    total_size BIGINT,
    avg_size BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        fp.preview_type,
        COUNT(*)::BIGINT as count,
        COALESCE(SUM(fp.file_size), 0)::BIGINT as total_size,
        COALESCE(AVG(fp.file_size), 0)::BIGINT as avg_size
    FROM file_previews fp
    WHERE fp.expires_at IS NULL OR fp.expires_at > CURRENT_TIMESTAMP
    GROUP BY fp.preview_type
    ORDER BY count DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE file_previews IS 'Cached file previews for quick access';
COMMENT ON COLUMN file_previews.preview_type IS 'Type of preview: THUMBNAIL, IMAGE, PDF, VIDEO, AUDIO, DOCUMENT, CODE';
COMMENT ON COLUMN file_previews.preview_data IS 'Small preview data stored inline (Base64 or binary)';
COMMENT ON COLUMN file_previews.preview_url IS 'URL for large previews stored in S3';
COMMENT ON COLUMN file_previews.duration IS 'Duration in seconds for video/audio files';
COMMENT ON COLUMN file_previews.page_count IS 'Number of pages for document files';
COMMENT ON COLUMN file_previews.expires_at IS 'Expiration timestamp for temporary previews';

COMMENT ON COLUMN file_metadata.preview_status IS 'Preview generation status: PENDING, PROCESSING, COMPLETED, FAILED, NOT_SUPPORTED';
COMMENT ON COLUMN file_metadata.preview_error IS 'Error message if preview generation failed';
COMMENT ON COLUMN file_metadata.preview_generated_at IS 'Timestamp when preview was generated';

COMMENT ON FUNCTION get_file_preview IS 'Retrieves preview by file ID and type';
COMMENT ON FUNCTION cleanup_expired_previews IS 'Removes expired preview entries';
COMMENT ON FUNCTION get_preview_stats IS 'Gets statistics about cached previews';
