-- V23: Taula de comentaris de viatge

CREATE TABLE comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id     UUID        NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    author_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL CHECK (char_length(content) BETWEEN 1 AND 1000),
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_trip_id   ON comments(trip_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_comments_author_id ON comments(author_id) WHERE deleted_at IS NULL;
