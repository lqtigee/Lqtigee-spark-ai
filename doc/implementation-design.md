# Runtime-Informed Implementation Design

Project: `Lqtigee-spark-ai`  
App name: `Lqtigee`  
Backend port: `20261`

This document converts the local discovery results into an implementation design. It is not a discovery report. It tells future agents exactly how the project should be built based on verified local facts.

## 1. What Changed After Looking

Before discovery, the project design had two dangerous unknowns:

1. How Codex CLI stores sessions.
2. How opencode stores sessions.

Discovery resolved both enough for first implementation:

```text
Codex sessions: JSONL files under /home/lqtiger/.codex/sessions/**/*.jsonl
opencode sessions: SQLite rows in /home/lqtiger/.local/share/opencode/opencode.db, table session
Lqtigee-owned persistence: PostgreSQL
```

Therefore the implementation design is now:

```text
CodexSessionReader = JSONL reader
OpencodeSessionReader = SQLite reader
LqtigeePersistence = PostgreSQL repositories for service-owned state
```

Do not implement opencode JSONL parsing for sessions in v1. `prompt-history.jsonl` is not a session source.

## 2. Backend Implementation Shape

### 2.1 Package Layout

```text
com.lqtigee.sparkai
  LqtigeeSparkAiApplication

com.lqtigee.sparkai.config
  RemoteProperties
  SecurityProperties
  ModelProperties

com.lqtigee.sparkai.dto
  ApiErrorDto
  HealthDto
  AdapterHealthDto
  RemoteSessionDto
  ModelDto
  StartRunRequest
  StartRunResponse
  RunEventDto
  AgentSource
  SessionStatus
  RunStatus
  CommandMode

com.lqtigee.sparkai.web
  HealthController
  SessionController
  ModelController
  RunController

com.lqtigee.sparkai.service
  HealthService
  SessionService
  ModelService
  RunService

com.lqtigee.sparkai.codex
  CodexFileScanner
  CodexJsonlSessionReader
  CodexCommandBuilder

com.lqtigee.sparkai.opencode
  OpencodeSqliteSessionReader
  OpencodeCommandBuilder

com.lqtigee.sparkai.runtime
  CommandSpec
  ManagedProcess
  ProcessLauncher
  ProcessOutputPump
  RunRegistry
  RunEventBus

com.lqtigee.sparkai.security
  BearerTokenFilter

com.lqtigee.sparkai.error
  ErrorCode
  ApiException
  GlobalExceptionHandler
```

## 3. Configuration Design

### 3.1 application.yml Required Shape

```yaml
server:
  port: 20261

spring:
  application:
    name: Lqtigee-spark-ai

lqtigee:
  security:
    api-token: ${LQTIGEE_API_TOKEN:}
  paths:
    codex-home: /home/lqtiger/.codex
    opencode-db: /home/lqtiger/.local/share/opencode/opencode.db
    opencode-bin: /home/lqtiger/.opencode/bin/opencode
    codex-bin: /home/lqtiger/.npm-global/bin/codex
    allowed-workdirs:
      - /home/lqtiger
      - /home/lqtiger/GIT_HUB
      - /home/lqtiger/Downloads
  models:
    entries:
      - id: gpt-5.5
        label: GPT-5.5
        command-model-name: gpt-5.5
        sources: [CODEX]
        enabled: true
      - id: openai/Lqtigee
        label: Lqtigee
        command-model-name: openai/Lqtigee
        sources: [OPENCODE]
        enabled: true
```

### 3.2 RemoteProperties

Fields:

```java
Path codexHome;
Path codexBin;
Path opencodeDb;
Path opencodeBin;
List<Path> allowedWorkdirs;
int maxPromptChars;
Duration processStopTimeout;
```

Methods:

```java
void validate()
```

Must check:

- `codexHome` exists and is readable.
- `codexBin` exists and is executable.
- `opencodeDb` exists and is readable.
- `opencodeBin` exists and is executable.
- `allowedWorkdirs` is not empty.

Do not silently create missing paths.

## 4. Codex Session Design

### 4.1 Verified Source

Source:

```text
/home/lqtiger/.codex/sessions/**/*.jsonl
```

Verified structure:

```json
{"type":"session_meta","timestamp":"<iso>","payload":{"id":"<uuid>","cwd":"<path>","cli_version":"<version>","source":"<source>","model_provider":"<provider>","originator":"<originator>","thread_source":"<thread_source>"}}
{"type":"turn_context","timestamp":"<iso>","payload":{"cwd":"<path>","model":"<model>","workspace_roots":["<path>"]}}
```

### 4.2 CodexFileScanner

Method:

```java
List<Path> scan(Path codexHome)
```

Algorithm:

