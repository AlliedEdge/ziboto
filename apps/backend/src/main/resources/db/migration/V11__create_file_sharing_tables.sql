-- V11: File Sharing & Permissions Tables
-- Created: August 11, 2026
-- Description: Tables for file sharing, permissions, and share links

-- ============================================================================
-- File Shares Table
-- ============================================================================
-- Purpose: Track files shared directly with specific users
-- Features: Permission levels, expiration, acceptance workflow
-- ============================================================================

CREATE TABLE file_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL,
    owner_id BIGINT NOT NULL,
    shared_with_user_id BIGINT NOT NULL,
    permission VARCHAR(20) NOT NULL CHECK (permission IN ('VIEW', 'EDIT', 'DOWNLOAD', 'FULL')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED')),
    message TEXT,
    expires_at TIMESTAMP,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT fk_file_shares_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_shares_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_shares_user FOREIGN KEY (shared_with_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_file_shares_different_users CHECK (owner_id != shared_with_user_id),
    CONSTRAINT chk_file_shares_expiration CHECK (expires_at IS NULL OR expires_at > created_at)
);

-- Indexes for file_shares
CREATE INDEX idx_file_shares_file_id ON file_shares(file_id);
CREATE INDEX idx_file_shares_owner_id ON file_shares(owner_id);
CREATE INDEX idx_file_shares_shared_with ON file_shares(shared_with_user_id);
CREATE INDEX idx_file_shares_status ON file_shares(status);
CREATE INDEX idx_file_shares_expires_at ON file_shares(expires_at) WHERE expires_at IS NOT NULL;
CREATE UNIQUE INDEX idx_file_shares_unique ON file_shares(file_id, shared_with_user_id) WHERE status != 'DECLINED';

-- ============================================================================
-- Folder Shares Table
-- ============================================================================
-- Purpose: Track folders shared with users (grants access to all files inside)
-- Features: Cascading permissions, expiration
-- ============================================================================

CREATE TABLE folder_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folder_id UUID NOT NULL,
    owner_id BIGINT NOT NULL,
    shared_with_user_id BIGINT NOT NULL,
    permission VARCHAR(20) NOT NULL CHECK (permission IN ('VIEW', 'EDIT', 'DOWNLOAD', 'FULL')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED')),
    message TEXT,
    expires_at TIMESTAMP,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT fk_folder_shares_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_shares_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_shares_user FOREIGN KEY (shared_with_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_folder_shares_different_users CHECK (owner_id != shared_with_user_id),
    CONSTRAINT chk_folder_shares_expiration CHECK (expires_at IS NULL OR expires_at > created_at)
);

-- Indexes for folder_shares
CREATE INDEX idx_folder_shares_folder_id ON folder_shares(folder_id);
CREATE INDEX idx_folder_shares_owner_id ON folder_shares(owner_id);
CREATE INDEX idx_folder_shares_shared_with ON folder_shares(shared_with_user_id);
CREATE INDEX idx_folder_shares_status ON folder_shares(status);
CREATE INDEX idx_folder_shares_expires_at ON folder_shares(expires_at) WHERE expires_at IS NOT NULL;
CREATE UNIQUE INDEX idx_folder_shares_unique ON folder_shares(folder_id, shared_with_user_id) WHERE status != 'DECLINED';

-- ============================================================================
-- Share Links Table
-- ============================================================================
-- Purpose: Public/anonymous file sharing via generated links
-- Features: Token-based, download tracking, expiration, password protection
-- ============================================================================

CREATE TABLE share_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL,
    owner_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    permission VARCHAR(20) NOT NULL DEFAULT 'VIEW' CHECK (permission IN ('VIEW', 'DOWNLOAD')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    password_hash VARCHAR(255),
    expires_at TIMESTAMP,
    max_downloads INTEGER,
    download_count INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP,
    last_accessed_ip VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT fk_share_links_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_share_links_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_share_links_max_downloads CHECK (max_downloads IS NULL OR max_downloads > 0),
    CONSTRAINT chk_share_links_download_count CHECK (download_count >= 0),
    CONSTRAINT chk_share_links_expiration CHECK (expires_at IS NULL OR expires_at > created_at)
);

-- Indexes for share_links
CREATE INDEX idx_share_links_file_id ON share_links(file_id);
CREATE INDEX idx_share_links_owner_id ON share_links(owner_id);
CREATE INDEX idx_share_links_token ON share_links(token);
CREATE INDEX idx_share_links_status ON share_links(status);
CREATE INDEX idx_share_links_expires_at ON share_links(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_share_links_created_at ON share_links(created_at DESC);

-- ============================================================================
-- Share Activities Table
-- ============================================================================
-- Purpose: Audit trail for share access and activities
-- Features: Track who accessed shared files, when, from where
-- ============================================================================

CREATE TABLE share_activities (
    id BIGSERIAL PRIMARY KEY,
    share_type VARCHAR(20) NOT NULL CHECK (share_type IN ('FILE_SHARE', 'FOLDER_SHARE', 'SHARE_LINK')),
    share_id UUID NOT NULL,
    file_id UUID NOT NULL,
    user_id BIGINT,
    activity_type VARCHAR(30) NOT NULL CHECK (activity_type IN ('VIEWED', 'DOWNLOADED', 'ACCESSED', 'SHARED', 'REVOKED', 'EXPIRED')),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for share_activities
CREATE INDEX idx_share_activities_share ON share_activities(share_type, share_id);
CREATE INDEX idx_share_activities_file_id ON share_activities(file_id);
CREATE INDEX idx_share_activities_user_id ON share_activities(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_share_activities_created_at ON share_activities(created_at DESC);
CREATE INDEX idx_share_activities_type ON share_activities(activity_type);

-- ============================================================================
-- Triggers for updated_at
-- ============================================================================

-- Trigger for file_shares
CREATE OR REPLACE FUNCTION update_file_shares_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_file_shares_updated_at
    BEFORE UPDATE ON file_shares
    FOR EACH ROW
    EXECUTE FUNCTION update_file_shares_updated_at();

-- Trigger for folder_shares
CREATE OR REPLACE FUNCTION update_folder_shares_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_folder_shares_updated_at
    BEFORE UPDATE ON folder_shares
    FOR EACH ROW
    EXECUTE FUNCTION update_folder_shares_updated_at();

-- Trigger for share_links
CREATE OR REPLACE FUNCTION update_share_links_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_share_links_updated_at
    BEFORE UPDATE ON share_links
    FOR EACH ROW
    EXECUTE FUNCTION update_share_links_updated_at();

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON TABLE file_shares IS 'Direct file sharing between users with permission levels';
COMMENT ON TABLE folder_shares IS 'Folder sharing between users (grants access to all files inside)';
COMMENT ON TABLE share_links IS 'Public/anonymous file sharing via generated tokens';
COMMENT ON TABLE share_activities IS 'Audit trail for all share-related activities';

COMMENT ON COLUMN file_shares.permission IS 'Permission level: VIEW (metadata only), EDIT (modify file), DOWNLOAD (download file), FULL (all permissions)';
COMMENT ON COLUMN file_shares.status IS 'Share status: PENDING (awaiting acceptance), ACCEPTED (active), DECLINED (rejected by recipient), REVOKED (cancelled by owner)';
COMMENT ON COLUMN share_links.token IS 'Unique token for accessing shared file (URL-safe random string)';
COMMENT ON COLUMN share_links.password_hash IS 'BCrypt hash if share link is password-protected';
COMMENT ON COLUMN share_links.max_downloads IS 'Maximum number of downloads allowed (NULL = unlimited)';
