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

## 7.1 OpencodeAgentDto

Shape:

```json
{
  "id": "build",
  "name": "build",
  "source": "primary"
}
```

Fields:

```text
id: required, opencode agent id/name passed later to --agent
name: required, display name from real opencode agent list/config output
source: required, real opencode source label such as primary or subagent
```

Rules:

- Agent rows come only from real opencode source/config or verified CLI evidence.
- `source` must describe the real opencode agent source, not a fake default.
- No fake agents may be synthesized.
- Agent list failures return typed errors, never fallback empty success arrays.

## 7.2 GET /api/opencode/agents

Auth:

```text
Bearer token required
```

Source:

```text
opencode agent list
```

Success:

```json
{
  "opencodeAgents": [
    {
      "id": "build",
      "name": "build",
      "source": "primary"
    },
    {
      "id": "explore",
      "name": "explore",
      "source": "subagent"
    }
  ]
}
```

Failures:

```text
OPENCODE_AGENT_LIST_FAILED
OPENCODE_AGENT_OUTPUT_INVALID
OPENCODE_AGENT_SOURCE_UNAVAILABLE
```

Rules:

- The endpoint is protected by the same bearer token as other `/api/**` endpoints.
- The backend must not return fake agents.
- The backend must not return a fallback empty success response when agent listing fails.
- Empty success is allowed only when the real opencode source was listed successfully and returned zero agents.

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

## 13.1 SourceCapabilityDto

Shape:

```json
{
  "source": "OPENCODE",
  "runOptions": ["model", "agent", "fork", "share", "variant", "thinking", "replay", "replayLimit"],
  "attachments": ["file"],
  "sessionActions": [],
  "dangerousOptions": ["shellDangerouslySkipPermissions"]
}
```

Fields:

```text
source: required, CODEX or OPENCODE
runOptions: required array of stable option ids enabled for this source
attachments: required array of stable attachment capability ids enabled for this source
sessionActions: required array of stable session action ids enabled for this source
dangerousOptions: required array of stable dangerous option ids enabled for this source
```

Capability id rules:

- Capability ids are backend-owned strings, not frontend guesses.
- Capability ids must be based on verified local CLI help and must also be backed by backend validation and command builder tests before runtime returns them.
- Local CLI help evidence alone is not enough to return a runtime capability.
- Runtime code must not claim a capability until backend validation and command builder tests exist for that capability.
- The frontend must hide source-specific controls that are not listed for the selected session source.
- No capability may be added as a frontend hardcoded fake switch.

Currently enabled source capabilities from implemented backend behavior:

```text
CODEX runOptions: model
CODEX attachments: image
CODEX sessionActions: none
CODEX dangerousOptions: none

OPENCODE runOptions: model, agent, fork, share, variant, thinking, replay, replayLimit
OPENCODE attachments: file
OPENCODE sessionActions: none
OPENCODE dangerousOptions: shellDangerouslySkipPermissions
```

Rules:

- `attachments` includes only capabilities whose ids are resolved into safe CLI argument arrays by source-specific command builder tests.
- `sessionActions` stays empty until source-specific session action command builders and endpoint contracts exist.
- Codex dangerous shell mode stays unavailable until a verified Codex command path supports it and command builder tests pass.
- Empty arrays are allowed only as an honest statement that no backend-supported capability exists for that field; they must not hide capability calculation failures.

## 13.2 GET /api/capabilities

Auth:

```text
Bearer token required
```

Success:

```json
{
  "capabilities": [
    {
      "source": "CODEX",
      "runOptions": ["model"],
      "attachments": ["image"],
      "sessionActions": [],
      "dangerousOptions": []
    },
    {
      "source": "OPENCODE",
      "runOptions": ["model", "agent", "fork", "share", "variant", "thinking", "replay", "replayLimit"],
      "attachments": ["file"],
      "sessionActions": [],
      "dangerousOptions": ["shellDangerouslySkipPermissions"]
    }
  ]
}
```

Failure:

- Missing token: `AUTH_TOKEN_MISSING`.
- Invalid token: `AUTH_TOKEN_INVALID`.
- Unexpected capability service failure: `INTERNAL_ERROR`.

Rules:

- The endpoint returns one `SourceCapabilityDto` per source that the backend knows how to evaluate.
- Capability examples are limited to locally verified CLI help and already implemented backend behavior.
- The backend must not return fake capabilities to make frontend controls appear.
- The frontend must call this endpoint before rendering source-specific options.
- The endpoint must not include tokens, prompts, transcript text, raw process output, or config file contents.

## 13.3 SessionActionRequest

Shape:

```json
{
  "action": "delete",
  "confirmDestructive": true
}
```

Fields:

```text
action: required stable action id allowed for the source
confirmDestructive: required boolean, true only after explicit user confirmation
```

Allowed action ids:

```text
CODEX: archive, delete, unarchive, fork
OPENCODE: delete, export, import, fork
```

Destructive action ids:

```text
CODEX: delete
OPENCODE: delete, import
```

Rules:

- The request is source-scoped by URL and must not infer source from an unscoped session id.
- Destructive actions require `confirmDestructive=true`.
- Non-destructive actions must ignore `confirmDestructive` for permission escalation; the flag is only a confirmation marker.
- Backend must reject any action id not allowed for the URL source.
- Backend must reject any action id that is not currently present in that source's runtime `sessionActions` capability.
- A later endpoint/service ticket must verify the selected session still exists before building or launching any action process.

## 13.4 SessionActionResponse

Shape:

```json
{
  "actionId": "act_01J00000000000000000000000",
  "source": "CODEX",
  "sessionId": "019ee090-24e8-7ac1-bd1c-8e4d6788fbf1",
  "action": "archive",
  "status": "STARTED",
  "startedAt": "2026-06-20T00:00:00Z"
}
```

