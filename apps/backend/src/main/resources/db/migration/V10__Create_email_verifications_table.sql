-- V10__Create_email_verifications_table.sql
-- Stores OTP tokens for both email verification (signup) and password reset flows.
-- The OTP value itself is managed by Redis (OtpCacheService); this table only
-- tracks the lifecycle state so the API can give the correct response even after
-- the Redis TTL has expired.

CREATE TABLE email_verifications (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         BIGINT      NOT NULL,
    email           VARCHAR(100) NOT NULL,
    type            VARCHAR(30)  NOT NULL,   -- EMAIL_VERIFICATION | PASSWORD_RESET
    used            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP    NOT NULL,
    used_at         TIMESTAMP,

    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Fast lookup by user + type (e.g. "does this user have a pending verification?")
CREATE INDEX idx_ev_user_type      ON email_verifications(user_id, type);

-- Fast lookup by email + type (used during OTP submission before user is loaded)
CREATE INDEX idx_ev_email_type     ON email_verifications(email, type);

-- Helps scheduled cleanup jobs find expired rows efficiently
CREATE INDEX idx_ev_expires_at     ON email_verifications(expires_at);

COMMENT ON TABLE  email_verifications                IS 'Tracks email-verification and password-reset OTP lifecycle';
COMMENT ON COLUMN email_verifications.type           IS 'EMAIL_VERIFICATION or PASSWORD_RESET';
COMMENT ON COLUMN email_verifications.used           IS 'TRUE once the OTP has been successfully consumed';
COMMENT ON COLUMN email_verifications.expires_at     IS 'Hard expiry — must match the OTP TTL configured in Redis (default 5 min)';
COMMENT ON COLUMN email_verifications.used_at        IS 'Timestamp at which the OTP was consumed';