1. Require `codexHome` exists.
2. Resolve `codexHome/sessions`.
3. Require sessions directory exists.
4. Walk recursively.
5. Include only files ending `.jsonl`.
6. Return absolute normalized paths sorted by last modified descending.

Failure:

- Missing home or sessions directory: `CODEX_HOME_NOT_FOUND`.
- IO failure: `CODEX_SESSION_SCAN_FAILED`.

### 4.3 CodexJsonlSessionReader

Method:

```java
RemoteSessionDto read(Path jsonlFile)
```

Required extraction:

```text
id -> first record where type=session_meta, payload.id
workspace -> session_meta.payload.cwd, or turn_context.payload.cwd
model -> newest or first turn_context.payload.model
updatedAt -> newest top-level timestamp found in file, else file mtime
status -> RUNNING only when event_msg task_started turn ids are not fully matched by task_complete
title -> use id or "Codex " + short id; do not parse prompt content for title in v1
lastMessage -> empty string or safe metadata only; do not expose prompt content in v1
rawFile -> absolute file path
```

Important:

- Do not use filename as session id.
- Do not use prompt content as title in v1.
- Do not expose `base_instructions`, prompt text, encrypted content, or full transcript.
- Do not infer `RUNNING` from file freshness, process list, or frontend local run state.

Failure:

- Invalid JSON line: `CODEX_SESSION_FORMAT_UNKNOWN`.
- Missing `payload.id`: `CODEX_SESSION_FIELD_MISSING`.
- Missing model: `CODEX_SESSION_FIELD_MISSING`.
- Missing workspace: `CODEX_SESSION_FIELD_MISSING`.

### 4.4 Codex DTO Mapping

```java
RemoteSessionDto(
  id = payload.id,
  source = CODEX,
  title = "Codex " + id.substring(0, 8),
  workspace = cwd,
  model = model,
  status = ACTIVE,
  updatedAt = latestTimestamp,
  lastMessage = "",
  rawFile = jsonlFile.toString()
)
```

Status is `RUNNING` only when a Codex JSONL file contains at least one `event_msg` with `payload.type = task_started` and a `turn_id` that has not been matched by an `event_msg` with `payload.type = task_complete` for the same `turn_id`. Status is `ACTIVE` when a Codex JSONL file is successfully parsed into a selectable session and no unmatched task is present.

## 5. opencode Session Design

### 5.1 Verified Source

Source:

```text
/home/lqtiger/.local/share/opencode/opencode.db
```

Verified table:

```text
session
```

Required columns:

```text
id
directory
title
model
time_created
time_updated
time_archived
path
agent
```

Model column shape:

```json
{"id":"Lqtigee","providerID":"openai","variant":"default"}
```

### 5.2 OpencodeSqliteSessionReader

Method:

```java
List<RemoteSessionDto> readSessions(Path databasePath)
```

Algorithm:

1. Require database exists and is readable.
2. Open SQLite connection read-only.
3. Confirm `session` table exists.
4. Query:

```sql
SELECT id, directory, title, model, time_updated, time_archived, path, agent
FROM session
ORDER BY time_updated DESC
LIMIT ?
```

5. Before DTO mapping, identify rows with empty `session.model.id`.
6. If a row with empty `session.model.id` has no non-empty `modelID` in inspected opencode metadata, classify it as a non-runnable excluded row.
7. Excluded rows are not parser failures and must not produce fake `RemoteSessionDto.model` values.
8. Map remaining rows to `RemoteSessionDto`.
9. Skip archived rows only if `time_archived` is not null and a future ticket explicitly wants hidden archived sessions. In v1 include them but mark status accordingly.
10. Runtime audits must report excluded row count.
11. For non-archived rows, compute `RUNNING` only from the latest assistant `message` row missing both `time.completed` and `finish`.

Failure:

- Missing database: `OPENCODE_CONFIG_NOT_FOUND`.
- SQL open/query failure: `OPENCODE_SESSION_SCAN_FAILED`.
- Missing required field on a non-excluded row: `OPENCODE_SESSION_FIELD_MISSING`.

### 5.3 opencode Model Parsing

Method:

```java
String parseModelId(String modelJson)
```

Algorithm:

1. Parse JSON object.
2. Read `providerID`.
3. Read `id`.
4. If both exist, return `providerID + "/" + id`.
5. If only `id` exists, return `id`.
6. If `id` is empty and no non-empty `modelID` exists in inspected metadata, mark the row non-runnable for exclusion before calling DTO mapping.

For discovered local data:

```text
{"id":"Lqtigee","providerID":"openai"} -> openai/Lqtigee
```

Do not fallback to "Lqtigee" unless JSON truly lacks providerID.
Do not use providerID alone as a model id.

### 5.4 opencode DTO Mapping

