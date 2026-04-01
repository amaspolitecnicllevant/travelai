-- V7: notifications table (the table already exists from V1 but with different schema)
-- Drop and recreate with the correct schema for the NotificationService

-- Add missing columns to existing notifications table
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS message VARCHAR(500);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS entity_id UUID;

-- Populate message from body for existing rows
UPDATE notifications SET message = COALESCE(body, title, 'Notificació') WHERE message IS NULL;

-- Make message NOT NULL after population
ALTER TABLE notifications ALTER COLUMN message SET NOT NULL;

-- Create optimized index for unread queries
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, read, created_at DESC);
