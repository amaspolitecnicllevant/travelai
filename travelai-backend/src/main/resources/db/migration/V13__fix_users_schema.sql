-- Fix users table to match User entity
-- Rename 'password' → 'password_hash'
ALTER TABLE users RENAME COLUMN password TO password_hash;

-- Add missing columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS birth_date DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS consent_version VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS consent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
