# Backend Response Fixtures

These JSON examples are contract fixtures for backend tests and frontend type checks. They are not mock business data and must not be used as runtime data.

Rules:

- Fixtures may be used only in tests, documentation, and type validation.
- Runtime endpoints must read real services, real config, or real process state.
- Any response shape change must update this file and `doc/contracts/backend-api-contract.md` in the same contract ticket.

## 1. ApiErrorDto

```json
{
  "code": "AUTH_TOKEN_MISSING",
  "message": "Authorization bearer token is required",
  "detail": null,
  "requestId": "req-test-001",
  "timestamp": "2026-06-20T00:00:00Z"
}
```

Required assertions:

- `code` exists.
- `message` exists.
- `requestId` exists.
- `timestamp` exists.
- Response content type is JSON.

## 2. HealthResponse

```json
{
  "status": "UP",
  "app": "Lqtigee",
  "port": 20261,
  "version": "dev"
}
```

Required assertions:

- `status` is `UP`.
- `app` is `Lqtigee`.
- `port` is `20261`.

## 3. SessionsResponse

```json
{
  "sessions": [
    {
      "id": "codex-session-id",
      "source": "CODEX",
      "title": "Codex session",
      "workspace": "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
      "model": "gpt-5.5",
      "status": "AVAILABLE",
      "updatedAt": "2026-06-20T00:00:00Z",
      "rawFile": "/home/lqtiger/.codex/sessions/yyyy/mm/dd/session.jsonl"
    },
    {
      "id": "opencode-session-id",
      "source": "OPENCODE",
      "title": "opencode session",
      "workspace": "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
      "model": "openai/Lqtigee",
      "status": "AVAILABLE",
      "updatedAt": "2026-06-20T00:00:00Z",
      "rawFile": "/home/lqtiger/.local/share/opencode/opencode.db"
    }
  ]
}
```

Required assertions:

- `sessions` exists and is an array.
- Every item has `id`, `source`, `workspace`, `model`, `status`, and `updatedAt`.
- `source` is only `CODEX` or `OPENCODE`.
- `rawFile` may be present but must not expose transcript content.

## 4. SessionTranscriptResponse

```json
{
  "session": {
    "id": "codex-session-id",
    "source": "CODEX",
    "title": "Build the phone session chat view",
    "workspace": "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
    "model": "gpt-5.5",
    "status": "UNKNOWN",
    "updatedAt": "2026-06-20T00:00:00Z",
    "lastMessage": "I will wire it to real transcript data.",
    "rawFile": "/home/lqtiger/.codex/sessions/yyyy/mm/dd/session.jsonl"
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

Required assertions:

- `session` exists and has the `RemoteSessionDto` fields.
- `messages` exists and is an array.
- Every message has `id`, `role`, `text`, and `timestamp`.
- `role` is only `user` or `assistant`.
- `pageInfo` exists and has the `TranscriptPageInfoDto` fields.
- `pageInfo.oldestCursor` is present when `messages` is non-empty.
- `pageInfo.newestCursor` is present when `messages` is non-empty.
- `pageInfo.hasMoreBefore` is a boolean.
- Developer/system/tool/reasoning records are not present.
- Fixtures must not be used as runtime mock transcript data.
- Runtime `GET /api/sessions/{source}/{id}/transcript?limit=10` returns newest visible messages first, sorted oldest-to-newest within the page.
- Runtime `GET /api/sessions/{source}/{id}/transcript?limit=10&before=<cursor>` returns older visible messages only.
- Runtime transcript pages must not contain generated summaries, fake messages, or mock transcript rows.

## 4.1 TranscriptPageInfoDto

```json
{
  "oldestCursor": "line-3",
  "newestCursor": "line-5",
  "hasMoreBefore": true
}
```

Required assertions:

- `oldestCursor` is a string when the returned page has messages.
- `newestCursor` is a string when the returned page has messages.
- `oldestCursor` and `newestCursor` are `null` when the returned page has zero messages.
- `hasMoreBefore` exists and is a boolean.
- Cursor values are opaque frontend values derived from the real selected Codex JSONL or opencode SQLite ordering.

## 5. ModelsResponse

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

Required assertions:

- `models` exists and is an array.
- Every item has `id`, `label`, `commandModelName`, `sources`, and `enabled`.
- No model appears unless it is configured in backend config.

## 6. StartRunResponse

```json
{
  "runId": "run-test-001",
  "status": "STARTING",
  "eventUrl": "/api/runs/run-test-001/events"
}
```

Required assertions:

- `runId` exists.
- `status` starts as `STARTING`.
- `eventUrl` points to `/api/runs/{runId}/events`.
- No fake run id is returned in runtime code.

## 7. RunEventDto

```json
{
  "runId": "run-test-001",
  "type": "stdout",
  "message": "event text",
  "timestamp": "2026-06-20T00:00:00Z"
}
```

Required assertions:

- `runId` exists.
- `type` exists.
- `timestamp` exists.
- Terminal event type is exactly one of `done`, `error`, or `stopped`.

## 8. StopRunResponse

```json
{
  "runId": "run-test-001",
  "status": "STOPPING"
}
```

Required assertions:

- `runId` exists.
- `status` is `STOPPING`, `STOPPED`, or an already-terminal state.