Fields:

```text
actionId: required backend-generated action id
source: required CODEX or OPENCODE, copied from URL source
sessionId: required, copied from URL id
action: required accepted action id
status: required, STARTED, COMPLETED, FAILED, or REJECTED
startedAt: required ISO-8601 timestamp
```

Rules:

- Response must not include transcript text, exported transcript content, tokens, process ids, or raw filesystem paths.
- Export output, if implemented later, must be streamed or fetched through an authenticated endpoint and must not be written to docs.

## 13.5 POST /api/sessions/{source}/{id}/actions

Auth:

```text
Bearer token required
```

Success request example:

```http
POST /api/sessions/CODEX/019ee090-24e8-7ac1-bd1c-8e4d6788fbf1/actions
```

```json
{
  "action": "archive",
  "confirmDestructive": false
}
```

Destructive request example:

```http
POST /api/sessions/CODEX/019ee090-24e8-7ac1-bd1c-8e4d6788fbf1/actions
```

```json
{
  "action": "delete",
  "confirmDestructive": true
}
```

Failures:

```text
VALIDATION_FAILED
AUTH_TOKEN_MISSING
AUTH_TOKEN_INVALID
SESSION_NOT_FOUND
DANGER_CONFIRM_REQUIRED
PROCESS_START_FAILED
```

Rules:

- The endpoint is source-scoped: `{source}` must be `CODEX` or `OPENCODE`.
- `{id}` is the selected real session id for that source.
- A CODEX URL may accept only `archive`, `delete`, `unarchive`, or `fork`.
- An OPENCODE URL may accept only `delete`, `export`, `import`, or `fork`.
- `delete` must require explicit confirmation for both sources.
- `import` must require explicit confirmation for OPENCODE because it can mutate session state.
- No action endpoint may accept raw filesystem paths.
- No action endpoint may run through shell strings.
- Runtime capabilities must keep `sessionActions` empty until the corresponding source action command builder and service tests pass.

## 13.6 AttachmentDto

Shape:

```json
{
  "id": "att_file_01",
  "filename": "context.txt",
  "contentType": "text/plain",
  "sizeBytes": 1280,
  "createdAt": "2026-06-20T00:00:00Z"
}
```

Fields:

```text
id: required, server-owned attachment id
filename: required original safe display filename, not a filesystem path
contentType: required media type accepted by backend allowlist
sizeBytes: required uploaded file size in bytes
createdAt: required ISO-8601
```

Rules:

- Attachment files are stored only under a configured Lqtigee temp directory.
- Frontend never sends raw filesystem paths for CLI attachment flags.
- Attachment ids must be resolved server-side to safe paths inside the configured attachment directory.
- Missing, deleted, forbidden, oversized, or path-escaping attachments return typed errors.
- Codex image attachments may map to repeatable `--image` after server-side id resolution.
- opencode file attachments may map to repeatable `--file` after server-side id resolution.

## 13.7 POST /api/attachments

Auth:

```text
Bearer token required
```

Request:

```text
multipart/form-data field name: file
```

Success:

```json
{
  "id": "att_file_01",
  "filename": "context.txt",
  "contentType": "text/plain",
  "sizeBytes": 1280,
  "createdAt": "2026-06-20T00:00:00Z"
}
```

Failures:

```text
ATTACHMENT_MISSING
ATTACHMENT_TOO_LARGE
ATTACHMENT_CONTENT_TYPE_FORBIDDEN
ATTACHMENT_STORAGE_FAILED
```

Rules:

- Uploaded files must be written only under the configured Lqtigee temp directory.
- Backend generates attachment ids; the frontend cannot choose ids.
- Response must not expose raw filesystem paths.

## 13.8 DELETE /api/attachments/{id}

Auth:

```text
Bearer token required
```

Success:

```json
{
  "id": "att_file_01",
  "deleted": true
}
```

Failures:

```text
ATTACHMENT_NOT_FOUND
ATTACHMENT_DELETE_FAILED
```

Rules:

- Delete only files owned by the attachment service.
- Deleting an attachment must never follow or accept a raw path from the frontend.

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
  },
  "opencodeOptions": {
    "agent": "build",
    "fork": false,
    "share": false,
    "variant": "high",
    "thinking": true,
    "replay": true,
    "replayLimit": 10,
    "fileAttachmentIds": ["att_file_01"]
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
codexOptions.addDirAttachmentIds: optional list of directory attachment ids mapped later to repeatable --add-dir after server-side id resolution
codexOptions.configOverrides: optional structured key/value config override list mapped later to repeatable --config
codexOptions.outputSchemaAttachmentId: optional schema attachment id resolved server-side, only usable after attachment service exists
```

opencode-only request options:

```text
opencodeOptions: optional, only valid when source is OPENCODE
opencodeOptions.agent: optional agent id/name mapped later to --agent
opencodeOptions.fork: optional fork toggle mapped later to --fork
opencodeOptions.share: optional share toggle mapped later to --share
opencodeOptions.variant: optional provider-specific variant mapped later to --variant
opencodeOptions.thinking: optional thinking display toggle mapped later to --thinking
opencodeOptions.replay: optional replay toggle mapped later to --replay or --no-replay
opencodeOptions.replayLimit: optional newest-message replay cap mapped later to --replay-limit
opencodeOptions.fileAttachmentIds: optional list of attachment ids mapped later to repeatable --file after server-side id resolution
```

Shared attachment request rules:

```text
attachmentIds: server-owned ids only, never raw frontend file paths
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
ATTACHMENT_NOT_FOUND
ATTACHMENT_PATH_INVALID
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
