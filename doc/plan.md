# Lqtigee-spark-ai AI Development Plan

This document is the execution plan for AI agents. Each ticket is intentionally small. Complete exactly one ticket at a time.

For the actual implementation queue, use `doc/micro-tickets.md`. This file remains the phase map only. If this file and `doc/micro-tickets.md` differ, `doc/micro-tickets.md`, `PROJECT_SPEC.md`, and `doc/implementation-design.md` win.

Current confirmed source decisions:

- Codex sessions: `~/.codex/sessions/**/*.jsonl`.
- opencode sessions: `/home/lqtiger/.local/share/opencode/opencode.db`, table `session`.
- Codex selected-session command: `codex -C <workspace> exec resume --json ... <sessionId> <prompt>`.
- opencode selected-session command: `opencode run --format json ... --session <sessionId> <prompt>`.

Runtime implementation is blocked until selected-session command builders are covered by static CLI/session evidence and unit tests. Do not run live Codex or opencode commands as gates.

## Ticket Rules

Each ticket must be executed exactly as written.

- Do not modify files outside `Allowed files`.
- Do not create files outside `Allowed new files`.
- Do not add mock data.
- Do not add fallback success behavior.
- Do not continue if verification fails.
- If blocked, output the required `BLOCKED` format from `requirements.md`.

## Phase P0: Discovery and Project Specification

### P0-001 Create Immutable Project Spec

Goal: Create the project constants document.

Allowed new files:

- `PROJECT_SPEC.md`

Allowed files:

- `PROJECT_SPEC.md`

Implementation:

1. Write project name `Lqtigee-spark-ai`.
2. Write app name `Lqtigee`.
3. Write backend port `20261`.
4. Write backend framework `Java 21 + Spring Boot 3`.
5. Write mobile mode `PWA first`.
6. Write no-mock/no-fallback rule.

Verification:

```bash
test -f PROJECT_SPEC.md
rg "Lqtigee-spark-ai|Lqtigee|20261|PWA|No mock" PROJECT_SPEC.md
```

Done when: verification passes.

### P0-002 Verify Port 20261

Goal: Prove whether port `20261` is available.

Allowed new files:

- `doc/discovery/port-20261.md`

Implementation:

1. Run a read-only port check.
2. Record command and output.
3. If occupied, mark BLOCKED and do not continue.

Verification:

```bash
test -f doc/discovery/port-20261.md
rg "20261" doc/discovery/port-20261.md
```

Done when: document states available or blocked.

### P0-003 Verify Codex Runtime

Goal: Record real Codex command and state paths.

Allowed new files:

- `doc/discovery/codex-command.md`
- `doc/discovery/codex-version.md`
- `doc/discovery/codex-help.md`
- `doc/discovery/codex-home.md`

Implementation:

1. Locate `codex` with `command -v codex`.
2. Run `codex --version` if command exists.
3. Check configured candidate path `~/.codex`.
4. List top-level files and directories under `~/.codex`.
5. Do not parse sessions yet.

Verification:

```bash
test -f doc/discovery/codex-command.md
test -f doc/discovery/codex-version.md
test -f doc/discovery/codex-help.md
test -f doc/discovery/codex-home.md
rg "codex|\\.codex" doc/discovery/codex-*.md
```

Done when: real command output is recorded.

### P0-004 Verify opencode Runtime

Goal: Record real opencode command and state paths.

Allowed new files:

- `doc/discovery/opencode-command.md`
- `doc/discovery/opencode-version.md`
- `doc/discovery/opencode-help.md`
- `doc/discovery/opencode-roots.md`

Implementation:

1. Locate opencode command candidates:
   - `command -v opencode`
   - `~/.opencode/bin/opencode`
2. Run version command only if available.
3. Check:
   - `~/.config/opencode`
   - `~/.local/share/opencode`
   - `~/.local/state/opencode`
4. Do not parse sessions yet.

Verification:

```bash
test -f doc/discovery/opencode-command.md
test -f doc/discovery/opencode-version.md
test -f doc/discovery/opencode-help.md
test -f doc/discovery/opencode-roots.md
rg "opencode|\\.config/opencode|\\.local/share/opencode|\\.local/state/opencode" doc/discovery/opencode-*.md
```

Done when: real command output is recorded.

### P0-005 Capture Session Format Samples

Goal: Collect real sample file descriptions for parser design without copying secrets.

Allowed new files:

- `doc/discovery/session-format-samples.md`

Implementation:

1. Find candidate Codex session files.
2. Find candidate opencode session files.
3. For each selected file, record:
   - absolute path
   - file size
   - modified time
   - first non-secret structural line with values redacted if needed
   - whether Codex source is JSONL
   - whether opencode source is SQLite
4. Do not write parser code.

Verification:

```bash
test -f doc/discovery/session-format-samples.md
rg "Codex|opencode|JSONL|SQLite" doc/discovery/session-format-samples.md
```

Done when: both Codex and opencode sample sections exist.

## Phase B1: Backend Skeleton

### B1-001 Create Spring Boot Skeleton

Goal: Create minimal Java service.

Allowed new files:

- `pom.xml`
- `src/main/java/com/lqtigee/sparkai/LqtigeeSparkAiApplication.java`
- `src/main/resources/application.yml`
- `src/test/java/com/lqtigee/sparkai/LqtigeeSparkAiApplicationTests.java`

Implementation:

1. Use Java 21.
2. Use Spring Boot 3.
3. Configure server port `20261`.
4. Add only required dependencies:
   - spring-boot-starter-web
   - spring-boot-starter-validation
   - spring-boot-starter-actuator
   - spring-boot-starter-test

Verification:

```bash
mvn test
mvn -q spring-boot:run
```

Done when: app starts on port `20261`.

### B1-002 Add Health Endpoint With Real Static Service Info

Goal: Expose service identity without adapter data.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/web/HealthController.java`
- `src/main/java/com/lqtigee/sparkai/dto/HealthDto.java`

Implementation:

1. Add `GET /api/health`.
2. Return:
   - serviceName: `Lqtigee-spark-ai`
   - appName: `Lqtigee`
   - port: `20261`
   - status: `STARTING`
3. Do not add fake adapter status.

Verification:

```bash
curl -s http://127.0.0.1:20261/api/health
```

Done when: response contains real service constants.

### B1-003 Add Error Model

Goal: Create stable API error output.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/error/ErrorCode.java`
- `src/main/java/com/lqtigee/sparkai/error/ApiException.java`
- `src/main/java/com/lqtigee/sparkai/error/GlobalExceptionHandler.java`
- `src/main/java/com/lqtigee/sparkai/dto/ApiErrorDto.java`

Implementation:

1. Define error codes from `requirements.md`.
2. Convert `ApiException` to JSON.
3. Convert validation errors to JSON.
4. Do not expose stack traces.

Verification:

```bash
curl -s http://127.0.0.1:20261/api/not-found
```

Done when: response is JSON error, not HTML.

## Phase B2: Security

### B2-001 Add Token Properties

Goal: Configure API token without hardcoding.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/config/SecurityProperties.java`

Allowed files:

- `src/main/resources/application.yml`

Implementation:

1. Add `lqtigee.security.api-token`.
2. Bind to `SecurityProperties`.
3. Empty token must fail application startup.

Verification:

```bash
mvn test
```

Done when: test proves empty token fails.

### B2-002 Add Bearer Token Filter

Goal: Protect API endpoints.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/security/BearerTokenFilter.java`
- `src/main/java/com/lqtigee/sparkai/config/SecurityConfig.java`

Implementation:

1. `/api/health` is public.
2. All other `/api/**` routes require bearer token.
3. Missing token returns `AUTH_TOKEN_MISSING`.
4. Wrong token returns `AUTH_TOKEN_INVALID`.

Verification:

```bash
curl -i http://127.0.0.1:20261/api/models
curl -i -H "Authorization: Bearer wrong" http://127.0.0.1:20261/api/models
```

Done when: both unauthorized calls return JSON 401.

## Phase C3: Contract DTOs

### C3-001 Define Agent and Session Enums

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/AgentSource.java`
- `src/main/java/com/lqtigee/sparkai/dto/SessionStatus.java`
- `src/main/java/com/lqtigee/sparkai/dto/RunStatus.java`
- `src/main/java/com/lqtigee/sparkai/dto/CommandMode.java`

Implementation:

1. `AgentSource`: `CODEX`, `OPENCODE`
2. `SessionStatus`: `ACTIVE`, `IDLE`, `RUNNING`, `FAILED`, `UNKNOWN`
3. `RunStatus`: `CREATED`, `RUNNING`, `EXITED`, `FAILED`, `STOPPED`
4. `CommandMode`: `ASK`, `EDIT`, `REVIEW`, `SHELL`

Verification:

```bash
mvn test
```

### C3-002 Define RemoteSessionDto

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/RemoteSessionDto.java`

