-- V16: OAuth Integration (Google OAuth)
-- Purpose: Social authentication with Google
-- Author: Ziboto Team
-- Date: August 11, 2026

-- ============================================================================
-- ENUM: OAuth Provider
-- ============================================================================

CREATE TYPE oauth_provider AS ENUM (
    'GOOGLE',
    'GITHUB',
    'MICROSOFT'
);

-- ============================================================================
-- TABLE: oauth_accounts
-- Purpose: Link user accounts with OAuth providers
-- ============================================================================

CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- User reference
    user_id BIGINT NOT NULL,
    
    -- OAuth provider
    provider oauth_provider NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL, -- ID from OAuth provider
    
    -- OAuth details
    email VARCHAR(255),
    name VARCHAR(255),
    picture_url VARCHAR(500),
    
    -- Tokens (encrypted)
    access_token TEXT,
    refresh_token TEXT,
    id_token TEXT,
    token_expires_at TIMESTAMP,
    
    -- Metadata
    provider_data JSONB, -- Additional data from provider
    
    -- Account linking
    linked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (provider, provider_user_id)
);

-- ============================================================================
-- INDEXES for Performance
-- ============================================================================

CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_oauth_accounts_provider ON oauth_accounts(provider);
CREATE INDEX idx_oauth_accounts_provider_user_id ON oauth_accounts(provider_user_id);
CREATE INDEX idx_oauth_accounts_email ON oauth_accounts(email);
CREATE UNIQUE INDEX idx_oauth_accounts_user_provider ON oauth_accounts(user_id, provider);

-- ============================================================================
-- TABLE: oauth_authorization_codes
-- Purpose: Store OAuth authorization codes (temporary)
-- ============================================================================

CREATE TABLE oauth_authorization_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    code VARCHAR(500) NOT NULL UNIQUE,
    provider oauth_provider NOT NULL,
    
    -- State for CSRF protection
    state VARCHAR(255) NOT NULL,
    
    -- Redirect URI
    redirect_uri VARCHAR(500),
    
    -- User ID (if already authenticated)
    user_id BIGINT,
    
    -- Expiration
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_oauth_codes_code ON oauth_authorization_codes(code);
CREATE INDEX idx_oauth_codes_state ON oauth_authorization_codes(state);
CREATE INDEX idx_oauth_codes_expires_at ON oauth_authorization_codes(expires_at);

-- ============================================================================
-- FUNCTION: Find user by OAuth provider ID
-- ============================================================================

CREATE OR REPLACE FUNCTION find_user_by_oauth(
    p_provider oauth_provider,
    p_provider_user_id VARCHAR
)
RETURNS BIGINT AS $$
DECLARE
    v_user_id BIGINT;
BEGIN
    SELECT user_id INTO v_user_id
    FROM oauth_accounts
    WHERE provider = p_provider
    AND provider_user_id = p_provider_user_id
    AND is_active = TRUE;
    
    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Check if email is already registered
-- ============================================================================

CREATE OR REPLACE FUNCTION is_oauth_email_registered(p_email VARCHAR)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM users WHERE email = p_email
        UNION
        SELECT 1 FROM oauth_accounts WHERE email = p_email
    );
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Link OAuth account to existing user
-- ============================================================================

CREATE OR REPLACE FUNCTION link_oauth_account(
    p_user_id BIGINT,
    p_provider oauth_provider,
    p_provider_user_id VARCHAR,
    p_email VARCHAR,
    p_name VARCHAR,
    p_picture_url VARCHAR,
    p_access_token TEXT,
    p_refresh_token TEXT,
    p_token_expires_at TIMESTAMP
)
RETURNS UUID AS $$
DECLARE
    v_account_id UUID;
