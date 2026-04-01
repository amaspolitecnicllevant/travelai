-- ── ITINERARIES ───────────────────────────────────────────────
-- Stores AI-generated and user-edited itineraries as structured JSON.
-- One itinerary per trip; versioned via status lifecycle.
CREATE TABLE itineraries (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id     UUID         NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    content     JSONB        NOT NULL DEFAULT '{}',
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    generated_by VARCHAR(50) NOT NULL DEFAULT 'AI',
    prompt_used TEXT,
    model_used  VARCHAR(100),
    tokens_used INTEGER,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT itineraries_status_check
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT itineraries_generated_by_check
        CHECK (generated_by IN ('AI', 'USER', 'MIXED'))
);

CREATE UNIQUE INDEX idx_itineraries_trip_active
    ON itineraries(trip_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_itineraries_trip_id ON itineraries(trip_id);
CREATE INDEX idx_itineraries_status  ON itineraries(status);
