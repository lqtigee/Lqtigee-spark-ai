# Backend API Contract

Project: `Lqtigee-spark-ai`  
App name: `Lqtigee`  
Port: `20261`

This contract is the source of truth between backend and frontend. Any backend response shape change must update this file before implementation. Any frontend API type change must reference this file.

## 1. General Rules

- All JSON responses use UTF-8.
- All `/api/**` endpoints except `/api/health` require bearer token.
- All errors return `ApiErrorDto`.
- No endpoint returns mock data.
- No endpoint hides scanner/parser/runtime failure as successful empty data.
- Times are ISO-8601 strings in API responses.
- Internal epoch milliseconds may be used inside readers, but controllers return ISO strings.

## 2. Error Response

Shape:

```json
{
  "code": "MODEL_NOT_FOUND",
  "message": "Model was not found",
  "detail": "modelId=gpt-x",
  "timestamp": "2026-06-20T00:00:00Z",
  "path": "/api/models"
}
```

Fields:

```text
code: required, stable ErrorCode
message: required, safe user-facing text
detail: optional, safe diagnostic text, no secrets
timestamp: required ISO-8601
path: required request path
```

Stable general error codes:

```text
VALIDATION_FAILED
INTERNAL_ERROR
```

Mapping rules:

- Validation exceptions return `VALIDATION_FAILED`.
- Unhandled generic exceptions return `INTERNAL_ERROR`.

## 3. GET /api/health

Auth:

```text
public
```

Success response:

```json
{
  "serviceName": "Lqtigee-spark-ai",
  "appName": "Lqtigee",
  "port": 20261,
  "status": "OK",
  "timestamp": "2026-06-20T00:00:00Z",
  "adapters": [
    {
      "source": "CODEX",
      "available": true,
      "status": "OK",
      "version": "codex-cli 0.141.0",
      "lastErrorCode": null,
      "lastErrorMessage": null
    },
    {
      "source": "OPENCODE",
      "available": true,
      "status": "OK",
      "version": "1.17.8",
      "lastErrorCode": null,
      "lastErrorMessage": null
    }
  ]
}
```

Rules:

- `status` is `OK`, `DEGRADED`, or `FAILED`.
- Health may be `DEGRADED` if one adapter is unavailable.
- Health must not include auth tokens, config file contents, prompts, or env values.

## 4. RemoteSessionDto

Shape:

```json
{
  "id": "019ee090-24e8-7ac1-bd1c-8e4d6788fbf1",
  "source": "CODEX",
  "title": "Codex 019ee090",
  "workspace": "/home/lqtiger",
  "model": "gpt-5.5",
  "status": "UNKNOWN",
  "updatedAt": "2026-06-20T01:52:34+09:00",
  "lastMessage": "",
  "rawFile": "/home/lqtiger/.codex/sessions/2026/06/20/rollout-redacted.jsonl"
}
```

Fields:

```text
id: required
source: required, CODEX or OPENCODE
title: required, safe display title. Codex may use the first visible user chat message as an authenticated chat-derived title.
workspace: required
model: required
status: required
updatedAt: required ISO-8601
lastMessage: required, may be empty when no visible chat message exists. It may contain an authenticated chat-derived preview from the latest visible user or assistant message.
rawFile: required, internal path shown only to authenticated clients
```

Security:

- `title` and `lastMessage` may contain visible user/assistant chat text because this is an authenticated single-user remote control UI.
- Developer, system, tool, reasoning, encrypted, and non-text records must not be used for chat-derived title or `lastMessage`.
- `rawFile` is allowed for authenticated admin UI only because this is a single-user remote control tool.

## 5. GET /api/sessions

Auth:

```text
Bearer token required
```

Success:

```json
{
  "sessions": [
    {
      "id": "019ee090-24e8-7ac1-bd1c-8e4d6788fbf1",
      "source": "CODEX",
      "title": "Codex 019ee090",
      "workspace": "/home/lqtiger",
      "model": "gpt-5.5",
      "status": "UNKNOWN",
      "updatedAt": "2026-06-20T01:52:34+09:00",
      "lastMessage": "",
      "rawFile": "/home/lqtiger/.codex/sessions/..."
    }
  ]
}
```

Failure:

- If Codex scan fails: return 424 with `CODEX_SESSION_SCAN_FAILED` or related code.
- If opencode scan fails: return 424 with `OPENCODE_SESSION_SCAN_FAILED` or related code.
- If either source fails, unified endpoint fails. It must not return partial success.

## 6. GET /api/codex/sessions

Auth:

```text
Bearer token required
```

Success:

```json
{
  "sessions": []
}
```

Rules:

- Empty success is allowed only if Codex scan completed successfully and found zero parseable sessions.
- Parser errors are not empty success.

## 7. GET /api/opencode/sessions

Auth:

```text
Bearer token required
```

Rules:

- Source is SQLite database `/home/lqtiger/.local/share/opencode/opencode.db`.
- First implementation reads only `session` table.
- Does not parse prompt history.
- Does not parse log as primary source.
- Rows with empty `session.model.id` and no non-empty `modelID` in inspected metadata are non-runnable and may be excluded from opencode session list results.
- Excluding those rows is not parser failure hiding: the row has no commandable model id, so no `RemoteSessionDto` with a fake model may be created.
- Any audit that relies on this exclusion must report the excluded row count.
- Other missing required fields still fail with `OPENCODE_SESSION_FIELD_MISSING`; do not convert parser failures into empty success.

## 8. SessionMessageDto

Shape:

```json
{
  "id": "line-3",
  "role": "user",
  "text": "Build the phone session chat view",
  "timestamp": "2026-06-20T00:00:00Z"
}
```

Fields:

```text
id: required, stable within the transcript response
role: required, user or assistant
text: required, non-empty visible text
timestamp: required ISO-8601
```

Rules:

- Messages come only from the real selected Codex JSONL file or opencode SQLite database.
- Developer, system, tool, reasoning, encrypted, snapshot, and non-text records are excluded.
- Empty text messages are excluded.
- No mock transcript, generated summary, or synthesized message may be returned.

## 9. SessionTranscriptDto

Shape:

```json
{
  "session": {
    "id": "019ee090-24e8-7ac1-bd1c-8e4d6788fbf1",
    "source": "CODEX",
    "title": "Build the phone session chat view",
    "workspace": "/home/lqtiger",
    "model": "gpt-5.5",
    "status": "UNKNOWN",
    "updatedAt": "2026-06-20T01:52:34+09:00",
    "lastMessage": "I will wire it to real transcript data.",
    "rawFile": "/home/lqtiger/.codex/sessions/..."
  },
  "messages": [
    {
      "id": "line-3",
      "role": "user",
      "text": "Build the phone session chat view",
      "timestamp": "2026-06-20T00:00:00Z"
    }
  ],
  "pageInfo": {
    "oldestCursor": "line-3",
    "newestCursor": "line-3",
    "hasMoreBefore": true
  }
}
```

Fields:

```text
session: required RemoteSessionDto
messages: required array of SessionMessageDto
pageInfo: required TranscriptPageInfoDto
```

## 10. TranscriptPageInfoDto

Shape:

```json
{
  "oldestCursor": "line-3",
  "newestCursor": "line-12",
  "hasMoreBefore": true
}
```

Fields:

```text
oldestCursor: optional cursor for the oldest message in this page, null when messages is empty
newestCursor: optional cursor for the newest message in this page, null when messages is empty
hasMoreBefore: required boolean, true only when older visible messages exist before oldestCursor
```

Rules:

- `messages` may be empty only when the real selected session contains no visible user/assistant text.
- Empty transcript success must still be tied to a real selected session.
- A missing selected session returns `SESSION_NOT_FOUND`.
- Cursor values are opaque to the frontend and source-specific inside the backend.
- Cursor values must be derived from real Codex JSONL or opencode SQLite ordering, never generated from fake messages.

## 11. GET /api/sessions/{source}/{id}/transcript

Auth:

```text
Bearer token required
```

Path variables:

```text
source: CODEX or OPENCODE
id: selected session id
```

Query parameters:

```text
limit: optional positive integer. Defaults to 10.
before: optional opaque cursor returned by pageInfo.oldestCursor.
```

Paging rules:

- When `limit` is omitted, the backend returns the newest 10 visible user/assistant messages.
- `limit=10` is the default phone chat page size.
- Backend configuration defines the maximum accepted limit; transcript reads must not be unbounded.
- `before=<cursor>` returns visible messages older than the provided cursor.
- Messages are sorted oldest-to-newest within each returned page so the frontend can render chat order directly.
- The first page and older pages must come from the real selected Codex JSONL file or real opencode SQLite rows.
- No generated summary, synthesized message, fake page, or mock transcript may be returned.
- Reader/parser failures return typed errors, never empty successful pages.

Success:

```json
{
  "session": {
    "id": "019ee090-24e8-7ac1-bd1c-8e4d6788fbf1",
    "source": "CODEX",
    "title": "Build the phone session chat view",
    "workspace": "/home/lqtiger",
    "model": "gpt-5.5",
    "status": "UNKNOWN",
    "updatedAt": "2026-06-20T01:52:34+09:00",
    "lastMessage": "I will wire it to real transcript data.",
    "rawFile": "/home/lqtiger/.codex/sessions/..."
  },
  "messages": [
    {
      "id": "line-3",
      "role": "user",
      "text": "Build the phone session chat view",
      "timestamp": "2026-06-20T00:00:00Z"
    },
    {
      "id": "line-5",
      "role": "assistant",
      "text": "I will wire it to real transcript data.",
      "timestamp": "2026-06-20T00:02:00Z"
    }
  ],
  "pageInfo": {
    "oldestCursor": "line-3",
    "newestCursor": "line-5",
    "hasMoreBefore": true
  }
}
```

