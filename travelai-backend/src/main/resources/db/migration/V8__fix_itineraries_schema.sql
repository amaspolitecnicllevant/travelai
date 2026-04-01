-- ── FIX ITINERARIES SCHEMA ─────────────────────────────────────────────────
-- V4 created the itineraries table with content (JSONB), status, generated_by,
-- prompt_used, model_used, tokens_used columns.
-- The Itinerary entity uses: day_number, date, content_json, generated_by_ai, version.
-- This migration adds the missing columns without removing old ones to preserve data.

ALTER TABLE itineraries
    ADD COLUMN IF NOT EXISTS day_number       INTEGER          NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS date             DATE,
    ADD COLUMN IF NOT EXISTS content_json     TEXT             NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS generated_by_ai  BOOLEAN          NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS version          INTEGER          NOT NULL DEFAULT 1;

-- Index to speed up lookups by trip + day_number (used by ItineraryRepository)
CREATE INDEX IF NOT EXISTS idx_itineraries_trip_day
    ON itineraries(trip_id, day_number);
