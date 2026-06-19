# Codex Sample 1

Ticket: `P0-M017`

File:

```text
/home/lqtiger/.codex/sessions/2026/06/20/rollout-2026-06-20T00-46-43-019ee090-24e8-7ac1-bd1c-8e4d6788fbf1.jsonl
```

File metadata:

```text
size=1226864
mtime=2026-06-20 01:52:34.373903945 +0900
```

Format:

```text
JSONL
```

Top-level record keys:

```text
payload
timestamp
type
```

Observed record types:

```text
session_meta
event_msg
response_item
turn_context
```

Observed counts in sample file:

```text
response_item: 296
event_msg: 163
turn_context: 14
session_meta: 1
```

Required session fields and where they were observed:

```text
id: payload.id from type=session_meta
workspace/cwd: payload.cwd from type=session_meta or type=turn_context
model: payload.model from type=turn_context
updatedAt: top-level timestamp from newest record or file mtime
rawFile: source path
```

Sanitized structural examples:

```json
{"type":"session_meta","timestamp":"<iso>","payload":{"id":"<uuid>","cwd":"<path>","cli_version":"<version>","source":"<source>","model_provider":"<provider>","originator":"<originator>","thread_source":"<thread_source>"}}
{"type":"turn_context","timestamp":"<iso>","payload":{"cwd":"<path>","model":"<model>","approval_policy":"<policy>","sandbox_policy":{"type":"<policy>"},"workspace_roots":["<path>"],"current_date":"<date>","timezone":"<tz>"}}
```

Parser requirements:

- Do not parse prompt content for required fields.
- Do not fallback to filename for required `id`, `workspace`, or `model`.
- If `session_meta.payload.id` is missing, return `CODEX_SESSION_FIELD_MISSING`.
- If no `turn_context.payload.model` exists, return `CODEX_SESSION_FIELD_MISSING`.

