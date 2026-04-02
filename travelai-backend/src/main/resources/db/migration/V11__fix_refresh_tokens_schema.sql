-- Rename 'token' column to 'token_hash' to match RefreshToken entity
ALTER TABLE refresh_tokens RENAME COLUMN token TO token_hash;

-- Drop old unique constraint (also removes its backing index)
ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_token_key;

-- Drop any remaining old index
DROP INDEX IF EXISTS idx_refresh_tokens_token;

-- Add new index for token_hash (entity expects idx_refresh_token_hash)
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_token_hash ON refresh_tokens(token_hash);
