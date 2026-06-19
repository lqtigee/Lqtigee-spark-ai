# Session Format Samples

Ticket: `P0-M019`

## Codex

Sample document:

```text
doc/discovery/codex-sample-1.md
```

Approved first parser format:

```text
JSONL
```

Approved source path:

```text
/home/lqtiger/.codex/sessions/**/*.jsonl
```

Required fields:

```text
id: session_meta.payload.id
workspace: session_meta.payload.cwd or turn_context.payload.cwd
model: turn_context.payload.model
updatedAt: top-level timestamp from newest record or file mtime
rawFile: source file path
```

## opencode

Sample document:

```text
doc/discovery/opencode-sample-1.md
```

Approved first parser format:

```text
SQLITE
```

Approved source path:

```text
/home/lqtiger/.local/share/opencode/opencode.db
```

Required fields:

```text
id: session.id
workspace: session.directory
title: session.title
model: session.model
updatedAt: session.time_updated
rawFile: database path
```

## Blocked Formats

Do not implement these as first parser targets:

```text
Codex SQLite state files
opencode prompt-history.jsonl
opencode log/opencode.log
opencode config JSONC
```