Implementation:

Required fields:

- id
- source
- title
- workspace
- model
- status
- updatedAt
- lastMessage
- rawFile

Verification:

```bash
mvn test
```

## Phase D4: Real Discovery Adapters

### D4-001 Define AgentAdapter Interface

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/adapter/AgentAdapter.java`
- `src/main/java/com/lqtigee/sparkai/dto/AdapterHealthDto.java`

Methods:

```java
AgentSource source();
AdapterHealthDto probe();
List<RemoteSessionDto> discoverSessions();
CommandSpec buildCommand(StartRunRequest request, RemoteSessionDto session);
```

Rules:

- Interface only.
- No implementation.

Verification:

```bash
mvn test
```

### D4-002 Implement Codex File Scanner

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFileScanner.java`
- `src/test/java/com/lqtigee/sparkai/codex/CodexFileScannerTest.java`

Method:

```java
List<Path> scan(Path codexHome)
```

Rules:

- Directory missing throws `CODEX_HOME_NOT_FOUND`.
- Scan only `codexHome/sessions`.
- Return only `sessions/**/*.jsonl`.
- Return absolute normalized paths.
- Do not parse files.
- Do not scan config, logs, shell snapshots, or SQLite files.

Verification:

```bash
mvn test -Dtest=CodexFileScannerTest
```

### D4-003 Implement Codex JSONL Parser Preparation

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFormatDetector.java`
- `src/main/java/com/lqtigee/sparkai/dto/SessionFileFormat.java`
- `src/test/java/com/lqtigee/sparkai/codex/CodexFormatDetectorTest.java`

Method:

```java
SessionFileFormat detect(Path file)
```

Allowed results:

- `JSONL`
- `UNKNOWN`

Rules:

- Do not parse business fields.
- First parser path accepts only confirmed JSONL session files.
- Unknown stays unknown.

Verification:

```bash
mvn test -Dtest=CodexFormatDetectorTest
```

### D4-004 Implement opencode File Scanner

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeFileScanner.java`
- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeFileScannerTest.java`

Method:

```java
List<Path> scan(List<Path> roots)
```

Rules:

- Missing all roots throws `OPENCODE_CONFIG_NOT_FOUND`.
- Return only `opencode.db`.
- Do not scan logs or prompt history as session source.
- Do not parse SQLite rows in the scanner.

Verification:

```bash
mvn test -Dtest=OpencodeFileScannerTest
```

## Phase M5: Models

### M5-001 Add Model DTO and Configuration

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/ModelDto.java`
- `src/main/java/com/lqtigee/sparkai/config/ModelProperties.java`
- `src/main/java/com/lqtigee/sparkai/service/ModelService.java`
- `src/main/java/com/lqtigee/sparkai/web/ModelController.java`

Allowed files:

- `src/main/resources/application.yml`

Rules:

- Models come only from config.
- Empty model list fails startup.
- Disabled models are returned with `enabled=false`.

