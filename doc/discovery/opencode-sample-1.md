# opencode Sample 1

Ticket: `P0-M018`

File:

```text
/home/lqtiger/.local/share/opencode/opencode.db
```

File metadata:

```text
SQLite 3.x database
size=201256960
mtime=2026-06-20 01:39:02.034272893 +0900
```

Format:

```text
SQLITE
```

Relevant tables:

```text
session
message
part
project
project_directory
```

Session table columns:

```text
id TEXT PRIMARY KEY
project_id TEXT
parent_id TEXT
slug TEXT
directory TEXT
title TEXT
version TEXT
share_url TEXT
summary_additions INTEGER
summary_deletions INTEGER
summary_files INTEGER
summary_diffs TEXT
revert TEXT
permission TEXT
time_created INTEGER
time_updated INTEGER
time_compacting INTEGER
time_archived INTEGER
workspace_id TEXT
path TEXT
agent TEXT
model TEXT
cost REAL
tokens_input INTEGER
tokens_output INTEGER
tokens_reasoning INTEGER
tokens_cache_read INTEGER
tokens_cache_write INTEGER
metadata TEXT
```

Observed row count:

```text
session: 437
message: 6280
part: 21951
project: 1
workspace: 0
```

Required session fields and where they were observed:

```text
id: session.id
workspace: session.directory
title: session.title
model: session.model JSON text
updatedAt: session.time_updated
rawFile: /home/lqtiger/.local/share/opencode/opencode.db
```

Sanitized row shape:

```json
{"id":"ses_<redacted>","title":"<redacted>","directory":"<path>","model":{"id":"Lqtigee","providerID":"openai","variant":"default"},"agent":"build","time_created":1781853025307,"time_updated":1781887279066}
```

Parser requirements:

- First opencode parser must be SQLite-based, not JSONL-based.
- Query only the `session` table for session list.
- Do not use prompt history as session source.
- `session.model` must be parsed as JSON text to extract model id/provider when present.
- Missing `id`, `directory`, `model`, or `time_updated` must return `OPENCODE_SESSION_FIELD_MISSING`.

