-- V11__Make_email_verifications_user_id_nullable.sql
-- Allow user_id to be NULL so that an EmailVerification audit row can be
-- created for a pending (pre-verification) registration before the User row
-- exists.  The User row is only inserted once the OTP is successfully verified.
ALTER TABLE email_verifications
    ALTER COLUMN user_id DROP NOT NULL;
