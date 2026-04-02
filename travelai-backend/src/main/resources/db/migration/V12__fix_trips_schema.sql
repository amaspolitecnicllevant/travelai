-- Fix trips table to match Trip entity:
-- 1. Rename user_id → owner_id (entity uses @JoinColumn name="owner_id")
-- 2. Add description column
-- 3. Add status column
-- 4. Add deleted_at column

-- Rename user_id to owner_id (drop FK first, recreate after rename)
ALTER TABLE trips DROP CONSTRAINT IF EXISTS trips_user_id_fkey;
DROP INDEX IF EXISTS idx_trips_user_id;

ALTER TABLE trips RENAME COLUMN user_id TO owner_id;

ALTER TABLE trips ADD CONSTRAINT trips_owner_id_fkey
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_trips_owner_id ON trips(owner_id);

-- Add missing columns
ALTER TABLE trips ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE trips ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE trips ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
