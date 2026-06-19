# Lqtigee-spark-ai Requirements

## 1. Fixed Project Constants

- Project directory: `/home/lqtiger/GIT_HUB/Lqtigee-spark-ai`
- Project name: `Lqtigee-spark-ai`
- App name shown to users: `Lqtigee`
- Backend port: `20261`
- Backend base URL in local development: `http://127.0.0.1:20261`
- Public mapping: handled outside this project
- Backend language: Java
- Backend framework: Spring Boot 3
- Java version: 21
- Service database: PostgreSQL for Lqtigee-owned persistent data
- Mobile install target: Android PWA

## 2. Main User Goal

The user opens `Lqtigee` on an Android phone, connects to the Java service on port `20261`, sees real Codex CLI and opencode sessions currently present on the server, chooses one session and one model, sends a command, watches real execution output, and can stop the run.

## 3. Absolute Prohibitions

These prohibitions apply to every implementation task:

1. Do not create fake sessions.
2. Do not create fake models.
3. Do not use mock API responses.
4. Do not let the UI show fabricated success states.
5. Do not return empty lists when a scan failed.
6. Do not continue after an unknown session file format.
7. Do not parse formats by guessing field names during production code implementation.
8. Do not execute shell command strings.
9. Do not pass unvalidated workspace paths to a process.
10. Do not read arbitrary paths outside configured roots.
11. Do not put Codex/opencode file logic in the frontend.
12. Do not put file scanning or process launching in controllers.

## 4. BLOCKED Rule

When a required fact is missing, the AI must stop and output:

```text
BLOCKED

Ticket:
Reason:
Missing information:
Required user input or required discovery artifact:
Files changed:
Files not changed:
```

Examples that must block:

- Codex CLI command syntax is unknown.
- opencode run command syntax is unknown.
- Session file format cannot be classified.
- Required path does not exist.
- Port `20261` is occupied.
- Token configuration is missing.
- A parser cannot extract required fields from real sample data.

## 5. Backend Functional Requirements

### 5.1 Health

The backend must expose:

```text
GET /api/health
```

It returns real server and adapter status:

- service name
- app name
- port
- uptime
- auth enabled
- Codex adapter status
- opencode adapter status
- active run count

The response must not include secrets.

### 5.2 Authentication

All `/api/**` endpoints except `/api/health` require:

```text
Authorization: Bearer <token>
```

The token comes from backend configuration. Wrong or missing token returns `401`.

### 5.3 Session Discovery

The backend must discover:

- Codex CLI sessions from configured Codex roots.
- opencode sessions from configured opencode roots.

Discovery must be real. If scanner or parser fails, the endpoint returns a typed error.
PostgreSQL must not replace Codex/opencode source discovery in v1. Codex sessions still come from Codex session files, and opencode sessions still come from the verified opencode SQLite database.

Unified endpoint:

```text
GET /api/sessions
```

Specific endpoints:

```text
GET /api/codex/sessions
GET /api/opencode/sessions
```

`GET /api/sessions` fails with `424 FAILED_DEPENDENCY` if either source fails.

### 5.4 Models

Models are read from backend configuration only:

```text
GET /api/models
```

The frontend must never hardcode the business model list.

Each model must define:

- id
- label
- command model name
- supported source list
- enabled flag

### 5.5 Command Execution

The backend must expose:

```text
POST /api/runs
```

Request must include:

- session id
- source
- model id
- command mode
- prompt
- dangerous confirmation flag

Rules:

- Prompt cannot be empty.
- Prompt cannot exceed configured max length.
- Model must support source.
- Session must exist.
- Workspace must be inside allowed roots.
- Shell mode requires explicit confirmation.
- Command must be executed with `ProcessBuilder` argument arrays only.
- Command mode permissions must follow `doc/security/command-permission-matrix.md`.

### 5.6 Run Events

The backend must expose:

```text
GET /api/runs/{runId}/events
```

