-- Add Google OAuth fields to users table
-- This allows users to sign in with Google and link their Google account

ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50);

-- Create index for fast Google ID lookups
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);
CREATE INDEX IF NOT EXISTS idx_users_oauth_provider ON users(oauth_provider);

-- Add unique constraint to prevent duplicate Google accounts
ALTER TABLE users ADD CONSTRAINT IF NOT EXISTS uk_users_google_id UNIQUE (google_id);

-- Update password column to be nullable (Google users may not have password)
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

COMMENT ON COLUMN users.google_id IS 'Google OAuth subject identifier (sub claim)';
COMMENT ON COLUMN users.oauth_provider IS 'OAuth provider name (e.g., google, github)';
