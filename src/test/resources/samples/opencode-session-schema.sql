CREATE TABLE session (
    id TEXT PRIMARY KEY,
    project_id TEXT,
    parent_id TEXT,
    slug TEXT,
    directory TEXT,
    title TEXT,
    version TEXT,
    share_url TEXT,
    summary_additions INTEGER,
    summary_deletions INTEGER,
    summary_files INTEGER,
    summary_diffs TEXT,
    revert TEXT,
    permission TEXT,
    time_created INTEGER,
    time_updated INTEGER,
    time_compacting INTEGER,
    time_archived INTEGER,
    workspace_id TEXT,
    path TEXT,
    agent TEXT,
    model TEXT,
    cost REAL,
    tokens_input INTEGER,
    tokens_output INTEGER,
    tokens_reasoning INTEGER,
    tokens_cache_read INTEGER,
    tokens_cache_write INTEGER,
    metadata TEXT
);

INSERT INTO session (
    id,
    directory,
    title,
    model,
    agent,
    time_created,
    time_updated
) VALUES (
    'ses_redacted',
    '<path>',
    '<redacted>',
    '{"id":"Lqtigee","providerID":"openai","variant":"default"}',
    'build',
    1781853025307,
    1781887279066
);