It streams real SSE events:

- `status`
- `stdout`
- `stderr`
- `tool`
- `done`
- `error`

No fake log events are allowed.

### 5.7 Stop Run

The backend must expose:

```text
POST /api/runs/{runId}/stop
```

It must:

1. Find the real process.
2. Try graceful stop.
3. Wait configured timeout.
4. Force kill if still alive.
5. Mark run `STOPPED`.
6. Emit stop event.

## 6. Frontend Functional Requirements

### 6.1 Pages

Frontend has exactly these primary pages:

```text
/           Overview
/sessions   Sessions
/control    Control
/runs       Runs
/settings   Settings
```

### 6.2 Overview Page

Shows only real API-derived state:

- connection status
- backend port `20261`
- Codex adapter health
- opencode adapter health
- active runs

If `/api/health` fails, show the real error. Do not show sample cards.

### 6.3 Sessions Page

Shows real sessions from `/api/sessions`.

States:

- loading
- error
- empty success
- success with session list

Empty success is allowed only when backend explicitly reports successful scan with zero sessions.

### 6.4 Control Page

Fields:

- session selector
- model selector
- command mode selector
- prompt textarea
- dangerous confirmation checkbox
- send button

Send button is disabled unless:

- a real session is selected
- a real model is selected
- prompt is non-empty
- dangerous mode is confirmed when required

### 6.5 Runs Page

Shows:

- run status
- selected run id
- SSE event timeline
- stop button

All timeline rows must come from real SSE events.

### 6.6 Settings Page

Stores only frontend-local settings:

- backend base URL
- token
- refresh interval

It must not edit server scanner paths, command templates, or model config in the first version.

## 7. Mobile UI Requirements

- Minimum width: 320px.
- Target Android width: 360px to 430px.
- No horizontal scroll.
- Bottom nav on mobile.
- Desktop side nav at width >= 900px.
- Touch targets at least 44px high.
- Content bottom padding accounts for safe area.
- No nested cards.
- No decorative landing page.
- First screen is operational Overview.

## 8. Error Codes

Backend errors must use stable codes:

```text
AUTH_TOKEN_MISSING
AUTH_TOKEN_INVALID
VALIDATION_FAILED
INTERNAL_ERROR
PORT_UNAVAILABLE
CODEX_HOME_NOT_FOUND
CODEX_BIN_NOT_FOUND
CODEX_SESSION_SCAN_FAILED
CODEX_SESSION_FORMAT_UNKNOWN
CODEX_SESSION_FIELD_MISSING
OPENCODE_BIN_NOT_FOUND
OPENCODE_CONFIG_NOT_FOUND
OPENCODE_SESSION_SCAN_FAILED
OPENCODE_SESSION_FORMAT_UNKNOWN
OPENCODE_SESSION_FIELD_MISSING
MODEL_NOT_FOUND
MODEL_SOURCE_UNSUPPORTED
SESSION_NOT_FOUND
WORKSPACE_NOT_ALLOWED
PROMPT_EMPTY
PROMPT_TOO_LONG
DANGER_CONFIRM_REQUIRED
RUN_NOT_FOUND
RUN_ALREADY_FINISHED
PROCESS_START_FAILED
PROCESS_STOP_FAILED
SSE_SUBSCRIBE_FAILED
```

Validation exceptions must map to `VALIDATION_FAILED`.

Unhandled generic exceptions must map to `INTERNAL_ERROR`.

## 9. Completion Definition

The project is complete only when:

1. Java service starts on `20261`.
2. Android phone can load UI.
3. Android can install PWA named `Lqtigee`.
4. `/api/health` returns real adapter status.
5. `/api/sessions` returns real Codex/opencode sessions or a typed failure.
6. `/api/models` returns configured real models.
7. UI can select session and model.
8. UI can start a real run.
9. UI can stream real run output.
10. UI can stop a real run.
11. All plan tickets are marked complete with verification output.