```java
RemoteSessionDto(
  id = session.id,
  source = OPENCODE,
  title = session.title,
  workspace = session.directory,
  model = parseModelId(session.model),
  status = statusFor(session.id, time_archived),
  updatedAt = Instant.ofEpochMilli(time_updated),
  lastMessage = "",
  rawFile = databasePath.toString()
)
```

`IDLE` means the row is archived. `RUNNING` means the row is non-archived and the latest real assistant `message` row for that session has no `$.time.completed` and no `$.finish`. `ACTIVE` means the row is non-archived, selectable, and does not have an incomplete latest assistant message. Do not infer `RUNNING` from the session row alone, file freshness, process list, or frontend local run state.

Do not expose prompt text from `message`, `part`, `prompt-history.jsonl`, or logs in v1.

## 5.5 PostgreSQL Persistence Boundary

PostgreSQL is the database for Lqtigee-owned state. It is not the first source for Codex or opencode session discovery.

Initial PostgreSQL responsibilities:

```text
run_records
audit_events
settings
```

Do not store fabricated sessions in PostgreSQL. Do not return PostgreSQL rows as live Codex/opencode sessions unless the relevant source adapter has successfully read the real Codex/opencode source in the same request flow or a later explicit cache-coherency ticket defines exact freshness rules.

Connection configuration:

```yaml
lqtigee:
  database:
    url: jdbc:postgresql://localhost:5432/lqtigee_spark_ai
    username: lqtigee
    password: ${LQTIGEE_DB_PASSWORD}
```

Required behavior:

1. Missing PostgreSQL configuration fails startup or health with a typed dependency error once PostgreSQL persistence is enabled.
2. Repositories must use parameterized JDBC or Spring JDBC APIs.
3. Runtime command execution must not depend on PostgreSQL until run persistence tickets are complete.
4. Session discovery must not silently fall back to PostgreSQL if Codex/opencode source reads fail.

## 6. SessionService Design

Constructor dependencies:

```java
CodexFileScanner codexFileScanner;
CodexJsonlSessionReader codexReader;
OpencodeSqliteSessionReader opencodeReader;
RemoteProperties properties;
```

Methods:

```java
List<RemoteSessionDto> listCodexSessions()
List<RemoteSessionDto> listOpencodeSessions()
List<RemoteSessionDto> listAllSessions()
RemoteSessionDto getRequiredSession(AgentSource source, String id)
```

Rules:

- `listCodexSessions()` scans and reads Codex JSONL.
- `listOpencodeSessions()` reads SQLite sessions.
- `listAllSessions()` calls both.
- If either source fails, `listAllSessions()` returns typed failure, not partial success.
- Source-specific endpoints may succeed independently.

Sorting:

```text
updatedAt descending
```

## 7. ModelService Design

Model data comes from `application.yml`, based on `doc/discovery/model-catalog.md`.

Methods:

```java
List<ModelDto> listModels()
ModelDto getRequiredModel(String modelId)
void validateModelForSource(String modelId, AgentSource source)
String commandModelName(String modelId)
```

Rules:

- No model ids in frontend.
- No model ids hardcoded in controllers.
- Disabled model cannot be used in `POST /api/runs`.

## 8. Command Builder Design

Command execution is not session discovery. Do not mix them.

### 8.1 CodexCommandBuilder

Method:

```java
CommandSpec build(StartRunRequest request, RemoteSessionDto session, ModelDto model)
```

Argument array:

```text
[
  "/home/lqtiger/.npm-global/bin/codex",
  "-C",
  "<session.workspace>",
  "exec",
  "resume",
  "--json",
  "-m",
  "<commandModelName>",
  "--skip-git-repo-check",
  "<session.id>",
  "<prompt>"
]
```

If dangerous confirmed:

```text
insert "--dangerously-bypass-approvals-and-sandbox" before prompt
```

Rules:

- Selected-session Codex control must use `exec resume`.
- Do not use plain `codex exec <prompt>` for selected sessions.
- `-C <workspace>` must appear before `exec`; local CLI help rejects `-C` after `resume`.
- Do not add dangerous flag unless `confirmDangerous=true`.
- Mode-to-permission mapping must follow `doc/security/command-permission-matrix.md`.
- Do not shell-concatenate.
- Workdir must pass `PathGuard`.
- Codex command builder unit tests must prove final argument order before enabling `POST /api/runs` for Codex.

### 8.2 OpencodeCommandBuilder

Method:

```java
CommandSpec build(StartRunRequest request, RemoteSessionDto session, ModelDto model)
```

Argument array:

```text
[
  "/home/lqtiger/.opencode/bin/opencode",
  "run",
  "--format",
  "json",
  "--model",
  "<commandModelName>",
  "--dir",
  "<session.workspace>",
  "--session",
  "<session.id>",
  "<prompt>"
]
```

