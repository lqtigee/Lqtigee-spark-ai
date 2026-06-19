# Lqtigee-spark-ai Architecture

## 1. Decoupling Rule

Every module depends on contracts, not on another module's implementation details.

Forbidden dependencies:

- Frontend -> local Codex/opencode files
- Controller -> filesystem scanners
- Controller -> ProcessBuilder
- Parser -> HTTP or frontend state
- Runtime process launcher -> UI details
- UI component -> command-line syntax

Allowed flow:

```text
Frontend UI
  -> Frontend API client
  -> HTTP API contract
  -> Controller
  -> Service
  -> Adapter interface
  -> Scanner / parser / command builder
  -> Process runtime
  -> Event bus
  -> SSE
  -> Frontend run timeline
```

## 1.1 Android Installability Boundary

The backend owns only port `20261`. External fixed IP mapping, domain mapping, and TLS termination are deployment concerns.

PWA installation on Android requires a secure browser context. The implementation must not claim Android-installable delivery for plain `http://<server-ip>:20261`; see `doc/deployment/pwa-installability.md`.

## 2. Backend Packages

```text
com.lqtigee.sparkai
  LqtigeeSparkAiApplication

com.lqtigee.sparkai.config
  RemoteProperties
  SecurityProperties
  CorsProperties
  DatabaseProperties

com.lqtigee.sparkai.web
  HealthController
  SessionController
  ModelController
  RunController
  SettingsController

com.lqtigee.sparkai.service
  HealthService
  SessionService
  ModelService
  RunService
  SettingsService

com.lqtigee.sparkai.adapter
  AgentAdapter
  CodexAdapter
  OpencodeAdapter

com.lqtigee.sparkai.codex
  CodexProbeService
  CodexFileScanner
  CodexFormatDetector
  CodexJsonParser
  CodexJsonlParser
  CodexCommandBuilder

com.lqtigee.sparkai.opencode
  OpencodeProbeService
  OpencodeFileScanner
  OpencodeFormatDetector
  OpencodeSqliteSessionReader
  OpencodeModelJsonReader
  OpencodeCommandBuilder

com.lqtigee.sparkai.runtime
  CommandSpec
  ManagedProcess
  ProcessLauncher
  ProcessOutputPump
  RunRegistry
  RunEventBus

com.lqtigee.sparkai.persistence
  RunRecordRepository
  AuditEventRepository
  SettingsRepository

com.lqtigee.sparkai.dto
  ApiErrorDto
  HealthDto
  AdapterHealthDto
  RemoteSessionDto
  SessionDetailDto
  ModelDto
  StartRunRequest
  StartRunResponse
  RunDto
  RunEventDto

com.lqtigee.sparkai.error
  ApiException
  ErrorCode
  GlobalExceptionHandler

com.lqtigee.sparkai.util
  PathGuard
  TimeProvider
  JsonReader
```

## 2.1 Database Boundary

Lqtigee-owned persistent data uses PostgreSQL. This includes service-owned data such as run records, audit events, settings, and future user preferences.

PostgreSQL is not the source of truth for external agent sessions in v1:

- Codex sessions are discovered from verified Codex session files.
- opencode sessions are discovered from `/home/lqtiger/.local/share/opencode/opencode.db` using read-only SQLite access.

The backend may later persist derived metadata into PostgreSQL only when a dedicated ticket defines the schema and invalidation rules. It must not return PostgreSQL-derived sessions as if they were live Codex/opencode sessions unless a real source read succeeded.

## 3. Frontend Structure

```text
src/
  main.tsx
  app/App.tsx
  app/routes.tsx
  api/httpClient.ts
  api/remoteApi.ts
  types/api.ts
  state/useConnectionState.ts
  state/useSessionsState.ts
  state/useModelsState.ts
  state/useRunEvents.ts
  components/AppShell.tsx
  components/BottomNav.tsx
  components/SideNav.tsx
  components/StatusBadge.tsx
  components/ErrorPanel.tsx
  components/LoadingBlock.tsx
  components/SessionCard.tsx
  components/SessionDetail.tsx
  components/ModelSelect.tsx
  components/RunTimeline.tsx
  components/PromptComposer.tsx
  pages/OverviewPage.tsx
  pages/SessionsPage.tsx
  pages/ControlPage.tsx
  pages/RunsPage.tsx
  pages/SettingsPage.tsx
  styles/global.css
  styles/layout.css
```

## 4. Backend Contract DTOs

### 4.1 ApiErrorDto

Fields:

```java
String code;
String message;
String detail;
Instant timestamp;
String path;
```

Rules:

- `code` must be one stable `ErrorCode`.
- `message` is user-facing but short.
- `detail` may contain implementation detail without secrets.

### 4.2 RemoteSessionDto

Fields:

```java
String id;
AgentSource source;
String title;
String workspace;
String model;
SessionStatus status;
Instant updatedAt;
String lastMessage;
String rawFile;
Map<String, Object> metadata;
```

Required fields:

- id
- source
- workspace
- model
- updatedAt
- rawFile

If any required field cannot be extracted from a real parsed file, parser must fail.

### 4.3 CommandSpec

Fields:

```java
List<String> command;
Path workdir;
Map<String, String> environment;
AgentSource source;
String sessionId;
String modelId;
```

Rules:

- `command.get(0)` is executable path or command.
- No shell wrapper unless explicitly approved in a later ticket.
- `workdir` must pass `PathGuard`.

## 5. Frontend Contract Types

Frontend `types/api.ts` must mirror backend DTOs.

No frontend type may include:

- `.codex`
- `.opencode`
- jsonl parser fields
- command syntax fields
- process id, unless backend exposes it intentionally

## 6. API Routes

```text
GET  /api/health
GET  /api/sessions
GET  /api/codex/sessions
GET  /api/opencode/sessions
GET  /api/sessions/{source}/{id}
GET  /api/models
POST /api/runs
GET  /api/runs
GET  /api/runs/{id}
GET  /api/runs/{id}/events
POST /api/runs/{id}/stop
GET  /api/settings/client
```

No route may return mock data.

## 7. Session Discovery Pipeline

Each adapter must follow:

```text
probe -> scan files -> detect formats -> parse -> validate required fields -> convert DTO
```

No step may silently skip required failures.

## 8. Command Runtime Pipeline

```text
validate request
validate session
validate model
validate workspace
build command spec
create run
start process
attach output pump
emit events
record completion
```

## 9. UI Runtime Pipeline

```text
load settings from localStorage
check health
load sessions
load models
select session
select model
submit run
subscribe SSE
append real events
stop run if requested
```
