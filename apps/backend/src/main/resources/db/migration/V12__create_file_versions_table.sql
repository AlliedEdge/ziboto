-- V12: Create File Versioning Tables
-- Purpose: Track file history, enable version rollback, compare versions
-- Author: Ziboto Team
-- Date: August 11, 2026

-- ============================================================================
-- TABLE: file_versions
-- Purpose: Store all versions of files
-- ============================================================================

CREATE TABLE file_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    
    -- File metadata (snapshot)
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(255),
    sha256_hash VARCHAR(64) NOT NULL,
    
    -- Storage info
    storage_key VARCHAR(1024) NOT NULL,
    storage_location VARCHAR(100) DEFAULT 'S3',
    
    -- Version metadata
    change_description TEXT,
    version_tag VARCHAR(100), -- e.g., "v1.0", "final", "draft"
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100) NOT NULL,
    
    -- Foreign keys
    FOREIGN KEY (file_id) REFERENCES file_metadata(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES for Performance
-- ============================================================================

-- Primary query index (get versions for a file)
CREATE INDEX idx_file_versions_file_id ON file_versions(file_id);

-- Query by user
CREATE INDEX idx_file_versions_user_id ON file_versions(user_id);

-- Latest version queries
CREATE INDEX idx_file_versions_created_at ON file_versions(created_at DESC);

-- Unique version number per file
CREATE UNIQUE INDEX idx_file_versions_file_version ON file_versions(file_id, version_number);

-- SHA hash lookup for deduplication
CREATE INDEX idx_file_versions_sha256 ON file_versions(sha256_hash);

-- ============================================================================
-- FUNCTION: Auto-increment version number
-- ============================================================================

CREATE OR REPLACE FUNCTION get_next_version_number(p_file_id UUID)
RETURNS INT AS $$
DECLARE
    next_version INT;
BEGIN
    SELECT COALESCE(MAX(version_number), 0) + 1
    INTO next_version
    FROM file_versions
    WHERE file_id = p_file_id;
    
    RETURN next_version;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TRIGGER: Create initial version when file is uploaded
-- ============================================================================

CREATE OR REPLACE FUNCTION create_initial_file_version()
RETURNS TRIGGER AS $$
BEGIN
    -- Create version 1 automatically for new files
    INSERT INTO file_versions (
        file_id,
        user_id,
        version_number,
        file_name,
        file_size,
        mime_type,
        sha256_hash,
        storage_key,
        storage_location,
        change_description,
        created_by
    ) VALUES (
        NEW.id,
        NEW.user_id,
        1,
        NEW.file_name,
        NEW.file_size,
        NEW.mime_type,
        NEW.sha256_hash,
        NEW.storage_key,
        'S3',
        'Initial version',
        NEW.created_by
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_create_initial_version
AFTER INSERT ON file_metadata
FOR EACH ROW
EXECUTE FUNCTION create_initial_file_version();

-- ============================================================================
-- TABLE: version_retention_policies
-- Purpose: Define how long to keep old versions (optional, for future)
-- ============================================================================

CREATE TABLE version_retention_policies (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- Retention settings
    max_versions_per_file INT DEFAULT 10, -- Keep last N versions
    max_days_to_keep INT DEFAULT 90, -- Keep versions for N days
    auto_delete_old_versions BOOLEAN DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_retention_policies_user_id ON version_retention_policies(user_id);

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE file_versions IS 'Stores all versions of files for version history and rollback';
COMMENT ON COLUMN file_versions.version_number IS 'Sequential version number starting from 1';
COMMENT ON COLUMN file_versions.version_tag IS 'Optional human-readable tag like v1.0, final, draft';
COMMENT ON COLUMN file_versions.change_description IS 'User description of what changed in this version';
COMMENT ON COLUMN file_versions.sha256_hash IS 'File content hash for deduplication across versions';

COMMENT ON TABLE version_retention_policies IS 'Defines version retention rules per user (optional, for future use)';

-- ============================================================================
-- Sample Queries for Testing
-- ============================================================================

-- Get all versions for a file
-- SELECT * FROM file_versions WHERE file_id = ? ORDER BY version_number DESC;

-- Get latest version
-- SELECT * FROM file_versions WHERE file_id = ? ORDER BY version_number DESC LIMIT 1;

-- Get version by number
-- SELECT * FROM file_versions WHERE file_id = ? AND version_number = ?;

-- Count versions for a file
-- SELECT COUNT(*) FROM file_versions WHERE file_id = ?;

-- Find duplicate content across versions (deduplication)
-- SELECT sha256_hash, COUNT(*) FROM file_versions GROUP BY sha256_hash HAVING COUNT(*) > 1;