If dangerous confirmed:

```text
insert "--dangerously-skip-permissions" before prompt
```

Rules:

- Do not use `--continue` for explicit selected session.
- Do not use logs or prompt history to infer session id.
- Mode-to-permission mapping must follow `doc/security/command-permission-matrix.md`.
- Workdir must pass `PathGuard`.

## 9. Runtime Design

### 9.1 ProcessLauncher

Method:

```java
ManagedProcess start(String runId, CommandSpec spec)
```

Rules:

- Use `new ProcessBuilder(spec.command())`.
- Set working directory to `spec.workdir()`.
- Merge only explicitly allowed environment variables.
- Do not use `sh -c`.

### 9.2 ProcessOutputPump

Method:

```java
void attach(String runId, ManagedProcess process)
```

Behavior:

- stdout line -> `RunEventDto(type="stdout")`
- stderr line -> `RunEventDto(type="stderr")`
- exit code 0 -> exactly one `done`
- non-zero exit -> exactly one `error`
- user stop -> exactly one `stopped`

Codex JSON output:

- If line is valid JSON, preserve it under `data.raw`.
- Do not attempt deep semantic mapping in v1.

opencode JSON output:

- If line is valid JSON, preserve it under `data.raw`.
- Do not expose hidden reasoning or secrets.

## 10. API Design

### 10.1 GET /api/health

Returns:

```json
{
  "serviceName": "Lqtigee-spark-ai",
  "appName": "Lqtigee",
  "port": 20261,
  "status": "OK",
  "timestamp": "<iso>",
  "adapters": [...]
}
```

Adapter status may include availability but must not include secrets.

### 10.2 GET /api/sessions

Implementation:

```java
return sessionService.listAllSessions();
```

Rules:

- Fails if either Codex or opencode source fails.
- Does not return partial success.

### 10.3 GET /api/codex/sessions

Implementation:

```java
return sessionService.listCodexSessions();
```

### 10.4 GET /api/opencode/sessions

Implementation:

```java
return sessionService.listOpencodeSessions();
```

### 10.5 GET /api/models

Implementation:

```java
return modelService.listModels();
```

### 10.6 POST /api/runs

Validation sequence:

1. Auth token valid.
2. Prompt non-empty.
3. Prompt length allowed.
4. Session exists.
5. Model exists.
6. Model supports selected source.
7. Workspace allowed.
8. Command mode permission allowed by `doc/security/command-permission-matrix.md`.
9. Dangerous mode confirmed if dangerous flag requested.
10. Build command.
11. Start process.
12. Return run id.

### 10.7 GET /api/runs/{id}/events

Returns SSE stream from `RunEventBus`.

### 10.8 POST /api/runs/{id}/stop

Stops process through `RunService`.

## 11. Frontend Design

Frontend never reads local files.

### 11.1 Page Order

```text
/settings
/
/sessions
/control
/runs
```

Implementation order:

1. Settings page can exist first because it only stores URL/token.
2. Overview requires `/api/health`.
3. Sessions requires `/api/sessions`.
4. Control requires `/api/sessions` and `/api/models`.
5. Runs requires `POST /api/runs` and SSE events.

### 11.2 SettingsPage

Fields:

```text
backend base URL
token
refresh seconds
```

Local storage keys:

```text
lqtigee_base_url
lqtigee_token
lqtigee_refresh_seconds
```

No server path configuration in v1.

### 11.3 SessionsPage

Data:

```typescript
RemoteSession[]
```

States:

```text
loading
error
empty-success
success
```

Empty success is allowed only if `/api/sessions` returned `[]` with 200.

### 11.4 ControlPage

Valid send conditions:

```text
session selected
model selected
model supports session.source
prompt.trim() not empty
danger confirmed when required
```

On submit:

```typescript
startRun(...)
```

On success:

```text
navigate to /runs?runId=<runId>
```

No fake run id.

### 11.5 RunsPage

If no runId:

```text
No run selected
```

If runId:

```text
open authenticated fetch SSE stream
append real events
close on done/error/stopped
```

Stop button calls:

```typescript
stopRun(runId)
```

## 12. What Future Agents Must Not Redesign

These decisions are now based on real local facts and should not be changed without new discovery evidence:

```text
Codex first session source = JSONL under ~/.codex/sessions
opencode first session source = SQLite opencode.db session table
Codex selected-session command = codex -C <workspace> exec resume --json ...
opencode command = opencode run --format json ...
models = backend config, initially gpt-5.5 for Codex and openai/Lqtigee for opencode
frontend = API-only, no local file parsing
```

If any of these appears wrong during implementation, stop and create a new discovery ticket. Do not patch around it.
