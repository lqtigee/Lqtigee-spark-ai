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
title: required, safe display title
workspace: required
model: required
status: required
updatedAt: required ISO-8601
lastMessage: required, may be empty in v1
rawFile: required, internal path shown only to authenticated clients
```

Security:

- `lastMessage` must not contain prompt content in v1.
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

## 8. ModelDto

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

## 9. GET /api/models

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
  "confirmDangerous": false
}
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
