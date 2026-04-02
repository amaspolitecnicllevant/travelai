-- Add missing 'revoked' column to refresh_tokens (required by RefreshToken entity)
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked BOOLEAN NOT NULL DEFAULT FALSE;
