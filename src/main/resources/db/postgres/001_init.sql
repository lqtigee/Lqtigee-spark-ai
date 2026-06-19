CREATE TABLE run_records (
    run_id TEXT PRIMARY KEY,
    source TEXT NOT NULL,
    session_id TEXT NOT NULL,
    model_id TEXT NOT NULL,
    status TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    exit_code INTEGER,
    error_code TEXT,
    error_message TEXT
);

CREATE TABLE audit_events (
    event_id BIGSERIAL PRIMARY KEY,
    event_type TEXT NOT NULL,
    run_id TEXT,
    source TEXT,
    session_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    detail JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE settings (
    setting_key TEXT PRIMARY KEY,
    setting_value JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