Verification:

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://127.0.0.1:20261/api/models
```

## Phase F6: Frontend Foundation

### F6-001 Create Frontend Skeleton

Allowed new files:

- `frontend/package.json`
- `frontend/index.html`
- `frontend/src/main.tsx`
- `frontend/src/app/App.tsx`
- `frontend/src/styles/global.css`

Rules:

- App renders only shell and connection error/unknown state.
- No fake sessions.
- No fake model list.

Verification:

```bash
cd frontend && npm install && npm run build
```

### F6-002 Implement Frontend API Types

Allowed new files:

- `frontend/src/types/api.ts`

Rules:

- Mirror backend DTO fields.
- Do not add frontend-only business fields.

Verification:

```bash
cd frontend && npm run build
```

### F6-003 Implement HTTP Client

Allowed new files:

- `frontend/src/api/httpClient.ts`

Method:

```ts
requestJson<T>(path: string, init?: RequestInit): Promise<T>
```

Rules:

- Read base URL from localStorage key `lqtigee_base_url`.
- Read token from localStorage key `lqtigee_token`.
- Missing token throws client error.
- Non-2xx throws parsed API error.
- No fallback response.

Verification:

```bash
cd frontend && npm run build
```

### F6-004 Implement Remote API Client

Allowed new files:

- `frontend/src/api/remoteApi.ts`

Methods:

```ts
getHealth()
listSessions()
listModels()
startRun(request)
stopRun(runId)
openRunEvents(runId, handlers)
```

Rules:

- No mock data.
- SSE uses authenticated `fetch` streaming because browser `EventSource` cannot send Authorization headers.
- SSE stream, parse, HTTP, and abort errors surface to caller.

Verification:

```bash
cd frontend && npm run build
```

## Phase U7: Frontend UI Pages

### U7-001 AppShell and Navigation

Allowed new files:

- `frontend/src/components/AppShell.tsx`
- `frontend/src/components/BottomNav.tsx`
- `frontend/src/components/SideNav.tsx`
- `frontend/src/app/routes.tsx`

Rules:

- Routes exactly:
  - `/`
  - `/sessions`
  - `/control`
  - `/runs`
  - `/settings`
- Mobile bottom nav.
- Desktop side nav.

Verification:

```bash
cd frontend && npm run build
```

### U7-002 Overview Page

Allowed new files:

- `frontend/src/pages/OverviewPage.tsx`
- `frontend/src/state/useConnectionState.ts`
- `frontend/src/components/ErrorPanel.tsx`
- `frontend/src/components/StatusBadge.tsx`

Rules:

- Calls real `/api/health`.
- Displays loading, connected, unauthorized, failed.
- No fake adapter cards.

Verification:

```bash
cd frontend && npm run build
```

### U7-003 Sessions Page

Allowed new files:

- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/state/useSessionsState.ts`
- `frontend/src/components/SessionCard.tsx`
- `frontend/src/components/SessionDetail.tsx`

Rules:

- Calls real `/api/sessions`.
- Error keeps previous data but shows error.
- Empty state only from successful empty response.
- No hardcoded sessions.

Verification:

```bash
cd frontend && npm run build
```

### U7-004 Control Page

Allowed new files:

- `frontend/src/pages/ControlPage.tsx`
- `frontend/src/state/useModelsState.ts`
- `frontend/src/components/ModelSelect.tsx`
- `frontend/src/components/PromptComposer.tsx`

Rules:

- Loads real sessions and models.
- Send disabled until valid.
- Sends real `POST /api/runs`.
- Navigates to `/runs?runId=<id>`.

Verification:

```bash
cd frontend && npm run build
```

### U7-005 Runs Page

Allowed new files:

- `frontend/src/pages/RunsPage.tsx`
- `frontend/src/state/useRunEvents.ts`
- `frontend/src/components/RunTimeline.tsx`

Rules:

- Reads `runId` from URL.
- Opens real SSE.
- Appends only real events.
- Stop calls real API.

Verification:

```bash
cd frontend && npm run build
```

### U7-006 Settings Page

Allowed new files:

- `frontend/src/pages/SettingsPage.tsx`

Rules:

- Edits only localStorage:
  - `lqtigee_base_url`
  - `lqtigee_token`
  - `lqtigee_refresh_seconds`
- Does not edit backend scanner paths.

Verification:

```bash
cd frontend && npm run build
```

## Phase P8: Android PWA

### P8-001 Add PWA Manifest

Allowed new files:

- `frontend/public/manifest.webmanifest`
- `frontend/public/icons/icon-192.png`
- `frontend/public/icons/icon-512.png`

Rules:

- App name `Lqtigee`.
- Display `standalone`.
- Start URL `/`.

Verification:

```bash
cd frontend && npm run build
```

### P8-002 Add Service Worker

Allowed new files:

- `frontend/public/sw.js`

Rules:

- Cache static shell only.
- Do not cache API responses.
- Do not hide backend failures.

Verification:

```bash
cd frontend && npm run build
```

## Phase E9: End-to-End

### E9-001 Backend Health E2E

Goal: Prove service starts and health works on port `20261`.

Verification:

```bash
mvn test
mvn -q spring-boot:run
curl -s http://127.0.0.1:20261/api/health
```

### E9-002 Sessions E2E

Goal: Prove sessions are real or typed failure.

Verification:

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://127.0.0.1:20261/api/sessions
```

Response must not be mock.

### E9-003 Android UI E2E

Goal: Prove Android can load and install `Lqtigee`.

Verification:

1. Start backend on `20261`.
2. Open mapped URL on Android Chrome.
3. Confirm app name `Lqtigee`.
4. Confirm install prompt or install menu is available.
5. Confirm no horizontal scroll at 360px width.