Failure:

- Missing selected session: `SESSION_NOT_FOUND`.
- Codex read or parse failure: `CODEX_SESSION_SCAN_FAILED` or `CODEX_SESSION_FORMAT_UNKNOWN`.
- opencode read or parse failure: `OPENCODE_SESSION_SCAN_FAILED` or `OPENCODE_SESSION_FORMAT_UNKNOWN`.
- Invalid `limit` or `before`: `VALIDATION_FAILED`.
- Endpoint must not return fake messages when transcript parsing fails.

## 12. ModelDto

Shape:

```json
{
  "id": "openai/Lqtigee",
  "label": "Lqtigee",
  "commandModelName": "openai/Lqtigee",
  "sources": ["OPENCODE"],
  "enabled": true
}
```

## 13. GET /api/models

Auth:

```text
Bearer token required
```

Success:

```json
{
  "models": [
    {
      "id": "gpt-5.5",
      "label": "GPT-5.5",
      "commandModelName": "gpt-5.5",
      "sources": ["CODEX"],
      "enabled": true
    },
    {
      "id": "openai/Lqtigee",
      "label": "Lqtigee",
      "commandModelName": "openai/Lqtigee",
      "sources": ["OPENCODE"],
      "enabled": true
    }
  ]
}
```

Rules:

- Models come from backend configuration.
- Frontend cannot hardcode these.

## 10. POST /api/runs

Auth:

```text
Bearer token required
```

Request:

```json
{
  "sessionId": "ses_121488be4ffeSI5wIkwYvHniqr",
  "source": "OPENCODE",
  "modelId": "openai/Lqtigee",
  "mode": "ASK",
  "prompt": "Summarize current repo status",
  "confirmDangerous": false,
  "codexOptions": {
    "imageAttachmentIds": ["att_image_01"],
    "profile": "work",
    "sandbox": "workspace-write",
    "approvalPolicy": "on-request",
    "searchEnabled": true,
    "addDirAttachmentIds": ["att_dir_01"],
    "configOverrides": [
      {
        "key": "model_reasoning_effort",
        "value": "high"
      }
    ],
    "outputSchemaAttachmentId": null
  }
}
```

Codex-only request options:

```text
codexOptions: optional, only valid when source is CODEX
codexOptions.imageAttachmentIds: optional list of attachment ids mapped later to repeatable --image
codexOptions.profile: optional Codex profile name
codexOptions.sandbox: optional Codex sandbox value
codexOptions.approvalPolicy: optional Codex approval policy value
codexOptions.searchEnabled: optional web search toggle mapped later to --search
codexOptions.addDirAttachmentIds: optional list of directory attachment ids mapped later to repeatable --add-dir
codexOptions.configOverrides: optional structured key/value config override list mapped later to repeatable --config
codexOptions.outputSchemaAttachmentId: optional schema attachment id, only usable after attachment contract exists
```

Success:

```json
{
  "runId": "run_01",
  "sessionId": "ses_121488be4ffeSI5wIkwYvHniqr",
  "source": "OPENCODE",
  "status": "CREATED",
  "startedAt": "2026-06-20T00:00:00Z"
}
```

Failures:

```text
PROMPT_EMPTY
PROMPT_TOO_LONG
SESSION_NOT_FOUND
MODEL_NOT_FOUND
MODEL_SOURCE_UNSUPPORTED
WORKSPACE_NOT_ALLOWED
DANGER_CONFIRM_REQUIRED
PROCESS_START_FAILED
```

Rules:

- Non-zero CLI exit is not success.
- Process start does not mean final run success.

## 11. GET /api/runs/{runId}/events

Auth:

```text
Bearer token required
```

Protocol:

```text
text/event-stream
```

Events:

```text
status
stdout
stderr
tool
done
error
stopped
```

Event data shape:

```json
{
  "runId": "run_01",
  "type": "stdout",
  "message": "line text",
  "timestamp": "2026-06-20T00:00:00Z",
  "data": {}
}
```

Terminal event rule:

```text
Every run emits exactly one terminal event: done, error, or stopped.
```

## 12. POST /api/runs/{runId}/stop

Auth:

```text
Bearer token required
```

Success:

```json
{
  "runId": "run_01",
  "status": "STOPPED"
}
```

Failures:

```text
RUN_NOT_FOUND
RUN_ALREADY_FINISHED
PROCESS_STOP_FAILED
```

## 13. Frontend Type Alignment Rule

Frontend `frontend/src/types/api.ts` must match this contract exactly for:

- `ApiErrorDto`
- `RemoteSession`
- `ModelDto`
- `StartRunRequest`
- `StartRunResponse`
- `RunEventDto`

Any mismatch is a contract bug, not a UI bug.