BEGIN
    -- Check if already linked
    SELECT id INTO v_account_id
    FROM oauth_accounts
    WHERE user_id = p_user_id
    AND provider = p_provider;
    
    IF v_account_id IS NOT NULL THEN
        -- Update existing link
        UPDATE oauth_accounts
        SET provider_user_id = p_provider_user_id,
            email = p_email,
            name = p_name,
            picture_url = p_picture_url,
            access_token = p_access_token,
            refresh_token = p_refresh_token,
            token_expires_at = p_token_expires_at,
            last_used_at = NOW(),
            updated_at = NOW()
        WHERE id = v_account_id;
    ELSE
        -- Create new link
        INSERT INTO oauth_accounts (
            user_id,
            provider,
            provider_user_id,
            email,
            name,
            picture_url,
            access_token,
            refresh_token,
            token_expires_at,
            last_used_at
        ) VALUES (
            p_user_id,
            p_provider,
            p_provider_user_id,
            p_email,
            p_name,
            p_picture_url,
            p_access_token,
            p_refresh_token,
            p_token_expires_at,
            NOW()
        )
        RETURNING id INTO v_account_id;
    END IF;
    
    RETURN v_account_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Update OAuth token
-- ============================================================================

CREATE OR REPLACE FUNCTION update_oauth_token(
    p_user_id BIGINT,
    p_provider oauth_provider,
    p_access_token TEXT,
    p_refresh_token TEXT,
    p_token_expires_at TIMESTAMP
)
RETURNS BOOLEAN AS $$
DECLARE
    rows_affected INT;
BEGIN
    UPDATE oauth_accounts
    SET access_token = p_access_token,
        refresh_token = p_refresh_token,
        token_expires_at = p_token_expires_at,
        updated_at = NOW()
    WHERE user_id = p_user_id
    AND provider = p_provider;
    
    GET DIAGNOSTICS rows_affected = ROW_COUNT;
    RETURN rows_affected > 0;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- FUNCTION: Delete expired authorization codes
-- ============================================================================

CREATE OR REPLACE FUNCTION delete_expired_oauth_codes()
RETURNS INT AS $$
DECLARE
    rows_deleted INT;
BEGIN
    DELETE FROM oauth_authorization_codes
    WHERE expires_at < NOW() OR used = TRUE;
    
    GET DIAGNOSTICS rows_deleted = ROW_COUNT;
    RETURN rows_deleted;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TRIGGER: Update timestamps
-- ============================================================================

CREATE OR REPLACE FUNCTION update_oauth_accounts_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_oauth_accounts_updated_at
BEFORE UPDATE ON oauth_accounts
FOR EACH ROW
EXECUTE FUNCTION update_oauth_accounts_timestamp();

-- ============================================================================
-- COMMENTS for Documentation
-- ============================================================================

COMMENT ON TABLE oauth_accounts IS 'OAuth provider accounts linked to users';
COMMENT ON TABLE oauth_authorization_codes IS 'Temporary OAuth authorization codes';

COMMENT ON COLUMN oauth_accounts.provider_user_id IS 'User ID from OAuth provider (e.g., Google user ID)';
COMMENT ON COLUMN oauth_accounts.access_token IS 'OAuth access token (should be encrypted at rest)';
COMMENT ON COLUMN oauth_accounts.provider_data IS 'Additional data from OAuth provider in JSON format';
COMMENT ON COLUMN oauth_authorization_codes.state IS 'CSRF protection state parameter';

-- ============================================================================
-- Sample Queries
-- ============================================================================

-- Find user by OAuth provider
-- SELECT find_user_by_oauth('GOOGLE', 'google-user-id-123');

-- Check if email is registered
-- SELECT is_oauth_email_registered('user@example.com');

-- Link OAuth account
-- SELECT link_oauth_account(1, 'GOOGLE', 'google-id', 'user@gmail.com', 'John Doe', 'https://...', 'token', 'refresh', NOW() + INTERVAL '1 hour');

-- Delete expired codes (run as scheduled job)
-- SELECT delete_expired_oauth_codes();
