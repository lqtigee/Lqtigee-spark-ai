CREATE TABLE run_records (
    run_id TEXT PRIMARY KEY,
    source TEXT NOT NULL,
    session_id TEXT NOT NULL,
    model_id TEXT NOT NULL,
    status TEXT NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ
);
