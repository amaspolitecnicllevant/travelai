-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── USERS ─────────────────────────────────────────────────────
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255),
    name         VARCHAR(100) NOT NULL,
    bio          TEXT,
    avatar_url   VARCHAR(500),
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    provider     VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    provider_id  VARCHAR(255),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    -- GDPR: camps de control de compte
    age_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- ── FOLLOWS ───────────────────────────────────────────────────
CREATE TABLE follows (
    follower_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (follower_id, following_id),
    CHECK (follower_id <> following_id)
);

-- ── TRIPS ─────────────────────────────────────────────────────
CREATE TABLE trips (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    destination     VARCHAR(200) NOT NULL,
    destination_lat DOUBLE PRECISION,
    destination_lng DOUBLE PRECISION,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    trip_type       VARCHAR(30)  NOT NULL,
    budget          VARCHAR(20)  NOT NULL,
    travelers       INTEGER      NOT NULL DEFAULT 1,
    visibility      VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',
    notes           TEXT,
    cover_image_url VARCHAR(500),
    ai_generated    BOOLEAN      NOT NULL DEFAULT FALSE,
    tags            TEXT[],
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CHECK (end_date >= start_date),
    CHECK (travelers > 0)
);
CREATE INDEX idx_trips_user_id     ON trips(user_id);
CREATE INDEX idx_trips_visibility  ON trips(visibility);
CREATE INDEX idx_trips_destination ON trips USING gin(destination gin_trgm_ops);
CREATE INDEX idx_trips_trip_type   ON trips(trip_type);
CREATE INDEX idx_trips_start_date  ON trips(start_date);

-- ── DAYS ──────────────────────────────────────────────────────
CREATE TABLE days (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id    UUID        NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    day_number INTEGER     NOT NULL,
    date       DATE        NOT NULL,
    title      VARCHAR(200),
    ai_notes   TEXT,
    user_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (trip_id, day_number)
);
CREATE INDEX idx_days_trip_id ON days(trip_id);

-- ── ACTIVITIES ────────────────────────────────────────────────
CREATE TABLE activities (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    day_id         UUID         NOT NULL REFERENCES days(id) ON DELETE CASCADE,
    time           VARCHAR(5),
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    location       VARCHAR(200),
    lat            DOUBLE PRECISION,
    lng            DOUBLE PRECISION,
    type           VARCHAR(30)  NOT NULL,
    estimated_cost NUMERIC(10,2),
    currency       VARCHAR(3)   DEFAULT 'EUR',
    tips           TEXT,
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_activities_day_id ON activities(day_id);

-- ── RATINGS ───────────────────────────────────────────────────
CREATE TABLE ratings (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trip_id    UUID        NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score      SMALLINT    NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (trip_id, user_id)
);
CREATE INDEX idx_ratings_trip_id ON ratings(trip_id);

-- ── FAVORITES ─────────────────────────────────────────────────
CREATE TABLE favorites (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    trip_id    UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, trip_id)
);

-- ── NOTIFICATIONS ─────────────────────────────────────────────
CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(50) NOT NULL,
    title      VARCHAR(200),
    body       TEXT,
    data       JSONB,
    read       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read);

-- ── REFRESH TOKENS ────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- ── AUDIT LOG (GDPR — registre d'accions sensibles) ───────────
CREATE TABLE audit_logs (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID        REFERENCES users(id) ON DELETE SET NULL,
    action     VARCHAR(100) NOT NULL,
    entity     VARCHAR(50),
    entity_id  UUID,
    ip_address INET,
    user_agent TEXT,
    details    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_user_id   ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action    ON audit_logs(action);
CREATE INDEX idx_audit_logs_created   ON audit_logs(created_at);

-- ── CONSENT LOGS (GDPR — registre de consentiments) ──────────
CREATE TABLE consent_logs (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(50) NOT NULL,
    version      VARCHAR(20) NOT NULL,
    accepted     BOOLEAN     NOT NULL,
    ip_address   INET,
    user_agent   TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_consent_logs_user_id ON consent_logs(user_id);

-- ── DELETION REQUESTS (GDPR — dret a l'oblit) ────────────────
CREATE TABLE deletion_requests (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requested_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scheduled_for TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '30 days',
    completed_at  TIMESTAMPTZ,
    reason        TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
CREATE INDEX idx_deletion_requests_status       ON deletion_requests(status);
CREATE INDEX idx_deletion_requests_scheduled    ON deletion_requests(scheduled_for);

-- ── LOGIN ATTEMPTS (seguretat — anti brute-force) ─────────────
CREATE TABLE login_attempts (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email        VARCHAR(255) NOT NULL,
    ip_address   INET,
    success      BOOLEAN NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_login_attempts_email      ON login_attempts(email, created_at);
CREATE INDEX idx_login_attempts_ip         ON login_attempts(ip_address, created_at);

-- ── LEGAL DOCUMENTS (versions dels documents legals) ──────────
CREATE TABLE legal_documents (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type         VARCHAR(50)  NOT NULL,
    version      VARCHAR(20)  NOT NULL,
    content      TEXT         NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    published_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (type, version)
);
CREATE INDEX idx_legal_documents_type_active ON legal_documents(type, active);
