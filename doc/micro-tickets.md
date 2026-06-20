# Lqtigee-spark-ai Micro Tickets

Project: `Lqtigee-spark-ai`  
App name: `Lqtigee`  
Backend port: `20261`

This is the smallest executable backlog. Each ticket is a micro function point. An AI must complete exactly one ticket at a time.

## 0. Micro Ticket Rules

Every ticket must obey:

1. One ticket implements one method, one DTO, one endpoint, one component, or one test.
2. No ticket may implement both backend and frontend behavior.
3. No ticket may implement scanner and parser together.
4. No ticket may implement parser and API endpoint together.
5. No ticket may implement command builder and process launcher together.
6. No ticket may implement UI page and API client together.
7. No mock data.
8. No fallback success.
9. No hidden assumptions.
10. If a real fact is missing, stop with `BLOCKED`.

Every ticket must end with:

```text
Changed files:
Verification command:
Verification result:
```

Bug fixes and future features must also be converted into micro tickets before implementation. Do not directly repair or extend the project from a broad request. Use `AGENTS.md` for the mandatory bug-fix and feature-addition protocols.

All API-facing tickets must preserve `doc/contracts/backend-api-contract.md`. If a response shape must change, create a contract-update micro ticket first. Test-only response examples must preserve `doc/contracts/backend-response-fixtures.md`. All tickets must satisfy `doc/quality/definition-of-done.md` and `doc/quality/rework-prevention.md`.

## 1. Risk Gate Micro Tickets

### CONTRACT-M001 Add API Contract Enforcement To Plan

Goal: Prevent frontend/backend drift.

Allowed files:

- `doc/plan.md`
- `doc/micro-tickets.md`

Implementation:

1. State API response shape changes require updating `doc/contracts/backend-api-contract.md` first.
2. State frontend type changes must reference the contract.
3. State endpoint tests must compare required fields from the contract.

Verification:

```bash
rg "backend-api-contract|frontend type changes|endpoint tests" doc/plan.md doc/micro-tickets.md
```

### QA-M001 Add Definition Of Done Enforcement To Plan

Goal: Prevent "almost done" tickets.

Allowed files:

- `doc/plan.md`
- `doc/micro-tickets.md`

Implementation:

1. Reference `doc/quality/definition-of-done.md`.
2. State every ticket must satisfy ticket-level DoD.

Verification:

```bash
rg "definition-of-done|Ticket-Level Done|DoD" doc/plan.md doc/micro-tickets.md
```

### QA-M002 Add E2E Matrix Release Gate To Plan

Goal: Prevent incomplete delivery.

Allowed files:

- `doc/plan.md`
- `doc/micro-tickets.md`

Implementation:

1. Reference `doc/quality/e2e-matrix.md`.
2. State release cannot be claimed until matrix passes.

Verification:

```bash
rg "e2e-matrix|Release Gate|matrix passes" doc/plan.md doc/micro-tickets.md
```

### CONTRACT-M002 Add Backend Response Fixture Examples

Goal: Prevent frontend and backend tests from inventing response shapes.

Allowed new files:

- `doc/contracts/backend-response-fixtures.md`

Implementation:

1. Add one JSON fixture for each public response:
   - `ApiErrorDto`
   - health response
   - sessions response
   - models response
   - start run response
   - run event
   - stop run response
2. Mark fixtures test-only.
3. State fixtures are not runtime mock data.

Verification:

```bash
test -f doc/contracts/backend-response-fixtures.md
rg "ApiErrorDto|SessionsResponse|ModelsResponse|test-only|not runtime mock" doc/contracts/backend-response-fixtures.md
```

### CONTRACT-M003 Add Backend Contract Test Ticket

Goal: Ensure implementation tests use the contract fixture shape.

Allowed files:

- `doc/micro-tickets.md`

Implementation:

1. Add endpoint test tickets that assert required fields from `doc/contracts/backend-response-fixtures.md`.
2. Each endpoint test must be separate.
3. No endpoint test may use invented response fields.

Verification:

```bash
rg "backend-response-fixtures|required fields|invented response fields" doc/micro-tickets.md
```

### QA-M003 Add Release Checklist

Goal: Make final delivery binary: pass checklist or do not claim release.

Allowed new files:

- `doc/quality/release-checklist.md`

Implementation:

1. Add documentation gate.
2. Add backend gate.
3. Add session gate.
4. Add runtime gate.
5. Add frontend gate.
6. Add PWA/Android gate.
7. Add blockers.

Verification:

```bash
test -f doc/quality/release-checklist.md
rg "Documentation Gate|Runtime Gate|PWA / Android Gate|Blockers" doc/quality/release-checklist.md
```

### QA-M004 Add 95 Percent Readiness Gate

Goal: Avoid false confidence claims.

Allowed new files:

- `doc/quality/readiness-95.md`

Implementation:

1. Define pass/fail readiness gates.
2. State current score range.
3. State what blocks a 95% claim.
4. Include infinite-loop prediction after the current pass.

Verification:

```bash
test -f doc/quality/readiness-95.md
rg "95|Current estimated readiness|What Blocks A 95|Infinite Loop" doc/quality/readiness-95.md
```

### QA-M005 Add Rework Prevention Protocol

Goal: Stop future agents from fixing the wrong layer.

Allowed new files:

- `doc/quality/rework-prevention.md`

Implementation:

1. Add layer ownership table.
2. Add two-failure rule.
3. Add runtime static evidence failure rule.
4. Add parser failure rule.
5. Add frontend failure rule.

Verification:

```bash
test -f doc/quality/rework-prevention.md
rg "Layer Ownership|Two-Failure Rule|Runtime Static Evidence Failure Rule|Parser Failure Rule" doc/quality/rework-prevention.md
```

### STATIC-M001 Verify Codex Resume Command Static Evidence

Goal: Prove selected-session Codex command design without running Codex.

Allowed new files:

- `doc/discovery/codex-resume-static-evidence.md`

Implementation:

1. Read `doc/discovery/codex-help.md`.
2. Read `doc/discovery/cli-command-syntax.md`.
3. Read `doc/discovery/codex-sample-1.md`.
4. Confirm existing session evidence contains:
   - session id
   - workspace
   - model
5. Confirm CLI help evidence supports:
   - `codex exec resume [SESSION_ID] [PROMPT]`
   - `--json`
   - `-m`
   - `-C` before `exec`
6. Record approved argument shape:

```text
codex -C <workspace> -s read-only exec resume --json -m <model> --skip-git-repo-check <sessionId> <prompt>
```

7. Do not execute Codex.
8. Do not record transcript text or secrets.
9. If any static evidence is missing, mark `BLOCKED`.

Verification:

```bash
test -f doc/discovery/codex-resume-static-evidence.md
rg "codex .*exec resume|static evidence|Do not execute|session id|workspace|model|BLOCKED|PASS" doc/discovery/codex-resume-static-evidence.md
```

### STATIC-M002 Verify opencode Session Command Static Evidence

Goal: Prove selected-session opencode command design without running opencode.

Allowed new files:

- `doc/discovery/opencode-session-static-evidence.md`

Implementation:

1. Read `doc/discovery/opencode-help.md`.
2. Read `doc/discovery/cli-command-syntax.md`.
3. Read `doc/discovery/opencode-sample-1.md`.
4. Confirm existing SQLite session evidence contains:
   - session id
   - directory/workspace
   - model JSON
5. Confirm CLI help evidence supports:
   - `opencode run`
   - `--format json`
   - `--model`
   - `--dir`
   - `--session`
6. Record approved argument shape:

```text
opencode run --format json --model <provider/model> --dir <workspace> --session <sessionId> <prompt>
```

7. Do not execute opencode.
8. Do not record transcript text or secrets.
9. If any static evidence is missing, mark `BLOCKED`.

Verification:

```bash
test -f doc/discovery/opencode-session-static-evidence.md
rg "opencode run|--session|static evidence|Do not execute|session id|workspace|model|BLOCKED|PASS" doc/discovery/opencode-session-static-evidence.md
```

### SQLITE-M001 Add opencode SQLite Schema Guard Ticket

Goal: Detect opencode database schema drift before query mapping breaks silently.

Allowed files:

- `doc/micro-tickets.md`

Implementation:

1. Add parser ticket requiring `PRAGMA table_info(session)` validation.
2. Required columns:
   - `id`
   - `directory`
   - `title`
   - `model`
   - `time_updated`
   - `time_archived`
   - `path`
   - `agent`
3. Missing column must throw `OPENCODE_SESSION_SCHEMA_MISMATCH`.

Verification:

```bash
rg "PRAGMA table_info\\(session\\)|OPENCODE_SESSION_SCHEMA_MISMATCH" doc/micro-tickets.md
```

### SEC-M001 Add Secret Leak Audit Ticket

Goal: Prevent tokens, auth files, prompts, and transcripts from appearing in docs or API responses.

Allowed files:

- `doc/micro-tickets.md`

Implementation:

1. Add audit ticket for generated docs and API response code.
2. Search for:
   - auth token values
   - `auth.json` content
   - transcript text
   - prompt text
   - environment secret values
3. Any match must be marked `BLOCKED`.

Verification:

```bash
rg "secret leak|auth\\.json|transcript text|environment secret" doc/micro-tickets.md
```

### SEC-M002 Add Command Permission Matrix

Goal: Prevent runtime from silently escalating CLI permissions.

Allowed new files:

- `doc/security/command-permission-matrix.md`

Implementation:

1. Define modes `ASK`, `REVIEW`, `EDIT`, and `SHELL`.
2. Map Codex modes to sandbox or dangerous args.
3. Map opencode modes to safe or dangerous args.
4. State `SHELL` without `confirmDangerous=true` returns `DANGER_CONFIRM_REQUIRED`.
5. State command builder tests must cover all modes.

Verification:

```bash
test -f doc/security/command-permission-matrix.md
rg "ASK|REVIEW|EDIT|SHELL|DANGER_CONFIRM_REQUIRED|workspace-write|dangerously" doc/security/command-permission-matrix.md
```

### DEPLOY-M001 Add PWA Installability Deployment Requirement

Goal: Prevent claiming Android installability over an insecure origin.

Allowed new files:

- `doc/deployment/pwa-installability.md`

Implementation:

1. State backend port remains `20261`.
2. State PWA installability requires secure browser context.
3. State plain `http://<server-ip>:20261` may load but is not enough for Android PWA installability.
4. List accepted deployment shapes:
   - reverse proxy TLS termination
   - direct Java HTTPS with trusted certificate
   - native wrapper later if HTTPS is not possible
5. Add final Android installability checks.

Verification:

```bash
test -f doc/deployment/pwa-installability.md
rg "20261|secure context|plain HTTP|Android|install" doc/deployment/pwa-installability.md
```

### RISK-001 Add Changed-Files Audit Rule

Goal: Ensure every future AI task reports changed files.

Allowed files:

- `doc/plan.md`
- `doc/micro-tickets.md`

Implementation:

1. Add rule: before a ticket, record current file list or git status.
2. Add rule: after a ticket, list changed files.
3. Add rule: if changed files exceed allowed files, task fails.

Verification:

```bash
rg "changed files" doc/plan.md doc/micro-tickets.md
```

### RISK-002 Add Discovery Gate To Plan

Goal: Prevent parser implementation before real samples.

Allowed files:

- `doc/plan.md`

Implementation:

1. Add "Discovery Gate" before Phase D4.
2. State D4 cannot start without:
   - `doc/discovery/codex-command.md`
   - `doc/discovery/codex-version.md`
   - `doc/discovery/codex-help.md`
   - `doc/discovery/codex-home.md`
   - `doc/discovery/opencode-command.md`
   - `doc/discovery/opencode-version.md`
   - `doc/discovery/opencode-help.md`
   - `doc/discovery/opencode-roots.md`
   - `doc/discovery/session-format-samples.md`
3. State missing files mean `BLOCKED`.

Verification:

```bash
rg "Discovery Gate|D4 cannot start|session-format-samples" doc/plan.md
```

### RISK-003 Add CLI Syntax Discovery Ticket

Goal: Prevent command builder guessing.

Allowed files:

- `doc/plan.md`

Implementation:

1. Add ticket `P0-006 Verify CLI Command Syntax`.
2. Required output: `doc/discovery/cli-command-syntax.md`.
3. Required content:
   - exact `codex --help` command evidence
   - exact `opencode --help` command evidence
   - static selected-session command evidence, or explicit reason it is blocked

Verification:

```bash
rg "P0-006 Verify CLI Command Syntax|cli-command-syntax" doc/plan.md
```

### RISK-004 Add Model Catalog Discovery Ticket

Goal: Prevent hardcoded model drift.

Allowed files:

- `doc/plan.md`

Implementation:

1. Add ticket `P0-007 Record Model Catalog`.
2. Required output: `doc/discovery/model-catalog.md`.
3. Required fields:
   - model id
   - label
   - supported source
   - command model name
   - enabled flag

Verification:

```bash
rg "P0-007 Record Model Catalog|model-catalog" doc/plan.md
```

### RISK-005 Add Parser Sample Fixture Gate

Goal: Ensure parser success tests use sanitized real samples.

Allowed files:

- `doc/plan.md`

Implementation:

1. Add rule before parser tickets.
2. Parser success tests must use `src/test/resources/samples/`.
3. Invented parser success fixtures are forbidden.

Verification:

```bash
rg "src/test/resources/samples|Invented parser success fixtures are forbidden" doc/plan.md
```

### RISK-006 Add Error Contract Test Requirements

Goal: Ensure all API errors use `ApiErrorDto`.

Allowed files:

- `doc/plan.md`

Implementation:

1. Add tests required for:
   - missing token
   - wrong token
   - session not found
   - model unsupported
   - run not found
2. State all must return JSON, never HTML.

Verification:

```bash
rg "missing token|wrong token|never HTML" doc/plan.md
```

### RISK-007 Add Non-Zero Process Exit Requirement

Goal: Prevent failed CLI command from appearing successful.

Allowed files:

- `doc/plan.md`

Implementation:

1. Add runtime test requirement.
2. Non-zero exit must set run state `FAILED`.
3. Non-zero exit must emit SSE `error`.

Verification:

```bash
rg "Non-zero exit|FAILED|SSE `error`" doc/plan.md
```

### RISK-008 Add Service Worker API Exclusion Requirement

Goal: Prevent PWA cache hiding backend failures.

Allowed files:

- `doc/plan.md`

Implementation:

1. Add PWA rule: service worker must not cache `/api/**`.
2. Add verification command/search.

Verification:

```bash
rg "must not cache `/api/\\*\\*`|service worker" doc/plan.md
```

## 2. Project Spec and Discovery Micro Tickets

### P0-M001 Create PROJECT_SPEC.md

Goal: Create immutable project constants.

Allowed new files:

- `PROJECT_SPEC.md`

Method/function point:

- No code method. Documentation-only.

Implementation:

1. Write project name `Lqtigee-spark-ai`.
2. Write app name `Lqtigee`.
3. Write backend port `20261`.
4. Write package root `com.lqtigee.sparkai`.
5. Write mobile delivery `PWA first`.
6. Write no mock / no fallback / BLOCKED rule.

Verification:

```bash
test -f PROJECT_SPEC.md
rg "Lqtigee-spark-ai|Lqtigee|20261|com.lqtigee.sparkai|PWA first|BLOCKED" PROJECT_SPEC.md
```

### P0-M002 Create Discovery Directory

Goal: Prepare discovery artifact folder.

Allowed new directories:

- `doc/discovery/`

Allowed files:

- none

Implementation:

1. Create `doc/discovery`.
2. Do not create runtime docs yet.

Verification:

```bash
test -d doc/discovery
```

### P0-M003 Check Port 20261 With ss

Goal: Record whether port `20261` is occupied.

Allowed new files:

- `doc/discovery/port-20261.md`

Implementation:

1. Run `ss -ltnp`.
2. Search for `:20261`.
3. Record exact command output.
4. If occupied, write `BLOCKED` in the document.

Verification:

```bash
test -f doc/discovery/port-20261.md
rg "20261|ss -ltnp|available|BLOCKED" doc/discovery/port-20261.md
```

### P0-M004 Locate Java Runtime

Goal: Record Java version.

Allowed new files:

- `doc/discovery/java-runtime.md`

Implementation:

1. Run `java -version`.
2. Run `javac -version`.
3. Record exact output.
4. If Java major version is not 21 or compatible, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/java-runtime.md
rg "java|javac|version|21|BLOCKED" doc/discovery/java-runtime.md
```

### P0-M005 Locate Maven Runtime

Goal: Record Maven version.

Allowed new files:

- `doc/discovery/maven-runtime.md`

Implementation:

1. Run `mvn -version`.
2. Record exact output.
3. If missing, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/maven-runtime.md
rg "Apache Maven|mvn|BLOCKED" doc/discovery/maven-runtime.md
```

### P0-M006 Locate Node Runtime

Goal: Record Node/npm version for frontend.

Allowed new files:

- `doc/discovery/node-runtime.md`

Implementation:

1. Run `node --version`.
2. Run `npm --version`.
3. Record exact output.
4. If missing, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/node-runtime.md
rg "node|npm|BLOCKED" doc/discovery/node-runtime.md
```

### P0-M007 Locate Codex Command

Goal: Record Codex executable path.

Allowed new files:

- `doc/discovery/codex-command.md`

Implementation:

1. Run `command -v codex`.
2. Record exact output.
3. Do not run sessions scan.
4. If missing, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/codex-command.md
rg "codex|BLOCKED" doc/discovery/codex-command.md
```

### P0-M008 Record Codex Version

Goal: Record Codex version or version command failure.

Allowed new files:

- `doc/discovery/codex-version.md`

Implementation:

1. Use path from `doc/discovery/codex-command.md`.
2. Run `codex --version`.
3. Record stdout, stderr, exit code.
4. If command fails, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/codex-version.md
rg "codex --version|exit code|BLOCKED" doc/discovery/codex-version.md
```

### P0-M009 Record Codex Help

Goal: Capture Codex command syntax evidence.

Allowed new files:

- `doc/discovery/codex-help.md`

Implementation:

1. Run `codex --help`.
2. Record relevant command sections.
3. Do not infer command builder behavior.
4. If help does not show usable noninteractive command, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/codex-help.md
rg "codex --help|Usage|BLOCKED" doc/discovery/codex-help.md
```

### P0-M010 Inspect Codex Home Top Level

Goal: Record top-level structure of `~/.codex`.

Allowed new files:

- `doc/discovery/codex-home.md`

Implementation:

1. Check `~/.codex` exists.
2. Run `find ~/.codex -maxdepth 2 -type f -o -type d`.
3. Record paths.
4. Do not print secrets.
5. If missing, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/codex-home.md
rg "\\.codex|BLOCKED" doc/discovery/codex-home.md
```

### P0-M011 Locate opencode Command

Goal: Record opencode executable path.

Allowed new files:

- `doc/discovery/opencode-command.md`

Implementation:

1. Run `command -v opencode`.
2. Check `~/.opencode/bin/opencode`.
3. Record exact results.
4. If neither exists, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/opencode-command.md
rg "opencode|BLOCKED" doc/discovery/opencode-command.md
```

### P0-M012 Record opencode Version

Goal: Record opencode version or version failure.

Allowed new files:

- `doc/discovery/opencode-version.md`

Implementation:

1. Use path from `doc/discovery/opencode-command.md`.
2. Run opencode version command supported by help or `--version`.
3. Record stdout, stderr, exit code.
4. If command fails, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/opencode-version.md
rg "opencode|version|exit code|BLOCKED" doc/discovery/opencode-version.md
```

### P0-M013 Record opencode Help

Goal: Capture opencode command syntax evidence.

Allowed new files:

- `doc/discovery/opencode-help.md`

Implementation:

1. Run opencode help command.
2. Record relevant run/session sections.
3. Do not infer command builder behavior.
4. If no usable run command is shown, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/opencode-help.md
rg "opencode|help|run|BLOCKED" doc/discovery/opencode-help.md
```

### P0-M014 Inspect opencode State Roots

Goal: Record opencode root directories.

Allowed new files:

- `doc/discovery/opencode-roots.md`

Implementation:

1. Check:
   - `~/.config/opencode`
   - `~/.local/share/opencode`
   - `~/.local/state/opencode`
2. Record existence and top-level structure.
3. Do not parse sessions.
4. If all missing, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/opencode-roots.md
rg "\\.config/opencode|\\.local/share/opencode|\\.local/state/opencode|BLOCKED" doc/discovery/opencode-roots.md
```

### P0-M015 List Codex Candidate Session Files

Goal: Record candidate Codex session files without parsing.

Allowed new files:

- `doc/discovery/codex-session-candidates.md`

Implementation:

1. Use `~/.codex/sessions`.
2. Find files matching:
   - `**/*.jsonl`
3. Record path, size, modified time.
4. Do not copy full contents.
5. Do not list config, logs, shell snapshots, or SQLite files as first parser candidates.

Verification:

```bash
test -f doc/discovery/codex-session-candidates.md
rg "\\.jsonl|Codex|sessions" doc/discovery/codex-session-candidates.md
```

### P0-M016 List opencode Candidate Session Files

Goal: Record candidate opencode session files without parsing.

Allowed new files:

- `doc/discovery/opencode-session-candidates.md`

Implementation:

1. Use roots from `doc/discovery/opencode-roots.md`.
2. Find only:
   - `/home/lqtiger/.local/share/opencode/opencode.db`
3. Record path, size, modified time.
4. Do not copy full contents.
5. Do not list logs or prompt-history as first parser candidates.

Verification:

```bash
test -f doc/discovery/opencode-session-candidates.md
rg "opencode.db|SQLite|opencode" doc/discovery/opencode-session-candidates.md
```

### P0-M017 Classify One Codex Candidate Format

Goal: Classify one real Codex candidate file.

Allowed new files:

- `doc/discovery/codex-sample-1.md`

Implementation:

1. Select the most recent likely session candidate from `codex-session-candidates.md`.
2. Record file path, size, modified time.
3. Record first non-secret structural line with values redacted.
4. Classify as `JSON`, `JSONL`, `TEXT`, or `UNKNOWN`.
5. If no candidate exists, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/codex-sample-1.md
rg "JSON|JSONL|TEXT|UNKNOWN|BLOCKED" doc/discovery/codex-sample-1.md
```

### P0-M018 Classify One opencode Candidate Format

Goal: Classify one real opencode candidate file.

Allowed new files:

- `doc/discovery/opencode-sample-1.md`

Implementation:

1. Select the most recent likely session candidate from `opencode-session-candidates.md`.
2. Record file path, size, modified time.
3. Record first non-secret structural line with values redacted.
4. Classify as `JSON`, `JSONL`, `TEXT`, `SQLITE`, or `UNKNOWN`.
5. If no candidate exists, document `BLOCKED`.

Verification:

```bash
test -f doc/discovery/opencode-sample-1.md
rg "JSON|JSONL|TEXT|SQLITE|UNKNOWN|BLOCKED" doc/discovery/opencode-sample-1.md
```

### P0-M019 Create Consolidated Session Format Samples

Goal: Consolidate sample findings for parser tickets.

Allowed new files:

- `doc/discovery/session-format-samples.md`

Implementation:

1. Link to `codex-sample-1.md`.
2. Link to `opencode-sample-1.md`.
3. State which formats are allowed for first parser implementation.
4. If either sample is blocked, this file must state `BLOCKED`.

Verification:

```bash
test -f doc/discovery/session-format-samples.md
rg "codex-sample-1|opencode-sample-1|allowed for first parser|BLOCKED" doc/discovery/session-format-samples.md
```

### P0-M020 Create CLI Command Syntax Summary

Goal: Consolidate command syntax evidence.

Allowed new files:

- `doc/discovery/cli-command-syntax.md`

Implementation:

1. Link to Codex help document.
2. Link to opencode help document.
3. State exact supported noninteractive command if proven.
4. If not proven, mark that source command builder `BLOCKED`.

Verification:

```bash
test -f doc/discovery/cli-command-syntax.md
rg "Codex|opencode|noninteractive|BLOCKED" doc/discovery/cli-command-syntax.md
```

### P0-M021 Create Model Catalog Discovery

Goal: Define real model catalog for config.

Allowed new files:

- `doc/discovery/model-catalog.md`

Implementation:

1. List initial model ids only if user or existing config confirms them.
2. For each model record:
   - id
   - label
   - supported source
   - command model name
   - enabled flag
3. If not confirmed, mark `BLOCKED`.

Verification:

```bash
test -f doc/discovery/model-catalog.md
rg "model id|supported source|enabled|BLOCKED" doc/discovery/model-catalog.md
```

## 3. Backend Skeleton Micro Tickets

### B1-M001 Create pom.xml Only

Goal: Add Maven project descriptor.

Allowed new files:

- `pom.xml`

Implementation:

1. Set group id `com.lqtigee`.
2. Set artifact id `Lqtigee-spark-ai`.
3. Use Spring Boot 3 parent.
4. Use Java 21.
5. Add only:
   - web
   - validation
   - actuator
   - test

Verification:

```bash
mvn -q -DskipTests validate
```

### B1-M002 Create Application Class Only

Goal: Add Spring Boot entrypoint.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/LqtigeeSparkAiApplication.java`

Implementation:

1. Create package `com.lqtigee.sparkai`.
2. Add `main` method.
3. Add `@SpringBootApplication`.
4. No controllers.
5. No business logic.

Verification:

```bash
mvn test
```

### B1-M003 Create application.yml Only

Goal: Add minimal config with port.

Allowed new files:

- `src/main/resources/application.yml`

Implementation:

1. Set `server.port: 20261`.
2. Set application name `Lqtigee-spark-ai`.
3. Do not add models yet.
4. Do not add scanner paths yet.

Verification:

```bash
rg "20261|Lqtigee-spark-ai" src/main/resources/application.yml
```

### B1-M004 Add Context Load Test Only

Goal: Add minimal Spring context test.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/LqtigeeSparkAiApplicationTests.java`

Implementation:

1. Add `contextLoads`.
2. No HTTP test.
3. No mock beans.

Verification:

```bash
mvn test
```

### B1-M005 Define HealthDto Only

Goal: Create health DTO.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/HealthDto.java`

Implementation:

Fields only:

- `serviceName`
- `appName`
- `port`
- `status`
- `timestamp`

Verification:

```bash
mvn test
```

### B1-M006 Add HealthController Only

Goal: Add public health endpoint.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/web/HealthController.java`

Implementation:

1. Add `GET /api/health`.
2. Return real constants:
   - `Lqtigee-spark-ai`
   - `Lqtigee`
   - `20261`
   - `STARTING`
3. No adapter status.

Verification:

```bash
mvn test
```

### B1-M007 Add HealthController Test Only

Goal: Verify health JSON.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/web/HealthControllerTest.java`

Implementation:

1. Test `GET /api/health`.
2. Assert status 200.
3. Assert service/app/port.

Verification:

```bash
mvn test -Dtest=HealthControllerTest
```

### B1-M008 Define ErrorCode Enum Only

Goal: Add stable error codes.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/error/ErrorCode.java`

Implementation:

1. Add only codes from `requirements.md`.
2. Do not add behavior.

Verification:

```bash
mvn test
```

### B1-M009 Define ApiErrorDto Only

Goal: Add JSON error DTO.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/ApiErrorDto.java`

Fields:

- `code`
- `message`
- `detail`
- `timestamp`
- `path`

Verification:

```bash
mvn test
```

### B1-M010 Define ApiException Only

Goal: Add typed runtime exception.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/error/ApiException.java`

Implementation:

1. Store `ErrorCode`.
2. Store HTTP status.
3. Store safe detail.
4. No controller advice.

Verification:

```bash
mvn test
```

### B1-M010A Add Missing Error Code Contract Tickets Only

Goal: Add micro tickets for stable validation and internal error codes before global exception handling.

Allowed files:

- `doc/micro-tickets.md`

Implementation:

1. Add one later ticket to update `doc/requirements.md` and `doc/contracts/backend-api-contract.md` with:
   - `VALIDATION_FAILED`
   - `INTERNAL_ERROR`
2. Add one later ticket to update `src/main/java/com/lqtigee/sparkai/error/ErrorCode.java` with those codes.
3. Do not change Java code in this ticket.
4. Do not implement `GlobalExceptionHandler` in this ticket.

Verification:

```bash
rg "B1-M010B|B1-M010C|VALIDATION_FAILED|INTERNAL_ERROR" doc/micro-tickets.md
```

### B1-M010B Add Missing Error Codes To Contracts Only

Goal: Define stable codes for validation and generic internal failures.

Allowed files:

- `doc/requirements.md`
- `doc/contracts/backend-api-contract.md`

Implementation:

1. Add `VALIDATION_FAILED` to the stable error code list.
2. Add `INTERNAL_ERROR` to the stable error code list.
3. State that validation exceptions map to `VALIDATION_FAILED`.
4. State that unhandled generic exceptions map to `INTERNAL_ERROR`.
5. Do not change Java code.

Verification:

```bash
rg "VALIDATION_FAILED|INTERNAL_ERROR" doc/requirements.md doc/contracts/backend-api-contract.md
```

### B1-M010C Add Missing Error Codes To ErrorCode Enum Only

Goal: Make code enum match the updated contract.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/error/ErrorCode.java`

Implementation:

1. Add `VALIDATION_FAILED`.
2. Add `INTERNAL_ERROR`.
3. Do not add behavior.
4. Do not implement `GlobalExceptionHandler`.

Verification:

```bash
mvn test
```

### B1-M011 Add GlobalExceptionHandler Only

Goal: Convert exceptions to `ApiErrorDto`.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/error/GlobalExceptionHandler.java`

Implementation:

1. Handle `ApiException`.
2. Handle validation exception.
3. Handle generic exception as internal error.
4. Return JSON.
5. No stack trace in response.

Verification:

```bash
mvn test
```

### B1-M012 Add Error Shape Test Only

Goal: Prove unknown path returns JSON.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/web/ErrorShapeTest.java`

Implementation:

1. Request non-existent `/api/not-found`.
2. Assert JSON content type.
3. Assert `code` exists.

Verification:

```bash
mvn test -Dtest=ErrorShapeTest
```

## 4. Security Micro Tickets

### B2-M001 Define SecurityProperties Only

Goal: Bind API token config.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/config/SecurityProperties.java`

Implementation:

1. Prefix `lqtigee.security`.
2. Field `apiToken`.
3. Add method `validate()`.
4. Empty token throws `ApiException` or startup validation error.

Verification:

```bash
mvn test
```

### B2-M002 Add SecurityProperties To application.yml

Goal: Add token config placeholder.

Allowed files:

- `src/main/resources/application.yml`

Implementation:

1. Add `lqtigee.security.api-token`.
2. Use environment variable placeholder.
3. No hardcoded real secret.

Verification:

```bash
rg "lqtigee:|security:|api-token" src/main/resources/application.yml
```

### B2-M003 Add SecurityProperties Validation Test

Goal: Prove empty token fails.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/config/SecurityPropertiesTest.java`

Implementation:

1. Instantiate with empty token.
2. Assert validation fails.
3. Instantiate with non-empty token.
4. Assert validation passes.

Verification:

```bash
mvn test -Dtest=SecurityPropertiesTest
```

### B2-M004 Add BearerTokenFilter Only

Goal: Implement token check.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/security/BearerTokenFilter.java`

Implementation:

1. Skip `/api/health`.
2. Protect other `/api/**`.
3. Missing header -> `AUTH_TOKEN_MISSING`.
4. Wrong token -> `AUTH_TOKEN_INVALID`.
5. Do not add CORS.

Verification:

```bash
mvn test
```

### B2-M005 Register BearerTokenFilter Only

Goal: Wire filter into Spring.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/config/SecurityConfig.java`

Implementation:

1. Register `BearerTokenFilter`.
2. Do not add user accounts.
3. Do not add OAuth.

Verification:

```bash
mvn test
```

### B2-M006 Add Auth Missing Token Test

Goal: Verify missing token.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/security/BearerTokenFilterTest.java`

Implementation:

1. Request protected endpoint without token.
2. Assert 401.
3. Assert JSON code `AUTH_TOKEN_MISSING`.

Verification:

```bash
mvn test -Dtest=BearerTokenFilterTest
```

### B2-M007 Add Auth Wrong Token Test

Goal: Verify wrong token.

Allowed files:

- `src/test/java/com/lqtigee/sparkai/security/BearerTokenFilterTest.java`

Implementation:

1. Add test for wrong token.
2. Assert 401.
3. Assert JSON code `AUTH_TOKEN_INVALID`.

Verification:

```bash
mvn test -Dtest=BearerTokenFilterTest
```

## 5. Contract DTO Micro Tickets

### C3-M001 Define AgentSource Enum

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/AgentSource.java`

Implementation:

1. Values exactly:
   - `CODEX`
   - `OPENCODE`

Verification:

```bash
mvn test
```

### C3-M002 Define SessionStatus Enum

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/SessionStatus.java`

Implementation:

1. Values exactly:
   - `ACTIVE`
   - `IDLE`
   - `RUNNING`
   - `FAILED`
   - `UNKNOWN`

Verification:

```bash
mvn test
```

### C3-M003 Define RunStatus Enum

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/RunStatus.java`

Implementation:

1. Values exactly:
   - `CREATED`
   - `RUNNING`
   - `EXITED`
   - `FAILED`
   - `STOPPED`

Verification:

```bash
mvn test
```

### C3-M004 Define CommandMode Enum

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/CommandMode.java`

Implementation:

1. Values exactly:
   - `ASK`
   - `EDIT`
   - `REVIEW`
   - `SHELL`

Verification:

```bash
mvn test
```

### C3-M005 Define RemoteSessionDto Fields

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/RemoteSessionDto.java`

Implementation:

1. Add fields:
   - `id`
   - `source`
   - `title`
   - `workspace`
   - `model`
   - `status`
   - `updatedAt`
   - `lastMessage`
   - `rawFile`
2. No parser logic.

Verification:

```bash
mvn test
```

### C3-M006 Define AdapterHealthDto Fields

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/AdapterHealthDto.java`

Fields:

- `source`
- `available`
- `status`
- `version`
- `lastErrorCode`
- `lastErrorMessage`

Verification:

```bash
mvn test
```

### C3-M007 Define ModelDto Fields

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/ModelDto.java`

Fields:

- `id`
- `label`
- `commandModelName`
- `sources`
- `enabled`

Verification:

```bash
mvn test
```

### C3-M008 Define StartRunRequest Fields

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/StartRunRequest.java`

Fields:

- `sessionId`
- `source`
- `modelId`
- `mode`
- `prompt`
- `confirmDangerous`

Verification:

```bash
mvn test
```

### C3-M009 Define StartRunResponse Fields

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/StartRunResponse.java`

Fields:

- `runId`
- `sessionId`
- `source`
- `status`
- `startedAt`

Verification:

```bash
mvn test
```

### C3-M010 Define RunEventDto Fields

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/RunEventDto.java`

Fields:

- `runId`
- `type`
- `message`
- `timestamp`
- `data`

Verification:

```bash
mvn test
```

## 6. Scanner and Format Detection Micro Tickets

### D4-M001 Define SessionFileFormat Enum

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/SessionFileFormat.java`

Values:

- `JSON`
- `JSONL`
- `TEXT`
- `SQLITE`
- `UNKNOWN`

Verification:

```bash
mvn test
```

### D4-M002 Create CodexFileScanner Class Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFileScanner.java`

Implementation:

1. Create class.
2. Add constructor if needed.
3. Add method signature:

```java
List<Path> scan(Path codexHome)
```

4. Method may throw `UnsupportedOperationException` only in this skeleton ticket.

Verification:

```bash
mvn test
```

### D4-M003 Implement CodexFileScanner Missing Directory Error

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFileScanner.java`

Implementation:

1. If `codexHome` does not exist, throw `ApiException`.
2. Error code `CODEX_HOME_NOT_FOUND`.
3. Do not scan files yet.

Verification:

```bash
mvn test
```

### D4-M004 Test CodexFileScanner Missing Directory

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/codex/CodexFileScannerTest.java`

Implementation:

1. Pass missing temp path.
2. Assert error code `CODEX_HOME_NOT_FOUND`.

Verification:

```bash
mvn test -Dtest=CodexFileScannerTest
```

### D4-M005 Implement CodexFileScanner Extension Filter

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFileScanner.java`

Implementation:

1. Scan only under `codexHome`.
2. Include only files under `codexHome/sessions`.
3. Include only extension:
   - `.jsonl`
4. Return absolute normalized paths.
5. Do not parse contents.
6. Do not scan `config.toml`, logs, shell snapshots, or SQLite files.

Verification:

```bash
mvn test
```

### D4-M006 Test CodexFileScanner Extension Filter

Allowed files:

- `src/test/java/com/lqtigee/sparkai/codex/CodexFileScannerTest.java`

Implementation:

1. Create temp directory.
2. Add `.jsonl` under `sessions`.
3. Add `.jsonl` outside `sessions`.
4. Add `.log`, `.txt`, `.md`, `.db`, and `.json` under `sessions`.
5. Assert only `sessions/**/*.jsonl` files are returned.

Verification:

```bash
mvn test -Dtest=CodexFileScannerTest
```

### D4-M007 Create CodexFormatDetector Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFormatDetector.java`

Implementation:

1. Add method:

```java
SessionFileFormat detect(Path file)
```

2. Skeleton may return `UNKNOWN`.
3. No business parsing.

Verification:

```bash
mvn test
```

### D4-M008 Implement CodexFormatDetector JSON Detection

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFormatDetector.java`

Implementation:

1. Read first non-whitespace character.
2. If first char is `{`, return `JSON`.
3. Do not validate business fields.

Verification:

```bash
mvn test
```

### D4-M009 Test CodexFormatDetector JSON Detection

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/codex/CodexFormatDetectorTest.java`

Implementation:

1. Create temp file with JSON object.
2. Assert `JSON`.

Verification:

```bash
mvn test -Dtest=CodexFormatDetectorTest
```

### D4-M010 Implement CodexFormatDetector JSONL Detection

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFormatDetector.java`

Implementation:

1. Read first three non-empty lines.
2. If each non-empty line starts with `{`, return `JSONL`.
3. JSON object single-line file remains `JSON` only if detected by previous rule.

Verification:

```bash
mvn test
```

### D4-M011 Test CodexFormatDetector JSONL Detection

Allowed files:

- `src/test/java/com/lqtigee/sparkai/codex/CodexFormatDetectorTest.java`

Implementation:

1. Create temp JSONL file.
2. Assert `JSONL`.

Verification:

```bash
mvn test -Dtest=CodexFormatDetectorTest
```

### D4-M012 Implement CodexFormatDetector Unknown Detection

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexFormatDetector.java`

Implementation:

1. If not JSON or JSONL, return `UNKNOWN`.
2. Do not throw for unknown content.

Verification:

```bash
mvn test
```

### D4-M013 Create OpencodeFileScanner Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeFileScanner.java`

Implementation:

1. Add method:

```java
List<Path> scan(List<Path> roots)
```

2. Skeleton may throw `UnsupportedOperationException`.

Verification:

```bash
mvn test
```

### D4-M014 Implement OpencodeFileScanner Missing Roots Error

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeFileScanner.java`

Implementation:

1. If all roots are missing, throw `ApiException`.
2. Error code `OPENCODE_CONFIG_NOT_FOUND`.
3. Do not scan files yet.

Verification:

```bash
mvn test
```

### D4-M015 Test OpencodeFileScanner Missing Roots

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeFileScannerTest.java`

Implementation:

1. Pass missing temp roots.
2. Assert `OPENCODE_CONFIG_NOT_FOUND`.

Verification:

```bash
mvn test -Dtest=OpencodeFileScannerTest
```

### D4-M016 Implement OpencodeFileScanner Extension Filter

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeFileScanner.java`

Implementation:

1. Scan existing roots only.
2. Include only file name:
   - `opencode.db`
3. Return absolute normalized paths.
4. Do not parse.
5. Do not scan logs or prompt history as primary session source.

Verification:

```bash
mvn test
```

### D4-M017 Test OpencodeFileScanner Extension Filter

Allowed files:

- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeFileScannerTest.java`

Implementation:

1. Create temp roots.
2. Add `opencode.db`.
3. Add `.json`, `.jsonl`, `.log`, `.txt`, `.sqlite`, and prompt-history files.
4. Assert only `opencode.db` is returned.

Verification:

```bash
mvn test -Dtest=OpencodeFileScannerTest
```

## 7. Parser Micro Tickets

Parser tickets are locked until `doc/discovery/session-format-samples.md` exists and identifies a parseable real sample. If not, output `BLOCKED`.

### PARSE-M001 Create Parser Fixture Directory

Allowed new directories:

- `src/test/resources/samples/`

Implementation:

1. Create directory only.
2. Do not add sample content yet.

Verification:

```bash
test -d src/test/resources/samples
```

### PARSE-M002 Add Sanitized Codex Sample Fixture

Allowed new files:

- `src/test/resources/samples/codex-session-sample.jsonl`

Implementation:

1. Use sanitized structure from `doc/discovery/codex-sample-1.md`.
2. Preserve field names.
3. Redact values.
4. Do not invent fields.

Verification:

```bash
test -f src/test/resources/samples/codex-session-sample.jsonl
```

### PARSE-M003 Create CodexJsonlParser Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexJsonlParser.java`

Implementation:

1. Add method:

```java
RemoteSessionDto parse(Path file)
```

2. Skeleton may throw `UnsupportedOperationException`.

Verification:

```bash
mvn test
```

### PARSE-M004 Implement CodexJsonlParser Required Field Extraction

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexJsonlParser.java`

Implementation:

1. Extract only fields proven in sanitized sample.
2. Required:
   - id
   - workspace
   - model
   - updatedAt
   - rawFile
3. Missing required field throws `CODEX_SESSION_FIELD_MISSING`.
4. Do not fallback to filename.

Verification:

```bash
mvn test
```

### PARSE-M005 Test CodexJsonlParser With Real Sanitized Sample

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/codex/CodexJsonlParserTest.java`

Implementation:

1. Load `src/test/resources/samples/codex-session-sample.jsonl`.
2. Assert required fields.
3. Assert source `CODEX`.

Verification:

```bash
mvn test -Dtest=CodexJsonlParserTest
```

### PARSE-M006 Test CodexJsonlParser Missing Field Failure

Allowed files:

- `src/test/java/com/lqtigee/sparkai/codex/CodexJsonlParserTest.java`

Implementation:

1. Create temp JSONL missing one required field.
2. Assert `CODEX_SESSION_FIELD_MISSING`.

Verification:

```bash
mvn test -Dtest=CodexJsonlParserTest
```

### BUG-PARSE-001 Make Codex Sanitized Sample Timestamp Parseable

Symptom:

`PARSE-M005` loads `src/test/resources/samples/codex-session-sample.jsonl`, but parsing fails before required fields can be asserted.

Expected:

The sanitized Codex sample keeps redacted session values while using a syntactically valid ISO timestamp so `updatedAt` can be parsed.

Actual:

The sample uses `"<iso>"`, which is redacted but not parseable by `Instant.parse`.

Allowed files:

- `src/test/resources/samples/codex-session-sample.jsonl`

Failing verification:

```bash
mvn test -Dtest=CodexJsonlParserTest
```

Implementation:

1. Replace `"<iso>"` values with a non-sensitive fixed ISO-8601 timestamp.
2. Keep all existing field names.
3. Do not add real session data.
4. Do not change parser or test code.

Verification:

```bash
mvn test -Dtest=CodexJsonlParserTest
```

### PARSE-M007 Add Sanitized opencode SQLite Session Schema Fixture

Allowed new files:

- `src/test/resources/samples/opencode-session-schema.sql`

Implementation:

1. Use table/column structure from `doc/discovery/opencode-sample-1.md`.
2. Preserve the `session` table column names.
3. Include only schema and sanitized insert rows needed for tests.
4. Do not invent fields not present in discovery.

Verification:

```bash
test -f src/test/resources/samples/opencode-session-schema.sql
```

### PARSE-M008 Create OpencodeSqliteSessionReader Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`

Implementation:

1. Add method:

```java
List<RemoteSessionDto> readSessions(Path databasePath)
```

2. Skeleton may throw `UnsupportedOperationException`.

Verification:

```bash
mvn test
```

### PARSE-M009 Implement OpencodeSqliteSessionReader Database Validation

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`

Implementation:

1. If `databasePath` does not exist, throw `OPENCODE_CONFIG_NOT_FOUND`.
2. If file is not readable, throw `OPENCODE_SESSION_SCAN_FAILED`.
3. Do not query session rows yet.
4. Use read-only SQLite connection mode.

Verification:

```bash
mvn test
```

### PARSE-M009A Implement OpencodeSqliteSessionReader Schema Guard

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`

Implementation:

1. Query `PRAGMA table_info(session)`.
2. Require columns:
   - `id`
   - `directory`
   - `title`
   - `model`
   - `time_updated`
   - `time_archived`
   - `path`
   - `agent`
3. Missing column throws `OPENCODE_SESSION_SCHEMA_MISMATCH`.
4. Do not query rows in this ticket.

Verification:

```bash
mvn test
```

### PARSE-M009B Test Opencode SQLite Schema Guard

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSchemaGuardTest.java`

Implementation:

1. Create temp SQLite database with required columns.
2. Assert schema validation passes.
3. Create temp SQLite database missing `model`.
4. Assert `OPENCODE_SESSION_SCHEMA_MISMATCH`.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteSchemaGuardTest
```

### PARSE-M010 Implement OpencodeSqliteSessionReader Session Query

Allowed new files:

- none

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`

Implementation:

1. Query only the `session` table.
2. Read columns:
   - `id`
   - `directory`
   - `title`
   - `model`
   - `time_updated`
   - `time_archived`
   - `path`
   - `agent`
3. Order by `time_updated DESC`.
4. Do not read prompt/message content.

Verification:

```bash
mvn test
```

### PARSE-M011 Implement OpencodeSqliteSessionReader DTO Mapping

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`

Implementation:

1. Map:
   - `session.id` -> `RemoteSessionDto.id`
   - `OPENCODE` -> `RemoteSessionDto.source`
   - `session.title` -> `RemoteSessionDto.title`
   - `session.directory` -> `RemoteSessionDto.workspace`
   - `session.model` JSON text -> model display/id
   - `session.time_updated` -> `updatedAt`
   - database path -> `rawFile`
2. If required fields are missing, throw `OPENCODE_SESSION_FIELD_MISSING`.
3. Do not fallback to filename or log files.

Verification:

```bash
mvn test
```

### PARSE-M012 Test OpencodeSqliteSessionReader With Sanitized SQLite Fixture

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReaderTest.java`

Implementation:

1. Create a temporary SQLite database using `src/test/resources/samples/opencode-session-schema.sql`.
2. Insert one sanitized session row.
3. Assert required DTO fields.
4. Assert source `OPENCODE`.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteSessionReaderTest
```

### PARSE-M013 Test OpencodeSqliteSessionReader Missing Field Failure

Allowed files:

- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReaderTest.java`

Implementation:

1. Create a temporary SQLite database with a session row missing one required field.
2. Assert `OPENCODE_SESSION_FIELD_MISSING`.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteSessionReaderTest
```

## 7.5 PostgreSQL Persistence Micro Tickets

These tickets add PostgreSQL for Lqtigee-owned persistence only. They must not replace Codex JSONL discovery or opencode SQLite discovery.

### PG-M000 Document PostgreSQL Persistence Boundary

Allowed files:

- `doc/requirements.md`
- `doc/architecture.md`
- `doc/implementation-design.md`
- `doc/micro-tickets.md`

Implementation:

1. State that Lqtigee-owned persistence uses PostgreSQL.
2. State that PostgreSQL does not replace Codex JSONL discovery.
3. State that PostgreSQL does not replace opencode SQLite discovery.
4. Add PostgreSQL implementation micro tickets.
5. Do not change Java code in this ticket.

Verification:

```bash
rg "PostgreSQL|jdbc:postgresql|PG-M001|opencode sessions: SQLite|PostgreSQL must not replace" doc/requirements.md doc/architecture.md doc/implementation-design.md doc/micro-tickets.md
```

### PG-M001 Add PostgreSQL JDBC Dependencies

Allowed files:

- `pom.xml`

Implementation:

1. Add Spring JDBC dependency.
2. Add PostgreSQL JDBC driver dependency.
3. Do not add JPA in this ticket.
4. Do not remove SQLite reader code.

Verification:

```bash
mvn test
```

### PG-M002 Create DatabaseProperties Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/config/DatabaseProperties.java`

Implementation:

1. Prefix `lqtigee.database`.
2. Add fields:
   - url
   - username
   - password
   - enabled
3. Add getters and setters.
4. Do not open a connection in this ticket.

Verification:

```bash
mvn test
```

### PG-M003 Add PostgreSQL Configuration Keys

Allowed files:

- `src/main/resources/application.yml`

Implementation:

1. Add `lqtigee.database.enabled`.
2. Add `lqtigee.database.url`.
3. Add `lqtigee.database.username`.
4. Add `lqtigee.database.password`.
5. Password must reference an environment variable.
6. Do not include real passwords.

Verification:

```bash
rg "database:|jdbc:postgresql|LQTIGEE_DB_PASSWORD" src/main/resources/application.yml
```

### PG-M004 Create PostgreSQL Connection Factory Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/persistence/PostgresConnectionFactory.java`

Implementation:

1. Constructor accepts `DatabaseProperties`.
2. Add method `Connection open()`.
3. Skeleton may throw `UnsupportedOperationException`.
4. Do not execute queries.

Verification:

```bash
mvn test
```

### PG-M005 Implement PostgreSQL Connection Validation

Allowed files:

- `src/main/java/com/lqtigee/sparkai/persistence/PostgresConnectionFactory.java`

Implementation:

1. If database is disabled, throw `ApiException` with `INTERNAL_ERROR`.
2. If URL, username, or password is missing, throw `ApiException` with `VALIDATION_FAILED`.
3. Open JDBC connection using configured values.
4. Do not create tables.
5. Do not fake success if connection fails.

Verification:

```bash
mvn test
```

### PG-M006 Add PostgreSQL Schema Fixture

Allowed new files:

- `src/main/resources/db/postgres/001_init.sql`

Implementation:

1. Add table `run_records`.
2. Add table `audit_events`.
3. Add table `settings`.
4. Use PostgreSQL syntax.
5. Do not add session cache tables.

Verification:

```bash
test -f src/main/resources/db/postgres/001_init.sql
rg "CREATE TABLE run_records|CREATE TABLE audit_events|CREATE TABLE settings" src/main/resources/db/postgres/001_init.sql
```

### PG-M007 Create RunRecordRepository Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/persistence/RunRecordRepository.java`

Implementation:

1. Constructor accepts `PostgresConnectionFactory`.
2. Add method `void saveStarted(String runId, String source, String sessionId, String modelId)`.
3. Skeleton may throw `UnsupportedOperationException`.
4. Do not write session discovery data.

Verification:

```bash
mvn test
```

### PG-M008 Implement RunRecordRepository saveStarted

Allowed files:

- `src/main/java/com/lqtigee/sparkai/persistence/RunRecordRepository.java`

Implementation:

1. Insert one row into `run_records`.
2. Use prepared statement parameters only.
3. On SQL failure, throw `PROCESS_START_FAILED`.
4. Do not catch and ignore SQL exceptions.

Verification:

```bash
mvn test
```

## 8. Model Micro Tickets

### M5-M001 Define ModelProperties Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/config/ModelProperties.java`

Implementation:

1. Prefix `lqtigee.models`.
2. Add list of model entries.
3. Add nested entry fields:
   - id
   - label
   - commandModelName
   - sources
   - enabled
4. No service.

Verification:

```bash
mvn test
```

### M5-M002 Add Model Config From Discovery

Allowed files:

- `src/main/resources/application.yml`

Implementation:

1. Use `doc/discovery/model-catalog.md`.
2. Add only confirmed models.
3. If no confirmed model exists, output `BLOCKED`.

Verification:

```bash
rg "models:|commandModelName|enabled" src/main/resources/application.yml
```

### M5-M003 Implement ModelProperties Empty Validation

Allowed files:

- `src/main/java/com/lqtigee/sparkai/config/ModelProperties.java`

Implementation:

1. Add `validate()`.
2. Empty list fails startup or throws.
3. Duplicate model id fails validation.

Verification:

```bash
mvn test
```

### M5-M004 Test ModelProperties Validation

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/config/ModelPropertiesTest.java`

Implementation:

1. Empty list fails.
2. Duplicate id fails.
3. One valid model passes.

Verification:

```bash
mvn test -Dtest=ModelPropertiesTest
```

### M5-M005 Create ModelService Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/service/ModelService.java`

Methods:

```java
List<ModelDto> listModels()
ModelDto getRequiredModel(String id)
void validateModelForSource(String id, AgentSource source)
```

Implementation:

1. Method bodies may be minimal.
2. No controller.

Verification:

```bash
mvn test
```

### M5-M006 Implement ModelService listModels

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/ModelService.java`

Implementation:

1. Return config-backed models only.
2. Do not hardcode models.

Verification:

```bash
mvn test
```

### M5-M007 Implement ModelService getRequiredModel

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/ModelService.java`

Implementation:

1. If id missing, throw `MODEL_NOT_FOUND`.
2. Return matching model.

Verification:

```bash
mvn test
```

### M5-M008 Implement ModelService validateModelForSource

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/ModelService.java`

Implementation:

1. Missing model -> `MODEL_NOT_FOUND`.
2. Source unsupported -> `MODEL_SOURCE_UNSUPPORTED`.
3. Disabled model -> `MODEL_NOT_FOUND` or explicit disabled error if defined.

Verification:

```bash
mvn test
```

### M5-M009 Add ModelService Test

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/service/ModelServiceTest.java`

Implementation:

1. Test list.
2. Test missing id.
3. Test unsupported source.

Verification:

```bash
mvn test -Dtest=ModelServiceTest
```

### M5-M010 Add ModelController

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/web/ModelController.java`

Implementation:

1. Add `GET /api/models`.
2. Controller calls only `ModelService.listModels()`.
3. No config parsing in controller.

Verification:

```bash
mvn test
```

### M5-M011 Add ModelController Test

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/web/ModelControllerTest.java`

Implementation:

1. With valid token, returns JSON list.
2. Without token, returns 401.

Verification:

```bash
mvn test -Dtest=ModelControllerTest
```

## 9. Session Service and API Micro Tickets

### SESS-M001 Define AgentAdapter Interface Only

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/adapter/AgentAdapter.java`

Methods:

```java
AgentSource source()
AdapterHealthDto probe()
List<RemoteSessionDto> discoverSessions()
```

No command method yet.

Verification:

```bash
mvn test
```

### SESS-M002 Create CodexAdapter Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/adapter/CodexAdapter.java`

Implementation:

1. Implements `AgentAdapter`.
2. `source()` returns `CODEX`.
3. Other methods throw `UnsupportedOperationException`.

Verification:

```bash
mvn test
```

### SESS-M003 Implement CodexAdapter probe

Allowed files:

- `src/main/java/com/lqtigee/sparkai/adapter/CodexAdapter.java`

Implementation:

1. Use discovered command/path config when available.
2. Return unavailable with typed error if missing.
3. Do not scan sessions.

Verification:

```bash
mvn test
```

### SESS-M004 Create OpencodeAdapter Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/adapter/OpencodeAdapter.java`

Implementation:

1. Implements `AgentAdapter`.
2. `source()` returns `OPENCODE`.
3. Other methods throw `UnsupportedOperationException`.

Verification:

```bash
mvn test
```

### SESS-M005 Implement OpencodeAdapter probe

Allowed files:

- `src/main/java/com/lqtigee/sparkai/adapter/OpencodeAdapter.java`

Implementation:

1. Use discovered command/path config when available.
2. Return unavailable with typed error if missing.
3. Do not scan sessions.

Verification:

```bash
mvn test
```

### SESS-M006 Create SessionService Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/service/SessionService.java`

Methods:

```java
List<RemoteSessionDto> listAllSessions()
List<RemoteSessionDto> listBySource(AgentSource source)
RemoteSessionDto getRequiredSession(AgentSource source, String id)
```

Verification:

```bash
mvn test
```

### SESS-M007 Implement SessionService Source Routing

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/SessionService.java`

Implementation:

1. Route `CODEX` to Codex adapter.
2. Route `OPENCODE` to opencode adapter.
3. Unknown source impossible due enum.

Verification:

```bash
mvn test
```

### SESS-M008 Implement SessionService Unified Failure Rule

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/SessionService.java`

Implementation:

1. `listAllSessions()` calls both adapters.
2. If either fails, throw `ApiException` with dependency failure.
3. Do not return partial success.

Verification:

```bash
mvn test
```

### SESS-M009 Test SessionService Partial Failure

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/service/SessionServiceTest.java`

Implementation:

1. Fake adapter A fails.
2. Fake adapter B succeeds.
3. Assert unified service fails.
4. This test uses fake adapters, not fake business data.

Verification:

```bash
mvn test -Dtest=SessionServiceTest
```

### SESS-M010 Add SessionController listAll

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/web/SessionController.java`

Implementation:

1. Add `GET /api/sessions`.
2. Controller calls only `SessionService.listAllSessions()`.
3. No scanner/parser in controller.

Verification:

```bash
mvn test
```

### SESS-M011 Add SessionController Source Endpoints

Allowed files:

- `src/main/java/com/lqtigee/sparkai/web/SessionController.java`

Implementation:

1. Add `GET /api/codex/sessions`.
2. Add `GET /api/opencode/sessions`.
3. Each calls `SessionService.listBySource`.

Verification:

```bash
mvn test
```

### SESS-M012 Add SessionController Auth Test

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/web/SessionControllerTest.java`

Implementation:

1. Missing token returns 401.
2. Valid token reaches service.

Verification:

```bash
mvn test -Dtest=SessionControllerTest
```

## 10. Runtime Micro Tickets

Runtime tickets are locked until `doc/discovery/cli-command-syntax.md` proves command syntax for the target source.

Runtime command builder tickets are locked until these files exist:

- `doc/discovery/codex-resume-static-evidence.md` for Codex runtime.
- `doc/discovery/opencode-session-static-evidence.md` for opencode runtime.

If a static evidence document is missing or marked `BLOCKED`, runtime for that source is blocked.

### RUN-M001 Define CommandSpec

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/CommandSpec.java`

Fields:

- `command`
- `workdir`
- `environment`
- `source`
- `sessionId`
- `modelId`

Verification:

```bash
mvn test
```

### RUN-M002 Define ManagedProcess

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/ManagedProcess.java`

Fields:

- `runId`
- `process`
- `startedAt`
- `commandSpec`

Verification:

```bash
mvn test
```

### RUN-M003 Create PathGuard Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/util/PathGuard.java`

Method:

```java
void assertAllowed(Path path, List<Path> allowedRoots)
```

Verification:

```bash
mvn test
```

### RUN-M004 Implement PathGuard Reject Outside Root

Allowed files:

- `src/main/java/com/lqtigee/sparkai/util/PathGuard.java`

Implementation:

1. Normalize path.
2. Normalize roots.
3. If path not under any root, throw `WORKSPACE_NOT_ALLOWED`.

Verification:

```bash
mvn test
```

### RUN-M005 Test PathGuard

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/util/PathGuardTest.java`

Implementation:

1. Path inside root passes.
2. Path outside root fails.

Verification:

```bash
mvn test -Dtest=PathGuardTest
```

### RUN-M005A Create CodexCommandBuilder Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java`

Method:

```java
CommandSpec build(StartRunRequest request, RemoteSessionDto session, ModelDto model)
```

Implementation:

1. Skeleton only.
2. May throw `UnsupportedOperationException`.
3. Do not launch process.

Verification:

```bash
mvn test
```

### RUN-M005B Implement CodexCommandBuilder Resume Args

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java`

Implementation:

1. Require `doc/discovery/codex-resume-static-evidence.md` to exist and not contain `BLOCKED`.
2. Build argument array according to `doc/security/command-permission-matrix.md`.
3. Safe base shape:

```text
codex -C <workspace> <permission-args> exec resume --json -m <commandModelName> --skip-git-repo-check <sessionId> <prompt>
```

4. `ASK` and `REVIEW` use `-s read-only`.
5. `EDIT` uses `-s workspace-write`.
6. `SHELL` without `confirmDangerous=true` throws `DANGER_CONFIRM_REQUIRED`.
7. If `SHELL` and `confirmDangerous=true`, add `--dangerously-bypass-approvals-and-sandbox`.
8. Do not use shell string.
9. Do not use plain `codex exec <prompt>`.

Verification:

```bash
mvn test
```

### RUN-M005C Test CodexCommandBuilder Resume Args

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java`

Implementation:

1. Build command for `ASK`.
2. Assert `-s read-only`.
3. Build command for `EDIT`.
4. Assert `-s workspace-write`.
5. Build command for `SHELL` without confirmation.
6. Assert `DANGER_CONFIRM_REQUIRED`.
7. Assert `-C` appears before `exec`.
8. Assert `resume` appears after `exec`.
9. Assert session id appears before prompt.
10. Assert no shell string.

Verification:

```bash
mvn test -Dtest=CodexCommandBuilderTest
```

### RUN-M005D Create OpencodeCommandBuilder Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilder.java`

Method:

```java
CommandSpec build(StartRunRequest request, RemoteSessionDto session, ModelDto model)
```

Implementation:

1. Skeleton only.
2. May throw `UnsupportedOperationException`.
3. Do not launch process.

Verification:

```bash
mvn test
```

### RUN-M005E Implement OpencodeCommandBuilder Session Args

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilder.java`

Implementation:

1. Require `doc/discovery/opencode-session-static-evidence.md` to exist and not contain `BLOCKED`.
2. Build argument array according to `doc/security/command-permission-matrix.md`.
3. Safe base shape:

```text
opencode run --format json --model <commandModelName> --dir <workspace> --session <sessionId> <prompt>
```

4. `ASK`, `REVIEW`, and `EDIT` do not add dangerous flags.
5. `SHELL` without `confirmDangerous=true` throws `DANGER_CONFIRM_REQUIRED`.
6. If `SHELL` and `confirmDangerous=true`, insert `--dangerously-skip-permissions` before `<prompt>`.
7. Do not use shell string.
8. Do not use `--continue` for explicit selected session.

Verification:

```bash
mvn test
```

### RUN-M005F Test OpencodeCommandBuilder Session Args

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilderTest.java`

Implementation:

1. Build command for `ASK`.
2. Assert no dangerous flag.
3. Build command for `EDIT`.
4. Assert no dangerous flag.
5. Build command for `SHELL` without confirmation.
6. Assert `DANGER_CONFIRM_REQUIRED`.
7. Build command for confirmed `SHELL`.
8. Assert `--dangerously-skip-permissions`.
9. Assert `--session` value is selected session id.
10. Assert prompt is one array element.
11. Assert no shell string.

Verification:

```bash
mvn test -Dtest=OpencodeCommandBuilderTest
```

### RUN-M006 Create ProcessLauncher Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/ProcessLauncher.java`

Method:

```java
ManagedProcess start(String runId, CommandSpec spec)
```

Implementation:

1. Skeleton only.
2. Do not start process yet.

Verification:

```bash
mvn test
```

### RUN-M007 Implement ProcessLauncher ProcessBuilder Array

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/ProcessLauncher.java`

Implementation:

1. Use `new ProcessBuilder(spec.command())`.
2. Set workdir.
3. Set environment.
4. Do not use shell string.

Verification:

```bash
mvn test
```

### RUN-M008 Test ProcessLauncher Deterministic Local Process

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/runtime/ProcessLauncherTest.java`

Implementation:

1. Use deterministic local process available on Linux, e.g. `/bin/echo`.
2. Assert process exits 0.
3. Assert no shell string used.
4. Do not execute Codex or opencode.

Verification:

```bash
mvn test -Dtest=ProcessLauncherTest
```

### RUN-M009 Define RunRegistry Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/RunRegistry.java`

Methods:

```java
String create(StartRunRequest request)
void markRunning(String runId)
void markExited(String runId, int exitCode)
void markFailed(String runId, String message)
void markStopped(String runId)
```

Verification:

```bash
mvn test
```

### RUN-M010 Test RunRegistry Illegal Transition

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/runtime/RunRegistryTest.java`

Implementation:

1. Create run.
2. Mark stopped.
3. Attempt mark running.
4. Assert failure.

Verification:

```bash
mvn test -Dtest=RunRegistryTest
```

### RUN-M011 Define RunEventBus Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/RunEventBus.java`

Methods:

```java
void publish(String runId, RunEventDto event)
SseEmitter subscribe(String runId)
```

Verification:

```bash
mvn test
```

### RUN-M012 Create ProcessOutputPump Skeleton

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/ProcessOutputPump.java`

Method:

```java
void attach(String runId, ManagedProcess process)
```

Verification:

```bash
mvn test
```

### RUN-M013 Test ProcessOutputPump Terminal Event

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/runtime/ProcessOutputPumpTest.java`

Implementation:

1. Use harmless process that exits 0.
2. Assert exactly one `done` event.
3. Use process that exits non-zero.
4. Assert exactly one `error` event.

Verification:

```bash
mvn test -Dtest=ProcessOutputPumpTest
```

### RUN-M014 Test ProcessOutputPump Non-Zero Exit Failure

Allowed files:

- `src/test/java/com/lqtigee/sparkai/runtime/ProcessOutputPumpTest.java`

Implementation:

1. Start a process that exits with code `7`.
2. Assert run state becomes `FAILED`.
3. Assert exactly one `error` event.
4. Assert no `done` event is emitted.

Verification:

```bash
mvn test -Dtest=ProcessOutputPumpTest
```

## 11. Frontend Foundation Micro Tickets

Frontend tickets are locked until the related backend endpoint exists, except Settings and static shell.

### FE-M001 Create frontend package.json

Allowed new files:

- `frontend/package.json`

Implementation:

1. Set app name `lqtigee`.
2. Add React + TypeScript + Vite.
3. Add scripts:
   - `dev`
   - `build`
   - `typecheck`
4. No UI code.

Verification:

```bash
cd frontend && npm install && npm run typecheck
```

### FE-M002 Create frontend index.html

Allowed new files:

- `frontend/index.html`

Implementation:

1. Title `Lqtigee`.
2. Viewport mobile-ready.
3. Root div.

Verification:

```bash
test -f frontend/index.html
rg "Lqtigee|viewport|root" frontend/index.html
```

### FE-M003 Create frontend tsconfig

Allowed new files:

- `frontend/tsconfig.json`

Implementation:

1. Set `target` to `ES2020`.
2. Set `module` to `ESNext`.
3. Set `jsx` to `react-jsx`.
4. Include `src`.
5. No UI code.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M004 Add frontend module script

Allowed files:

- `frontend/index.html`

Implementation:

1. Add `<script type="module" src="/src/main.tsx"></script>`.
2. Keep title `Lqtigee`.
3. Keep viewport.
4. Keep root div.

Verification:

```bash
rg "/src/main.tsx|Lqtigee|viewport|root" frontend/index.html
```

### FE-M005 Create Global CSS Base

Allowed new files:

- `frontend/src/styles/global.css`

Implementation:

1. Set body margin 0.
2. Set min-width 320px.
3. Set background and text colors.
4. Add no layout cards yet.

Verification:

```bash
test -f frontend/src/styles/global.css
rg "margin|320px|background|color" frontend/src/styles/global.css
```

### FE-M006 Create App Shell Placeholder

Allowed new files:

- `frontend/src/app/App.tsx`

Implementation:

1. Render app shell title `Lqtigee`.
2. Render "Not connected" state.
3. No fake sessions/models.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M007 Create main.tsx

Allowed new files:

- `frontend/src/main.tsx`

Implementation:

1. Render `App`.
2. Import global CSS.
3. No API calls.

Verification:

```bash
cd frontend && npm run build
```

### FE-M008 Define Frontend API Types

Allowed new files:

- `frontend/src/types/api.ts`

Implementation:

1. Define `AgentSource`.
2. Define `SessionStatus`.
3. Define `RunStatus`.
4. Define `RemoteSession`.
5. Define `ModelDto`.
6. Define `ApiErrorDto`.
7. No mock objects.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M009 Create HTTP Client Skeleton

Allowed new files:

- `frontend/src/api/httpClient.ts`

Method:

```ts
requestJson<T>(path: string, init?: RequestInit): Promise<T>
```

Implementation:

1. Read base URL from localStorage.
2. If missing, use current origin.
3. Do not add token yet.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M010 Add HTTP Client Token Requirement

Allowed files:

- `frontend/src/api/httpClient.ts`

Implementation:

1. Read token from `lqtigee_token`.
2. If path is not `/api/health` and token missing, throw client error.
3. Do not call network when token missing.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M011 Add HTTP Client Error Parsing

Allowed files:

- `frontend/src/api/httpClient.ts`

Implementation:

1. Non-2xx parses `ApiErrorDto`.
2. Throw parsed error.
3. No fallback success.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M012 Create Remote API getHealth Only

Allowed new files:

- `frontend/src/api/remoteApi.ts`

Implementation:

1. Export `getHealth()`.
2. Calls `/api/health`.
3. No other methods.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M013 Add Remote API listModels Only

Allowed files:

- `frontend/src/api/remoteApi.ts`

Implementation:

1. Export `listModels()`.
2. Calls `/api/models`.
3. No hardcoded models.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M014 Add Remote API listSessions Only

Allowed files:

- `frontend/src/api/remoteApi.ts`

Implementation:

1. Export `listSessions()`.
2. Calls `/api/sessions`.
3. No hardcoded sessions.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M014B Define Frontend Run API Types

Allowed files:

- `frontend/src/types/api.ts`

Implementation:

1. Define `CommandMode` exactly as `ASK`, `EDIT`, `REVIEW`, `SHELL`.
2. Define `StartRunRequest` fields:
   - `sessionId: string`
   - `source: AgentSource`
   - `modelId: string`
   - `mode: CommandMode`
   - `prompt: string`
   - `confirmDangerous: boolean`
3. Define `StartRunResponse` fields:
   - `runId: string`
   - `sessionId: string`
   - `source: AgentSource`
   - `status: RunStatus`
   - `startedAt: string`
4. Define `RunEventDto` fields:
   - `runId: string`
   - `type: string`
   - `message: string`
   - `timestamp: string`
   - `data: Record<string, unknown>`
5. Do not add constants, examples, mock responses, or API calls.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M015 Add Remote API startRun Only

Allowed files:

- `frontend/src/api/remoteApi.ts`

Implementation:

1. Export `startRun(request)`.
2. POST `/api/runs`.
3. No fake run id.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M016 Add Remote API stopRun Only

Allowed files:

- `frontend/src/api/remoteApi.ts`

Implementation:

1. Export `stopRun(runId)`.
2. POST `/api/runs/{id}/stop`.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M016B Export Authenticated Request Helpers

Allowed files:

- `frontend/src/api/httpClient.ts`

Implementation:

1. Export `toApiUrl(path)`.
2. Export `getRequiredToken()`.
3. `getRequiredToken()` reads `lqtigee_token`.
4. If token is missing, throw client error.
5. Keep `/api/health` behavior unchanged for `requestJson`.
6. Do not perform network calls.
7. Do not add fallback tokens.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M017 Add Remote API openRunEvents Only

Allowed files:

- `frontend/src/api/remoteApi.ts`

Implementation:

1. Export `openRunEvents(runId, handlers)`.
2. Use `fetch` with `Authorization: Bearer <token>` because browser `EventSource` cannot send Authorization headers.
3. Request `/api/runs/{id}/events`.
4. Parse real `text/event-stream` frames.
5. Parse each event `data` field as `RunEventDto`.
6. Call `handlers.onEvent(event)` for each parsed event.
7. Close the stream on terminal event `done`, `error`, or `stopped`.
8. Surface HTTP, parse, stream, and abort errors to `handlers.onError`.
9. Return an object with `close()` that aborts the fetch stream.
10. Do not synthesize events.
11. Do not report success on HTTP failure.

Verification:

```bash
cd frontend && npm run typecheck
```

## 12. Frontend UI Micro Tickets

### UI-M001 Create ErrorPanel Component

Allowed new files:

- `frontend/src/components/ErrorPanel.tsx`

Implementation:

1. Props: `title`, `error`.
2. Display error code/message/detail.
3. No retry behavior.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M002 Create LoadingBlock Component

Allowed new files:

- `frontend/src/components/LoadingBlock.tsx`

Implementation:

1. Props: `label`.
2. Display loading text.
3. No fake content.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M003 Create StatusBadge Component

Allowed new files:

- `frontend/src/components/StatusBadge.tsx`

Implementation:

1. Props: `status`, `label`.
2. Map known statuses to CSS classes.
3. Unknown status displays as unknown, not success.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M004 Create BottomNav Component

Allowed new files:

- `frontend/src/components/BottomNav.tsx`

Implementation:

1. Links exactly:
   - Overview
   - Sessions
   - Control
   - Runs
   - Settings
2. No business data.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M005 Create SideNav Component

Allowed new files:

- `frontend/src/components/SideNav.tsx`

Implementation:

1. Same links as BottomNav.
2. No business data.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M006 Create AppShell Component

Allowed new files:

- `frontend/src/components/AppShell.tsx`

Implementation:

1. Renders header with `Lqtigee`.
2. Renders SideNav desktop.
3. Renders BottomNav mobile.
4. Renders children.
5. No API calls.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M007 Add Layout CSS For Navigation

Allowed files:

- `frontend/src/styles/global.css`

Implementation:

1. Mobile bottom nav fixed.
2. Desktop side nav at min-width 900px.
3. Content bottom padding includes safe area.
4. No horizontal scroll at 320px.

Verification:

```bash
cd frontend && npm run build
```

### UI-M008 Create SettingsPage Local Storage Form

Allowed new files:

- `frontend/src/pages/SettingsPage.tsx`

Implementation:

1. Fields:
   - base URL
   - token
   - refresh seconds
2. Save to localStorage.
3. No backend path settings.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M009 Create useConnectionState Hook

Allowed new files:

- `frontend/src/state/useConnectionState.ts`

Implementation:

1. States:
   - idle
   - checking
   - connected
   - unauthorized
   - failed
2. Method `checkConnection`.
3. Calls `getHealth`.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M010 Create OverviewPage Connection Only

Allowed new files:

- `frontend/src/pages/OverviewPage.tsx`

Implementation:

1. Use `useConnectionState`.
2. Show loading/error/connected.
3. Do not show adapter cards yet.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M011 Create useModelsState Hook

Allowed new files:

- `frontend/src/state/useModelsState.ts`

Implementation:

1. State: loading/error/models.
2. Method `loadModels`.
3. Calls `listModels`.
4. No fallback models.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M012 Create ModelSelect Component

Allowed new files:

- `frontend/src/components/ModelSelect.tsx`

Implementation:

1. Props:
   - models
   - value
   - onChange
   - source
2. Display only enabled models supporting selected source.
3. Empty list shows disabled select.
4. No hardcoded models.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M013 Create useSessionsState Hook

Allowed new files:

- `frontend/src/state/useSessionsState.ts`

Implementation:

1. State: loading/error/sessions/selectedSessionId.
2. Method `loadSessions`.
3. Method `selectSession`.
4. Calls `listSessions`.
5. No fallback sessions.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M014 Create SessionCard Component

Allowed new files:

- `frontend/src/components/SessionCard.tsx`

Implementation:

1. Props: `session`, `selected`, `onSelect`.
2. Display source/title/workspace/model/status.
3. Do not display if no session prop.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M015 Create SessionDetail Component

Allowed new files:

- `frontend/src/components/SessionDetail.tsx`

Implementation:

1. Props: `session`.
2. Display rawFile, updatedAt, lastMessage.
3. If no session, display "No session selected".
4. No fake detail.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M015B Add Sessions Loaded State

Allowed files:

- `frontend/src/state/useSessionsState.ts`

Implementation:

1. Add `loaded: boolean` to returned state.
2. Initial `loaded` is `false`.
3. Set `loaded` to `true` only after `listSessions()` succeeds.
4. Do not set `loaded` to `true` on request failure.
5. Do not add fallback sessions.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M016 Create SessionsPage

Allowed new files:

- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Use `useSessionsState`.
2. On load, call real API.
3. Show loading/error/empty/success states.
4. Empty state only when API succeeded with empty array.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M017 Create PromptComposer Component

Allowed new files:

- `frontend/src/components/PromptComposer.tsx`

Implementation:

1. Props:
   - prompt
   - mode
   - confirmDangerous
   - disabled
   - onPromptChange
   - onModeChange
   - onConfirmDangerousChange
   - onSubmit
2. Shell mode requires checkbox UI.
3. No API call inside component.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M018 Create ControlPage Validation Only

Allowed new files:

- `frontend/src/pages/ControlPage.tsx`

Implementation:

1. Load sessions and models.
2. Validate:
   - session selected
   - model selected
   - prompt non-empty
   - dangerous confirmed for shell
3. Do not call `startRun` yet.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M019 Add ControlPage startRun Call

Allowed files:

- `frontend/src/pages/ControlPage.tsx`

Implementation:

1. On valid submit, call `startRun`.
2. On success navigate to `/runs?runId=<id>`.
3. On failure show `ErrorPanel`.
4. No fake run id.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M020 Create RunTimeline Component

Allowed new files:

- `frontend/src/components/RunTimeline.tsx`

Implementation:

1. Props: events.
2. Render event type/message/timestamp.
3. No fake initial events.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M021 Create useRunEvents Hook

Allowed new files:

- `frontend/src/state/useRunEvents.ts`

Implementation:

1. Accept runId.
2. Calls `openRunEvents`.
3. Appends real events.
4. Closes on terminal event.
5. Exposes error.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M022 Create RunsPage

Allowed new files:

- `frontend/src/pages/RunsPage.tsx`

Implementation:

1. Read runId from URL.
2. If missing, show "No run selected".
3. If present, use `useRunEvents`.
4. Render `RunTimeline`.

Verification:

```bash
cd frontend && npm run typecheck
```

### UI-M023 Add RunsPage Stop Button

Allowed files:

- `frontend/src/pages/RunsPage.tsx`

Implementation:

1. Add Stop button.
2. Calls `stopRun(runId)`.
3. Disable after terminal state.
4. On failure show `ErrorPanel`.

Verification:

```bash
cd frontend && npm run typecheck
```

## 13. PWA Micro Tickets

### PWA-M001 Add manifest.webmanifest

Allowed new files:

- `frontend/public/manifest.webmanifest`

Implementation:

1. name `Lqtigee`.
2. short_name `Lqtigee`.
3. start_url `/`.
4. display `standalone`.
5. theme/background colors.
6. Reference icons.

Verification:

```bash
test -f frontend/public/manifest.webmanifest
rg "Lqtigee|standalone|start_url" frontend/public/manifest.webmanifest
```

### PWA-M002 Add Icon Placeholders From Real Generated Assets

Allowed new files:

- `frontend/public/icons/icon-192.png`
- `frontend/public/icons/icon-512.png`

Implementation:

1. Add actual PNG files.
2. Do not use broken empty files.

Verification:

```bash
file frontend/public/icons/icon-192.png frontend/public/icons/icon-512.png
```

### PWA-M003 Link Manifest In index.html

Allowed files:

- `frontend/index.html`

Implementation:

1. Add manifest link.
2. Add theme-color.

Verification:

```bash
rg "manifest.webmanifest|theme-color" frontend/index.html
```

### PWA-M004 Add Service Worker File

Allowed new files:

- `frontend/public/sw.js`

Implementation:

1. Cache static shell only.
2. Explicitly bypass requests whose pathname starts `/api/`.
3. Do not cache API responses.

Verification:

```bash
rg "/api/|caches|fetch" frontend/public/sw.js
```

### PWA-M005 Register Service Worker

Allowed files:

- `frontend/src/main.tsx`

Implementation:

1. Register `/sw.js` only in production or after load.
2. Do not block app render.

Verification:

```bash
cd frontend && npm run build
```

### PWA-M006 Add Secure Context Runtime Check

Allowed new files:

- `frontend/src/pwa/secureContext.ts`

Implementation:

1. Export `isSecureContextForInstall(): boolean`.
2. Return `window.isSecureContext === true`.
3. Do not force install prompt.
4. Do not fake installability on HTTP server IP.

Verification:

```bash
cd frontend && npm run typecheck
```

### PWA-M007 Add Install Prompt State Hook

Allowed new files:

- `frontend/src/pwa/useInstallPrompt.ts`

Implementation:

1. Listen for `beforeinstallprompt`.
2. Store event only when secure context is true.
3. Expose `canPrompt`, `promptInstall`, and `installed`.
4. Do not display success unless browser install flow resolves accepted.

Verification:

```bash
cd frontend && npm run typecheck
```

## 14. No-Hardcode Audit Micro Tickets

### AUDIT-M001 Backend No Controller Shortcut Audit

Goal: Detect controller layer violations.

Allowed new files:

- `doc/audit/backend-controller-shortcuts.md`

Implementation:

1. Search controllers for:
   - `Files.`
   - `Path.`
   - `ProcessBuilder`
   - `new File`
2. Record results.
3. Any match must be marked `BLOCKED` unless it is harmless import-free text.

Verification:

```bash
test -f doc/audit/backend-controller-shortcuts.md
```

### AUDIT-M002 Frontend No Mock Audit

Goal: Detect fake business data.

Allowed new files:

- `doc/audit/frontend-no-mock.md`

Implementation:

1. Search frontend for:
   - `mock`
   - `sample`
   - `demo`
   - `fake`
   - hardcoded model ids
2. Record results.
3. Any business-data match must be `BLOCKED`.

Verification:

```bash
test -f doc/audit/frontend-no-mock.md
```

### AUDIT-M003 API Cache Audit

Goal: Ensure PWA does not cache API responses.

Allowed new files:

- `doc/audit/pwa-api-cache.md`

Implementation:

1. Inspect `frontend/public/sw.js`.
2. Confirm `/api/` bypass.
3. Record result.

Verification:

```bash
test -f doc/audit/pwa-api-cache.md
```

### AUDIT-M004 Secret Leak Audit

Goal: Ensure docs and code do not expose secrets, prompt transcripts, or auth files.

Allowed new files:

- `doc/audit/secret-leak.md`

Implementation:

1. Search project files for:
   - `auth.json`
   - `api-token:`
   - `Bearer `
   - full prompt transcript text in discovery docs
   - environment secret values
2. Record command and result.
3. If any real secret or transcript appears, mark `BLOCKED`.
4. Do not print secret values into the audit file.

Verification:

```bash
test -f doc/audit/secret-leak.md
rg "secret leak|auth\\.json|transcript|BLOCKED|PASS" doc/audit/secret-leak.md
```

### AUDIT-M005 Release Checklist Audit

Goal: Prove release checklist status before any release claim.

Allowed new files:

- `doc/audit/release-checklist-status.md`

Implementation:

1. Read `doc/quality/release-checklist.md`.
2. Mark every checklist item as `PASS`, `FAIL`, or `NOT_RUN`.
3. If any item is `FAIL` or `NOT_RUN`, release is blocked.
4. Do not mark release ready by assumption.

Verification:

```bash
test -f doc/audit/release-checklist-status.md
rg "PASS|FAIL|NOT_RUN|release is blocked" doc/audit/release-checklist-status.md
```

### AUDIT-M006 Android PWA Secure Origin Audit

Goal: Verify final phone URL can actually support Android PWA installation.

Allowed new files:

- `doc/audit/android-pwa-secure-origin.md`

Implementation:

1. Record final Android URL without secrets.
2. On Android Chrome, open the final URL.
3. Verify `window.isSecureContext === true`.
4. Verify manifest is loaded.
5. Verify service worker is registered.
6. Verify install option appears.
7. If final URL is `http://<server-ip>:20261`, mark `BLOCKED`.

Verification:

```bash
test -f doc/audit/android-pwa-secure-origin.md
rg "secure context|manifest|service worker|install option|BLOCKED|PASS" doc/audit/android-pwa-secure-origin.md
```

## 15. Post-Audit Delivery Closure Micro Tickets

These tickets exist because `AUDIT-M005` proved the previous queue could reach its end while release remained blocked.

Rules for this section:

- Complete exactly one ticket per pass.
- Commit and push after each ticket.
- Do not skip a blocker because a skeleton exists.
- Do not use mock business data.
- Do not execute Codex or opencode as a live release gate.
- Lqtigee-owned persistence is PostgreSQL.
- opencode session discovery still reads opencode-owned SQLite as an external source.
- PostgreSQL must not replace Codex JSONL discovery.
- PostgreSQL must not replace opencode SQLite discovery.

### PLANFIX-M001 Add Post-Audit Delivery Tickets

Goal: Convert `AUDIT-M005` release blockers into executable micro tickets.

Allowed files:

- `doc/micro-tickets.md`
- `doc/plan.md`

Implementation:

1. Add tickets for the exact blockers listed in `doc/audit/release-checklist-status.md`.
2. Add dependency tickets needed to unblock those blockers.
3. Keep every ticket scoped to one method, class, endpoint, page mount, or audit.
4. State PostgreSQL remains the application database.
5. State opencode SQLite remains an external session source.
6. Do not edit Java or frontend implementation files.

Verification:

```bash
rg "PLANFIX-M001|DELIVERY-M001|OPENCODE-JDBC-M001|RUN-API-M001|APP-WIRE-M001|RELEASE-AUDIT-M001" doc/micro-tickets.md doc/plan.md
```

### DELIVERY-M001 Add Delivery Blocker Tracker

Goal: Create a short tracker that maps each release blocker to the ticket that will remove it.

Allowed new files:

- `doc/quality/post-audit-delivery-tracker.md`

Implementation:

1. Read `doc/audit/release-checklist-status.md`.
2. List each blocker exactly once.
3. Assign one or more ticket ids from this section to each blocker.
4. Mark the initial status as `OPEN`.
5. Do not mark any blocker closed in this ticket.

Verification:

```bash
test -f doc/quality/post-audit-delivery-tracker.md
rg "OPEN|CodexAdapter|OpencodeAdapter|RunController|App.tsx|Android" doc/quality/post-audit-delivery-tracker.md
```

### OPENCODE-JDBC-M001 Add SQLite JDBC Dependency For External opencode Source

Goal: Make `jdbc:sqlite:` usable for reading opencode-owned session data.

Allowed files:

- `pom.xml`

Implementation:

1. Add `org.xerial:sqlite-jdbc` as a runtime dependency.
2. Keep `org.postgresql:postgresql` dependency.
3. Do not add JPA.
4. Do not change application persistence from PostgreSQL.
5. Do not edit `OpencodeSqliteSessionReader` in this ticket.

Verification:

```bash
mvn test
rg "sqlite-jdbc|postgresql" pom.xml
```

### OPENCODE-ERR-M001 Add Opencode Schema Mismatch Error Code

Goal: Add the error code required by opencode SQLite schema guard tickets.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/error/ErrorCode.java`

Implementation:

1. Add `OPENCODE_SESSION_SCHEMA_MISMATCH`.
2. Keep existing enum names unchanged.
3. Do not change exception mapping in this ticket.

Verification:

```bash
mvn test
rg "OPENCODE_SESSION_SCHEMA_MISMATCH" src/main/java/com/lqtigee/sparkai/error/ErrorCode.java
```

### OPENCODE-READ-M001 Extract Opencode SQLite Schema Validation Method

Goal: Add a focused method that validates the opencode `session` table columns.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`

Implementation:

1. Add method `void validateSchema(Connection connection)`.
2. Query `PRAGMA table_info(session)`.
3. Require columns:
   - `id`
   - `directory`
   - `title`
   - `model`
   - `time_updated`
   - `time_archived`
   - `path`
   - `agent`
4. Missing column throws `ApiException` with `OPENCODE_SESSION_SCHEMA_MISMATCH`.
5. Keep `readSessions(Path databasePath)` behavior unchanged except calling this method before the existing unsupported-operation point.
6. Do not query rows in this ticket.

Verification:

```bash
mvn test
rg "validateSchema|PRAGMA table_info|OPENCODE_SESSION_SCHEMA_MISMATCH" src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java
```

### OPENCODE-READ-M002 Test Opencode SQLite Schema Validation

Goal: Prove schema validation passes and fails deterministically.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSchemaGuardTest.java`

Implementation:

1. Create a temporary SQLite database with required `session` columns.
2. Open a connection to the database.
3. Call `validateSchema(connection)`.
4. Assert no exception for the complete schema.
5. Create another database missing `model`.
6. Assert `OPENCODE_SESSION_SCHEMA_MISMATCH`.
7. Do not insert prompt or message content.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteSchemaGuardTest
```

### OPENCODE-READ-M003 Extract Opencode Model Text From JSON

Goal: Parse the model field from opencode `session.model` JSON without guessing missing values.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`

Implementation:

1. Add method `String extractModel(String modelJson)`.
2. Use Jackson `ObjectMapper`.
3. Accept a textual `id` field when present.
4. If `id` is missing, accept textual `model` only if the real sample proves it exists.
5. If no proven model field exists, throw `OPENCODE_SESSION_FIELD_MISSING`.
6. Do not fallback to provider name, title, or filename.

Verification:

```bash
mvn test
rg "extractModel|ObjectMapper|OPENCODE_SESSION_FIELD_MISSING" src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java
```

### OPENCODE-READ-M004 Query Opencode Session Rows

Goal: Read opencode session rows from the `session` table.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`

Implementation:

1. Replace the final unsupported-operation throw in `readSessions`.
2. Query only:
   - `id`
   - `directory`
   - `title`
   - `model`
   - `time_updated`
   - `time_archived`
   - `path`
   - `agent`
3. Order by `time_updated DESC`.
4. Do not read message, prompt, event, log, or transcript tables.
5. Return mapped DTOs.
6. If required fields are missing, throw `OPENCODE_SESSION_FIELD_MISSING`.

Verification:

```bash
mvn test
rg "SELECT id, directory, title, model, time_updated, time_archived, path, agent" src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java
```

### OPENCODE-READ-M005 Test Opencode Reader Success Fixture

Goal: Prove `readSessions` maps a sanitized SQLite row into `RemoteSessionDto`.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReaderTest.java`

Implementation:

1. Create a temporary SQLite database.
2. Create only the `session` table.
3. Insert one sanitized row with required fields.
4. Call `readSessions(databasePath)`.
5. Assert:
   - one session returned
   - source is `OPENCODE`
   - id, title, workspace, model, updatedAt, rawFile are populated
   - `lastMessage` is empty
6. Do not insert prompt or transcript content.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteSessionReaderTest
```

### OPENCODE-READ-M006 Test Opencode Reader Missing Field Failure

Goal: Prove missing required opencode session fields fail instead of producing fallback DTOs.

Allowed files:

- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReaderTest.java`

Implementation:

1. Add a test with a row missing one required value.
2. Call `readSessions(databasePath)`.
3. Assert `OPENCODE_SESSION_FIELD_MISSING`.
4. Do not change production code in this ticket unless the test exposes a narrow bug.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteSessionReaderTest
```

### CODEX-ADAPTER-M001 Wire CodexAdapter Scanner And Parser

Goal: Prepare `CodexAdapter` to use existing scanner and parser without starting discovery yet.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/adapter/CodexAdapter.java`

Implementation:

1. Add fields for `CodexFileScanner` and `CodexJsonlParser`.
2. Add a default constructor that creates those dependencies.
3. Add package-private constructor for tests.
4. Keep `probe()` behavior unchanged.
5. Keep `discoverSessions()` throwing unsupported in this ticket.

Verification:

```bash
mvn test
rg "CodexFileScanner|CodexJsonlParser" src/main/java/com/lqtigee/sparkai/adapter/CodexAdapter.java
```

### CODEX-ADAPTER-M002 Implement CodexAdapter discoverSessions

Goal: Return real Codex sessions from JSONL files under the discovered Codex home.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/adapter/CodexAdapter.java`

Implementation:

1. Call `CodexFileScanner.scan(CODEX_HOME)`.
2. For each returned path, call `CodexJsonlParser.parse(path)`.
3. Return parsed DTOs in scanner order.
4. If scan or parse fails, propagate the typed `ApiException`.
5. Do not catch parser errors and return an empty list.
6. Do not read transcript text outside parser-proven fields.

Verification:

```bash
mvn test
rg "discoverSessions|scanner.scan|parser.parse" src/main/java/com/lqtigee/sparkai/adapter/CodexAdapter.java
```

### SESS-M012A Decouple SessionController Test From Real Adapters

Goal: Keep the Web auth/routing test deterministic after real session adapters are wired.

Allowed files:

- `src/test/java/com/lqtigee/sparkai/web/SessionControllerTest.java`

Implementation:

1. Replace the real `SessionService` bean with a Mockito-backed test bean.
2. For the valid-token test, make `SessionService.listAllSessions()` return `List.of()`.
3. Assert the HTTP response is `200 OK`.
4. Assert `$.sessions` is an empty array.
5. Verify `SessionService.listAllSessions()` was called exactly once.
6. Keep the missing-token assertion unchanged.
7. Do not call real Codex or opencode adapters from this Web-layer test.
8. Do not assert adapter-specific failure codes in this Web-layer test.

Verification:

```bash
mvn test -Dtest=SessionControllerTest
```

### CODEX-ADAPTER-M003 Test CodexAdapter discoverSessions

Goal: Prove `CodexAdapter.discoverSessions()` uses scanner output and parser output.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/adapter/CodexAdapterTest.java`

Implementation:

1. Use package-private constructor from `CODEX-ADAPTER-M001`.
2. Provide deterministic test scanner behavior with a temp Codex home.
3. Use sanitized JSONL fixture content.
4. Assert returned DTO source is `CODEX`.
5. Add a parser-failure test.
6. Assert failure is not converted to empty success.

Verification:

```bash
mvn test -Dtest=CodexAdapterTest
```

### OPENCODE-ADAPTER-M001 Wire OpencodeAdapter Reader

Goal: Prepare `OpencodeAdapter` to use `OpencodeSqliteSessionReader`.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/adapter/OpencodeAdapter.java`

Implementation:

1. Add field for `OpencodeSqliteSessionReader`.
2. Add a default constructor that creates the reader.
3. Add package-private constructor for tests.
4. Keep `probe()` behavior unchanged.
5. Keep `discoverSessions()` throwing unsupported in this ticket.

Verification:

```bash
mvn test
rg "OpencodeSqliteSessionReader" src/main/java/com/lqtigee/sparkai/adapter/OpencodeAdapter.java
```

### OPENCODE-ADAPTER-M002 Implement OpencodeAdapter discoverSessions

Goal: Return real opencode sessions from the opencode-owned SQLite database.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/adapter/OpencodeAdapter.java`

Implementation:

1. Call `OpencodeSqliteSessionReader.readSessions(OPENCODE_DB)`.
2. Return the reader result.
3. Propagate typed `ApiException`.
4. Do not scan prompt history.
5. Do not return empty success when SQLite open, schema, or field validation fails.

Verification:

```bash
mvn test
rg "readSessions\\(OPENCODE_DB\\)" src/main/java/com/lqtigee/sparkai/adapter/OpencodeAdapter.java
```

### OPENCODE-ADAPTER-M003 Test OpencodeAdapter discoverSessions Failure Rule

Goal: Prove opencode adapter does not hide reader failure.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/adapter/OpencodeAdapterTest.java`

Implementation:

1. Use package-private constructor from `OPENCODE-ADAPTER-M001`.
2. Make the reader throw a typed `ApiException`.
3. Assert `discoverSessions()` throws the same typed failure.
4. Do not create fake sessions.

Verification:

```bash
mvn test -Dtest=OpencodeAdapterTest
```

### SESSION-FIX-M001 Implement SessionService getRequiredSession

Goal: Allow runtime start to resolve the selected session by source and id.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/SessionService.java`

Implementation:

1. Call `listBySource(source)`.
2. Match `RemoteSessionDto.id()` exactly.
3. Return the matching DTO.
4. If no match exists, throw `SESSION_NOT_FOUND`.
5. Do not fallback to the first session.
6. Do not search the other source.

Verification:

```bash
mvn test
rg "getRequiredSession|SESSION_NOT_FOUND" src/main/java/com/lqtigee/sparkai/service/SessionService.java
```

### SESSION-FIX-M002 Test SessionService getRequiredSession

Goal: Prove exact selected-session lookup behavior.

Allowed files:

- `src/test/java/com/lqtigee/sparkai/service/SessionServiceTest.java`

Implementation:

1. Add success test for exact id and source.
2. Add missing id test.
3. Assert missing id throws `SESSION_NOT_FOUND`.
4. Do not use fake prompt transcript data.

Verification:

```bash
mvn test -Dtest=SessionServiceTest
```

### RUN-DTO-M001 Add StopRunResponse DTO

Goal: Add the response shape for `POST /api/runs/{runId}/stop`.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/dto/StopRunResponse.java`

Implementation:

1. Create record `StopRunResponse(String runId, RunStatus status)`.
2. Do not add controller logic.

Verification:

```bash
mvn test
test -f src/main/java/com/lqtigee/sparkai/dto/StopRunResponse.java
```

### RUN-REGISTRY-M001 Store ManagedProcess In RunRegistry

Goal: Let stop logic locate the process for a run id.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/RunRegistry.java`

Implementation:

1. Add `ManagedProcess` to internal run state.
2. Add method `void attachProcess(String runId, ManagedProcess process)`.
3. Add method `ManagedProcess getRequiredProcess(String runId)`.
4. Preserve existing status transitions.
5. Missing run throws `RUN_NOT_FOUND`.
6. Finished run throws `RUN_ALREADY_FINISHED` when process access is requested for stop.

Verification:

```bash
mvn test
rg "attachProcess|getRequiredProcess|ManagedProcess" src/main/java/com/lqtigee/sparkai/runtime/RunRegistry.java
```

### RUN-REGISTRY-M002 Test ManagedProcess Storage

Goal: Prove registry can attach and retrieve a process without changing illegal transition behavior.

Allowed files:

- `src/test/java/com/lqtigee/sparkai/runtime/RunRegistryTest.java`

Implementation:

1. Create a run.
2. Attach a deterministic `ManagedProcess` from a harmless local process.
3. Assert `getRequiredProcess` returns it.
4. Mark run terminal.
5. Assert process access reports already terminal.

Verification:

```bash
mvn test -Dtest=RunRegistryTest
```

### RUN-SERVICE-M001 Create RunService Skeleton

Goal: Centralize run orchestration outside controllers.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`

Implementation:

1. Constructor accepts:
   - `SessionService`
   - `ModelService`
   - `CodexCommandBuilder`
   - `OpencodeCommandBuilder`
   - `ProcessLauncher`
   - `ProcessOutputPump`
   - `RunRegistry`
2. Add methods:

```java
StartRunResponse start(StartRunRequest request)
SseEmitter events(String runId)
StopRunResponse stop(String runId)
```

3. Method bodies may throw `UnsupportedOperationException`.
4. Do not add controller in this ticket.

Verification:

```bash
mvn test
test -f src/main/java/com/lqtigee/sparkai/service/RunService.java
```

### RUN-SERVICE-M002 Validate StartRunRequest

Goal: Validate prompt and required fields before command construction.

Blocked until:

- `REMOTE-CONFIG-M001`

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`

Implementation:

1. Constructor accepts `RemoteProperties`.
2. Store it in a final field.
3. Call `remoteProperties.validate()` in the constructor.
4. Add private method `validateRequest(StartRunRequest request)`.
5. Null request throws `VALIDATION_FAILED`.
6. Blank prompt throws `PROMPT_EMPTY`.
7. Prompt longer than `remoteProperties.getMaxPromptChars()` throws `PROMPT_TOO_LONG`.
8. Null source, session id, model id, or mode throws `VALIDATION_FAILED`.
9. Do not start a process in this ticket.
10. Do not introduce any hardcoded prompt length outside `RemoteProperties`.

Verification:

```bash
mvn test
rg "RemoteProperties|getMaxPromptChars|validateRequest|PROMPT_EMPTY|PROMPT_TOO_LONG" src/main/java/com/lqtigee/sparkai/service/RunService.java
```

### REMOTE-CONFIG-M001 Add RemoteProperties Max Prompt Config

Goal: Provide a real configured prompt length limit for run validation.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/config/RemoteProperties.java`

Allowed files:

- `src/main/resources/application.yml`

Implementation:

1. Create `RemoteProperties` with prefix `lqtigee.remote`.
2. Add integer field `maxPromptChars`.
3. Add getter and setter.
4. Add method `validate()` that throws `VALIDATION_FAILED` if `maxPromptChars <= 0`.
5. Add `lqtigee.remote.max-prompt-chars: 8000` to `application.yml`.
6. Do not add Codex/opencode path settings in this ticket.
7. Do not use hardcoded prompt limits outside this config.

Verification:

```bash
mvn test
rg "maxPromptChars|max-prompt-chars|8000" src/main/java/com/lqtigee/sparkai/config/RemoteProperties.java src/main/resources/application.yml
```

### RUN-SERVICE-M003 Build CommandSpec By Source

Goal: Convert a valid start request into the source-specific command spec.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`

Implementation:

1. Resolve session with `SessionService.getRequiredSession(request.source(), request.sessionId())`.
2. Resolve model with `ModelService.getRequiredModel(request.modelId())`.
3. Call `ModelService.validateModelForSource(request.modelId(), request.source())`.
4. For `CODEX`, call `CodexCommandBuilder.build(request, session, model)`.
5. For `OPENCODE`, call `OpencodeCommandBuilder.build(request, session, model)`.
6. Do not start a process in this ticket.

Verification:

```bash
mvn test
rg "CodexCommandBuilder|OpencodeCommandBuilder|validateModelForSource" src/main/java/com/lqtigee/sparkai/service/RunService.java
```

### RUN-SERVICE-M004 Implement RunService start

Goal: Create a run, launch the process, attach output pump, and return the API response.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`

Implementation:

1. Validate the request.
2. Build `CommandSpec`.
3. Create a run id with `RunRegistry.create(request)`.
4. Launch with `ProcessLauncher.start(runId, spec)`.
5. Attach process to registry.
6. Mark run running.
7. Attach `ProcessOutputPump`.
8. Return `StartRunResponse` with status `RUNNING` or the documented created status if contract is updated first.
9. If process start fails, mark run failed and throw the typed error.
10. Do not claim final CLI success at process start.

Verification:

```bash
mvn test
rg "start\\(StartRunRequest|ProcessLauncher|ProcessOutputPump|StartRunResponse" src/main/java/com/lqtigee/sparkai/service/RunService.java
```

### RUN-SERVICE-M005 Implement RunService events

Goal: Subscribe callers to run events through the event bus.

Blocked until:

- `RUN-REGISTRY-M003`

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`

Implementation:

1. Verify run id exists through `RunRegistry`.
2. Return `RunEventBus.subscribe(runId)`.
3. Convert subscribe failure to `SSE_SUBSCRIBE_FAILED`.
4. Do not synthesize fake events.

Verification:

```bash
mvn test
rg "events\\(|SseEmitter|SSE_SUBSCRIBE_FAILED" src/main/java/com/lqtigee/sparkai/service/RunService.java
```

### RUN-REGISTRY-M003 Expose RunRegistry Status Lookup

Goal: Allow services outside the runtime package to verify a run id exists before subscribing to events.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/RunRegistry.java`

Implementation:

1. Make `RunRegistry.statusOf(String runId)` public.
2. Preserve `RUN_NOT_FOUND` behavior.
3. Do not change status transition rules.
4. Do not add event subscription logic in this ticket.

Verification:

```bash
mvn test
rg "public RunStatus statusOf" src/main/java/com/lqtigee/sparkai/runtime/RunRegistry.java
```

### RUN-SERVICE-M006 Implement RunService stop

Goal: Stop a running process or report that it is already terminal.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`

Implementation:

1. Get the process from `RunRegistry.getRequiredProcess(runId)`.
2. If process is alive, call `destroy()`.
3. Wait a short bounded time.
4. If still alive, call `destroyForcibly()`.
5. Mark run stopped.
6. Publish one `stopped` terminal event.
7. Return `StopRunResponse`.
8. If run is already terminal, throw `RUN_ALREADY_FINISHED`.
9. Do not hide process stop failures.

Verification:

```bash
mvn test
rg "destroyForcibly|StopRunResponse|stopped" src/main/java/com/lqtigee/sparkai/service/RunService.java
```

### RUN-SERVICE-M007 Test RunService Validation

Goal: Prove invalid start requests fail before any process is launched.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/service/RunServiceTest.java`

Implementation:

1. Blank prompt throws `PROMPT_EMPTY`.
2. Over-limit prompt throws `PROMPT_TOO_LONG`.
3. Missing source or session id throws `VALIDATION_FAILED`.
4. Verify launcher is not called for invalid requests.
5. Do not use fake business session data beyond minimal DTOs required for service unit tests.

Verification:

```bash
mvn test -Dtest=RunServiceTest
```

### RUN-API-M001 Add RunController start Endpoint

Goal: Expose `POST /api/runs`.

Blocked until:

- `RUN-WIRE-M001`

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/web/RunController.java`

Implementation:

1. Add `@PostMapping("/api/runs")`.
2. Accept `StartRunRequest`.
3. Return `RunService.start(request)`.
4. Controller must not build commands.
5. Controller must not use `ProcessBuilder`.
6. Controller must not read files.

Verification:

```bash
mvn test
rg "PostMapping\\(\"/api/runs\"\\)|RunService" src/main/java/com/lqtigee/sparkai/web/RunController.java
```

### RUN-WIRE-M001 Add Run Runtime Spring Beans

Goal: Make `RunService` and its runtime collaborators injectable before adding `RunController`.

Allowed new files:

- `src/main/java/com/lqtigee/sparkai/runtime/RunRuntimeConfig.java`

Implementation:

1. Add `@Configuration`.
2. Enable `RemoteProperties`.
3. Add beans for:
   - `CodexCommandBuilder`
   - `OpencodeCommandBuilder`
   - `ProcessLauncher`
   - `RunEventBus`
   - `RunRegistry`
   - `ProcessOutputPump`
   - `RunService`
4. Construct `ProcessOutputPump` with both `RunEventBus` and `RunRegistry` so process exit updates registry state.
5. Construct `RunService` with Spring-provided `SessionService`, `ModelService`, runtime beans, and `RemoteProperties`.
6. Do not start any process in this ticket.
7. Do not add controller endpoints in this ticket.

Verification:

```bash
mvn test
rg "@Bean|RunService|ProcessOutputPump|RunRegistry|RemoteProperties" src/main/java/com/lqtigee/sparkai/runtime/RunRuntimeConfig.java
```

### RUN-API-M002 Add RunController events Endpoint

Goal: Expose authenticated SSE stream for a run.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/web/RunController.java`

Implementation:

1. Add `@GetMapping("/api/runs/{runId}/events")`.
2. Return `RunService.events(runId)`.
3. Produce `text/event-stream`.
4. Do not use browser `EventSource` assumptions in backend.
5. Do not synthesize fake events in controller.

Verification:

```bash
mvn test
rg "text/event-stream|/api/runs/\\{runId\\}/events|events\\(runId\\)" src/main/java/com/lqtigee/sparkai/web/RunController.java
```

### RUN-API-M003 Add RunController stop Endpoint

Goal: Expose `POST /api/runs/{runId}/stop`.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/web/RunController.java`

Implementation:

1. Add `@PostMapping("/api/runs/{runId}/stop")`.
2. Return `RunService.stop(runId)`.
3. Controller must not call `Process.destroy()` directly.
4. Controller must not catch and convert failures to success.

Verification:

```bash
mvn test
rg "/api/runs/\\{runId\\}/stop|stop\\(runId\\)" src/main/java/com/lqtigee/sparkai/web/RunController.java
```

### RUN-API-M004 Test RunController Auth And Delegation

Goal: Prove run endpoints are protected and delegate to service.

Allowed new files:

- `src/test/java/com/lqtigee/sparkai/web/RunControllerTest.java`

Implementation:

1. Missing token on `POST /api/runs` returns 401.
2. Wrong token on events endpoint returns 401.
3. Missing token on stop endpoint returns 401.
4. Valid token reaches service for start.
5. Do not start real Codex or opencode processes in controller tests.

Verification:

```bash
mvn test -Dtest=RunControllerTest
```

### APP-WIRE-M001 Add Frontend Route Resolver

Goal: Select the correct page component from the current path.

Allowed files:

- `frontend/src/app/App.tsx`

Implementation:

1. Add function `resolvePage(pathname: string)`.
2. Map:
   - `/` to `OverviewPage`
   - `/sessions` to `SessionsPage`
   - `/control` to `ControlPage`
   - `/runs` to `RunsPage`
   - `/settings` to `SettingsPage`
3. Unknown paths render `OverviewPage`.
4. Do not add a router dependency.
5. Do not hardcode sessions, models, or runs.

Verification:

```bash
cd frontend && npm install && npm run typecheck && npm run build
rm -rf node_modules package-lock.json dist
```

### APP-WIRE-M002 Mount AppShell In App

Goal: Make the implemented navigation and pages reachable.

Allowed files:

- `frontend/src/app/App.tsx`

Implementation:

1. Import `AppShell`.
2. Render the resolved page inside `AppShell`.
3. Remove placeholder-only `Not connected` markup.
4. Do not change page internals.

Verification:

```bash
cd frontend && npm install && npm run typecheck && npm run build
rm -rf node_modules package-lock.json dist
```

### APP-WIRE-M003 Add Active Navigation State

Goal: Let users see the current section without adding app state.

Allowed files:

- `frontend/src/components/BottomNav.tsx`
- `frontend/src/components/SideNav.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Read `window.location.pathname`.
2. Add `aria-current="page"` to the matching link.
3. Style active link with contrast that works on mobile and desktop.
4. Do not use localStorage for navigation.
5. Do not introduce a router dependency.

Verification:

```bash
cd frontend && npm install && npm run typecheck && npm run build
rm -rf node_modules package-lock.json dist
```

### APP-WIRE-M004 Add Frontend 360px Layout Audit

Goal: Replace the previous `NOT_RUN` mobile-width item with a repeatable local audit.

Allowed new files:

- `doc/audit/frontend-360-layout.md`

Implementation:

1. Build the frontend.
2. Inspect CSS for `min-width`, fixed widths, and bottom navigation overflow risk.
3. If browser tooling is available, run a 360px viewport check.
4. If browser tooling is not available, mark browser screenshot as `NOT_RUN`.
5. Mark any known horizontal overflow as `BLOCKED`.
6. Remove generated frontend files after verification.

Verification:

```bash
test -f doc/audit/frontend-360-layout.md
rg "360|horizontal|PASS|NOT_RUN|BLOCKED" doc/audit/frontend-360-layout.md
```

### RELEASE-AUDIT-M001 Re-run Release Checklist After Delivery Fixes

Goal: Recalculate release status after session, run, and frontend wiring blockers are fixed.

Allowed files:

- `doc/audit/release-checklist-status.md`

Implementation:

1. Re-run `mvn test`.
2. Re-run frontend typecheck and build.
3. Start backend on port `20261`.
4. Verify health and auth behavior.
5. Verify `/api/sessions` returns real sessions or a typed dependency failure.
6. Verify `/api/runs` endpoint exists and does not return mock data.
7. Verify frontend pages are reachable through `App.tsx`.
8. Keep Android secure-origin items blocked unless `AUDIT-M006` passes.
9. Do not mark release ready by assumption.
10. Remove generated frontend files after verification.

Verification:

```bash
test -f doc/audit/release-checklist-status.md
rg "PASS|FAIL|NOT_RUN|release is blocked|release is ready" doc/audit/release-checklist-status.md
```

### RELEASE-AUDIT-M002 Re-run Android Secure Origin Audit With Final URL

Goal: Convert Android installability from blocked to pass only with a real final URL.

Allowed files:

- `doc/audit/android-pwa-secure-origin.md`

Implementation:

1. Record the final Android URL without secrets.
2. If it is `http://<server-ip>:20261`, keep `BLOCKED`.
3. If it is HTTPS, open it on Android Chrome.
4. Verify secure context.
5. Verify manifest.
6. Verify service worker.
7. Verify install option.
8. Do not mark `PASS` from desktop-only checks.

Verification:

```bash
test -f doc/audit/android-pwa-secure-origin.md
rg "secure context|manifest|service worker|install option|BLOCKED|PASS" doc/audit/android-pwa-secure-origin.md
```

### BUG-OPENCODE-SESSION-MODEL-001 Document Empty Model Id Blocking Rows

Symptom:

Live `/api/opencode/sessions` returns HTTP 422 with `OPENCODE_SESSION_FIELD_MISSING` and `detail=session.model.id`.

Expected:

- Valid opencode sessions with a proven model id are listed.
- opencode sessions without a recoverable model id do not become fake runnable sessions.
- Unified `/api/sessions` still does not return partial success unless the backend API contract is explicitly changed first.

Actual:

- The local opencode database contains four non-archived `session` rows whose `session.model` JSON has an empty `id`.
- The same rows have `message.data.model.providerID`, `event.data.model.providerID`, or `session_message.data.model.providerID`, but no textual `modelID` or `id`.
- Treating `providerID` as the model would violate the existing no-fallback rule.

Allowed files:

- `doc/discovery/opencode-empty-model-id.md`
- `doc/micro-tickets.md`

Failing verification:

```bash
curl -H "Authorization: Bearer <token>" http://127.0.0.1:20261/api/opencode/sessions
```

Implementation:

1. Create `doc/discovery/opencode-empty-model-id.md`.
2. Record only schema/metadata evidence; do not copy prompts, message text, transcript parts, or secrets.
3. Include the observed `session.model` JSON patterns with counts.
4. Include whether `message`, `event`, and `session_message` metadata contain a recoverable `modelID` or `id`.
5. State that `providerID` alone is not enough to construct the model under the current contract.
6. Add a follow-up implementation ticket after this ticket:
   - It must choose exactly one behavior before code changes:
     - keep typed failure for unrecoverable rows, or
     - update the backend contract to allow excluding non-runnable opencode sessions from list results.
   - It must not fallback to provider name, title, workspace, filename, or configured default model.

Verification:

```bash
test -f doc/discovery/opencode-empty-model-id.md
rg "OPENCODE_SESSION_FIELD_MISSING|empty id|providerID alone|no fallback|follow-up" doc/discovery/opencode-empty-model-id.md doc/micro-tickets.md
```

### BUG-OPENCODE-SESSION-MODEL-002 Decide Empty Model Id Handling Contract

Symptom:

`doc/discovery/opencode-empty-model-id.md` proves four live opencode session rows have no recoverable non-empty `model.id` or `modelID`.

Expected:

The project has one explicit contract for these rows before parser code changes.

Actual:

The current contract requires `RemoteSessionDto.model`, forbids partial unified success, and forbids fallback values. Under that contract, the existing typed failure is correct but keeps release blocked.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/implementation-design.md`
- `doc/micro-tickets.md`

Failing verification:

```bash
rg "OPENCODE_SESSION_FIELD_MISSING|providerID alone|Current Contract Impact" doc/discovery/opencode-empty-model-id.md
```

Implementation:

1. Choose exactly one documented behavior:
   - keep typed failure for unrecoverable opencode rows, or
   - define an explicit non-runnable exclusion rule for opencode rows whose model id cannot be recovered.
2. If choosing exclusion, update the API contract before any Java code:
   - state that exclusion is allowed only for rows with empty `session.model.id` and no non-empty `modelID` in inspected metadata,
   - state that this is not parser failure hiding,
   - state that no fake model is created,
   - state how audit evidence must report excluded row count.
3. Update implementation design with the same behavior.
4. Add the next tiny Java implementation ticket matching the chosen behavior.
5. Do not change Java code in this ticket.

Verification:

```bash
rg "empty session.model.id|non-runnable|excluded row count|no fake model|OPENCODE_SESSION_FIELD_MISSING" doc/contracts/backend-api-contract.md doc/implementation-design.md doc/micro-tickets.md
```

### BUG-OPENCODE-SESSION-MODEL-003 Exclude Non-Runnable Empty Model Rows

Symptom:

Live `/api/opencode/sessions` is blocked by rows with empty `session.model.id` and no recoverable `modelID`.

Expected:

- Rows with a proven non-empty model id are returned.
- Rows with empty `session.model.id` and no non-empty `modelID` in inspected metadata are excluded as non-runnable.
- No fake model is created.
- Other missing required fields still fail with `OPENCODE_SESSION_FIELD_MISSING`.

Actual:

`OpencodeSqliteSessionReader.extractModel()` throws `OPENCODE_SESSION_FIELD_MISSING` for every empty `session.model.id`, including non-runnable rows documented in `doc/discovery/opencode-empty-model-id.md`.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java`
- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReaderTest.java`
- `doc/audit/release-checklist-status.md`

Failing verification:

```bash
mvn test -Dtest=OpencodeSqliteSessionReaderTest
```

Implementation:

1. Add a private helper that detects empty `session.model.id`.
2. For empty-id rows, check only metadata already proven in `doc/discovery/opencode-empty-model-id.md`; do not read prompt, part text, or transcript fields.
3. If no non-empty `modelID` or `id` exists in inspected metadata, exclude the row from returned DTOs.
4. Do not use `providerID` alone as model.
5. Do not fallback to title, workspace, filename, configured default model, or provider name.
6. Keep `OPENCODE_SESSION_FIELD_MISSING` for non-excluded rows with missing required fields.
7. Add tests:
   - empty `session.model.id` with no metadata model id is excluded,
   - empty `session.model.id` is not converted to provider-only model,
   - normal rows still map to `providerID/id`.
8. Update release checklist status to report excluded row count instead of marking this as parser success by assumption.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteSessionReaderTest
rg "exclude|non-runnable|providerID|OPENCODE_SESSION_FIELD_MISSING" src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReader.java src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteSessionReaderTest.java doc/audit/release-checklist-status.md
```

## 11. Remaining Release Evidence Micro Tickets

These tickets exist because `BUG-OPENCODE-SESSION-MODEL-003` clears the opencode session blocker, while `doc/audit/release-checklist-status.md` still contains concrete `NOT_RUN` release rows.

Rules for this section:

- These are evidence and audit tickets, not smoke tests.
- Do not add fake sessions, fake models, fake SSE events, fake install results, or fallback success.
- Do not use PostgreSQL as a substitute for Codex JSONL or opencode SQLite discovery.
- Do not run a live Codex or opencode command as a hidden gate; if a real run is required, record the selected real session and exact request body first.
- Android installability remains blocked until the user provides a final HTTPS or Android-trusted URL and verifies it on Android Chrome.

### PLANFIX-M002 Add Remaining Release Evidence Tickets

Symptom:

The previous ticket queue reached its end while the release checklist still has `NOT_RUN` items.

Expected:

The remaining release blockers are represented as small, executable, non-mock tickets.

Actual:

`doc/micro-tickets.md` ends at `BUG-OPENCODE-SESSION-MODEL-003`, so the next Agent could either stop or improvise.

Allowed files:

- `doc/micro-tickets.md`

Implementation:

1. Add one ticket to inspect existing real sessions for a live run evidence candidate.
2. Add one ticket to capture real run SSE evidence without calling it a smoke test.
3. Add one ticket to run a 360px browser viewport audit.
4. Add one ticket to re-run the release checklist after local evidence tickets.
5. Add one ticket to record the final Android URL requirement.
6. Add one ticket to verify Android Chrome installability only when the final URL exists.
7. Add one ticket to recalculate the post-audit delivery tracker.
8. Keep PostgreSQL as Lqtigee-owned persistence only.

Verification:

```bash
rg "PLANFIX-M002|EVIDENCE-RUNS-M001|EVIDENCE-RUNS-M002|EVIDENCE-FRONTEND-360-M001|ANDROID-FINAL-M001|ANDROID-FINAL-M002|TRACKER-M002|PostgreSQL" doc/micro-tickets.md
```

### EVIDENCE-RUNS-M001 Inspect Existing Real Sessions For Run Evidence Candidate

Symptom:

`doc/audit/release-checklist-status.md` says the Runs page SSE path was not verified with a real started run.

Expected:

A later live run evidence ticket has a real existing session and model selected from the API before it attempts to start anything.

Actual:

No candidate session evidence file records which real session/model can be used.

Allowed new files:

- `doc/audit/run-evidence-candidate.md`

Implementation:

1. Confirm port `20261` is free before starting the backend.
2. Start the backend with a local audit token.
3. Call `GET /api/sessions` with the token.
4. Select one candidate using only API fields required by `StartRunRequest`: `source`, `id`, `model`, and `workspace`.
5. Do not copy prompt text, transcript text, secrets, or full raw responses.
6. If no safe real candidate exists, write `BLOCKED` with the exact missing field.
7. Stop the backend process before finishing.
8. Do not call `POST /api/runs` in this ticket.

Verification:

```bash
test -f doc/audit/run-evidence-candidate.md
rg "PASS|BLOCKED|source|sessionId|model|workspace|no fake session|no run started" doc/audit/run-evidence-candidate.md
```

### EVIDENCE-RUNS-M002 Capture Real Run SSE Evidence

Symptom:

The release checklist still marks real run SSE evidence as `NOT_RUN`.

Expected:

A real run started through the Java API emits real SSE events and exactly one terminal event.

Actual:

The backend has tests for SSE behavior, but no audit document records a real API run and real SSE stream.

Allowed new files:

- `doc/audit/runs-sse-live-evidence.md`

Allowed files:

- `doc/audit/release-checklist-status.md`

Failing verification:

```bash
rg "Runs page streams real SSE events. \\| NOT_RUN" doc/audit/release-checklist-status.md
```

Implementation:

1. Require `doc/audit/run-evidence-candidate.md` to exist and be marked `PASS`.
2. Confirm port `20261` is free before starting the backend.
3. Start the backend with a local audit token.
4. Call `POST /api/runs` with the candidate `source`, `sessionId`, `modelId`, and `workspace`.
5. Use `READ_ONLY` mode unless a later ticket explicitly approves a stronger mode.
6. Use a short verification prompt generated for this audit; record the exact prompt in the audit file.
7. Open `GET /api/runs/{runId}/events` with the token.
8. Record event types, run id, start timestamp, terminal event type, and terminal count.
9. Do not fabricate events; if the stream fails, record `FAIL` with the actual typed error.
10. Stop the backend process before finishing.
11. Update `doc/audit/release-checklist-status.md` only if real SSE evidence passed.

Verification:

```bash
test -f doc/audit/runs-sse-live-evidence.md
rg "PASS|FAIL|runId|terminal event|terminal count|READ_ONLY|no fake events" doc/audit/runs-sse-live-evidence.md doc/audit/release-checklist-status.md
```

### EVIDENCE-FRONTEND-360-M001 Run Browser 360px Viewport Audit

Symptom:

`doc/audit/release-checklist-status.md` says 360px layout has CSS-level evidence only and browser evidence is `NOT_RUN`.

Expected:

A real browser opens the built frontend at a 360px viewport and records whether horizontal overflow exists.

Actual:

`doc/audit/frontend-360-layout.md` contains static CSS evidence but no browser viewport evidence.

Allowed files:

- `doc/audit/frontend-360-layout.md`
- `doc/audit/release-checklist-status.md`

Allowed new files:

- `doc/audit/screenshots/frontend-360-home.png`
- `doc/audit/screenshots/frontend-360-sessions.png`
- `doc/audit/screenshots/frontend-360-control.png`
- `doc/audit/screenshots/frontend-360-runs.png`
- `doc/audit/screenshots/frontend-360-settings.png`

Failing verification:

```bash
rg "browser screenshot was `NOT_RUN`|360px viewport has no horizontal scroll. \\| NOT_RUN" doc/audit/frontend-360-layout.md doc/audit/release-checklist-status.md
```

Implementation:

1. Run `cd frontend && npm install && npm run typecheck && npm run build`.
2. Start the frontend locally from the built or dev output.
3. Use a real local browser at width `360` and a stable mobile-height viewport.
4. Capture screenshots for `/`, `/sessions`, `/control`, `/runs`, and `/settings`.
5. Inspect horizontal overflow with browser tooling if available; if unavailable, record screenshot evidence and keep the overflow row `NOT_RUN`.
6. Do not claim `PASS` from CSS inspection alone.
7. Remove generated dependency/build directories after verification unless the ticket intentionally changes frontend dependencies.
8. Stop all local servers before finishing.
9. Update `doc/audit/release-checklist-status.md` only if the browser evidence supports the status.

Verification:

```bash
test -f doc/audit/frontend-360-layout.md
rg "360|browser|horizontal|PASS|FAIL|NOT_RUN|frontend-360-home.png" doc/audit/frontend-360-layout.md doc/audit/release-checklist-status.md
```

### RELEASE-AUDIT-M003 Re-run Release Checklist After Local Evidence

Symptom:

The release checklist was recalculated before all local evidence tickets were complete.

Expected:

The checklist reflects the current committed evidence and does not mark unverified items as pass.

Actual:

The checklist still contains `NOT_RUN` rows that may or may not be clearable on this machine.

Allowed files:

- `doc/audit/release-checklist-status.md`

Implementation:

1. Re-run the checklist against current committed code and audit files.
2. Use `PASS`, `FAIL`, or `NOT_RUN` only.
3. Keep Android secure-origin and installability as `NOT_RUN` unless `ANDROID-FINAL-M002` has passed.
4. Do not convert missing live evidence into success.
5. Record exact test counts and commands run.

Verification:

```bash
rg "PASS|FAIL|NOT_RUN|release is blocked|release is ready" doc/audit/release-checklist-status.md
```

### ANDROID-FINAL-M001 Record Final Android URL Requirement

Symptom:

Android PWA installability cannot be proven from port `20261` alone.

Expected:

The final phone URL is recorded before any Android installability claim.

Actual:

`doc/audit/android-pwa-secure-origin.md` says no final Android URL was supplied.

Allowed files:

- `doc/audit/android-pwa-secure-origin.md`

Implementation:

1. If the user provides a final URL, record its origin without tokens or secrets.
2. If the URL is plain `http://<server-ip>:20261`, keep installability `BLOCKED`.
3. If the URL is HTTPS or Android-trusted, record it as ready for Android Chrome verification.
4. Do not claim installability in this ticket.
5. If no final URL is provided, update the audit with `BLOCKED` and the exact missing input.

Verification:

```bash
rg "Final Android URL|secure context|plain HTTP|BLOCKED|ready for Android Chrome" doc/audit/android-pwa-secure-origin.md
```

### ANDROID-FINAL-M002 Verify Android Chrome Installability

Symptom:

The app is intended to be Android installable, but installability has not been verified on Android Chrome.

Expected:

The final Android URL is opened on Android Chrome and installability is proven or blocked with real evidence.

Actual:

No Android Chrome test has been run against a final secure URL.

Allowed files:

- `doc/audit/android-pwa-secure-origin.md`
- `doc/audit/release-checklist-status.md`

Implementation:

1. Require `ANDROID-FINAL-M001` to record an HTTPS or Android-trusted final URL.
2. On Android Chrome, open the final URL.
3. Verify `window.isSecureContext === true`.
4. Verify manifest loads with name `Lqtigee`.
5. Verify service worker registers and does not cache `/api/**`.
6. Verify the browser install option appears or Android Chrome reports the app as installable.
7. Record all results without screenshots containing secrets.
8. If any Android-only check cannot be executed, keep the related row `NOT_RUN` or `BLOCKED`.

Verification:

```bash
rg "secure context|manifest|service worker|install option|PASS|BLOCKED|NOT_RUN" doc/audit/android-pwa-secure-origin.md doc/audit/release-checklist-status.md
```

### TRACKER-M002 Recalculate Post-Audit Delivery Tracker

Symptom:

`doc/quality/post-audit-delivery-tracker.md` still contains entries opened before later delivery and opencode fixes.

Expected:

The tracker reflects current committed implementation and remaining release evidence blockers.

Actual:

Several tracker rows still say `OPEN` even after their clearing tickets have been implemented.

Allowed files:

- `doc/quality/post-audit-delivery-tracker.md`

Implementation:

1. Recalculate each tracker row from current code and audit evidence.
2. Mark implemented backend/frontend rows `CLOSED` only when their clearing tickets are committed and verified.
3. Keep Android installability `OPEN` until `ANDROID-FINAL-M002` passes.
4. Keep PostgreSQL boundary text unchanged: PostgreSQL remains Lqtigee-owned persistence and must not replace session discovery.
5. Do not modify code in this ticket.

Verification:

```bash
rg "OPEN|CLOSED|PostgreSQL|Android|Sessions API|Runs API" doc/quality/post-audit-delivery-tracker.md
```

### ANDROID-SCOPE-M001 Correct Android Mapping Boundary

Symptom:

Release evidence currently treats the final Android Chrome HTTPS URL as a project-internal blocker.

Expected:

The project is complete when the Java service exposes the fixed port `20261`, local browser evidence passes, and Android/PWA documentation states that public IP, DNS, TLS, and fixed mapping are external deployment responsibilities.

Actual:

`doc/audit/release-checklist-status.md`, `doc/audit/android-pwa-secure-origin.md`, and `doc/quality/post-audit-delivery-tracker.md` still block release on a final Android Chrome URL even though `doc/requirements.md` states public mapping is handled outside this project.

Allowed files:

- `doc/quality/release-checklist.md`
- `doc/audit/release-checklist-status.md`
- `doc/audit/android-pwa-secure-origin.md`
- `doc/quality/post-audit-delivery-tracker.md`

Implementation:

1. Keep Android installability claims limited to local project-owned evidence.
2. Require local evidence that the backend listens on port `20261`.
3. Require local evidence that the PWA assets exist and `/api/**` is not cached.
4. State that public IP mapping, DNS, HTTPS certificates, Android Chrome final URL, and phone-side installation are external deployment responsibilities.
5. Do not claim that Android Chrome installability has been verified.
6. Do not mark plain HTTP server IP as installable.
7. Mark release ready only if every project-owned gate passes and the only remaining item is external deployment mapping.
8. Keep PostgreSQL boundary text unchanged.

Verification:

```bash
ss -ltnp | rg ':20261'
rg "20261|external deployment|public mapping|Android Chrome|PASS|NOT_RUN|OPEN|CLOSED|PostgreSQL" doc/quality/release-checklist.md doc/audit/release-checklist-status.md doc/audit/android-pwa-secure-origin.md doc/quality/post-audit-delivery-tracker.md
```

### ANDROID-SCOPE-M002 Retire Internal Android Final URL Tickets

Symptom:

Older planning tickets still describe the final Android Chrome URL and phone-side installability as if they were project-owned release blockers.

Expected:

The micro-ticket plan must make port `20261` the project-owned delivery boundary and classify public mapping, DNS, TLS, final Android Chrome URL, and phone-side installability as external deployment work.

Actual:

`AUDIT-M006`, `RELEASE-AUDIT-M003`, `ANDROID-FINAL-M001`, `ANDROID-FINAL-M002`, and `TRACKER-M002` still contain old instructions that can make a future Agent re-enter the final-URL blocking loop.

Allowed files:

- `doc/micro-tickets.md`

Implementation:

1. Update older Android-related tickets so they cannot be selected as project-owned release blockers.
2. Mark final Android URL and Android Chrome installability verification as optional external deployment audit work.
3. Keep plain `http://<server-ip>:20261` installability claims prohibited.
4. Keep the project-owned release boundary at a Java service listening on port `20261`, real API/session/run behavior, local PWA assets, and service worker `/api/**` bypass.
5. Do not modify source code or audit result files in this ticket.

Verification:

```bash
rg "ANDROID-SCOPE-M002|external deployment|project-owned|ANDROID-FINAL|AUDIT-M006|TRACKER-M002|20261" doc/micro-tickets.md
rg "Keep Android secure-origin and installability as `NOT_RUN` unless|Keep Android installability `OPEN` until|If no final URL is provided, update the audit with `BLOCKED`" doc/micro-tickets.md
```

### BUG-RUN-SSE-M001 Make Production Output Pump Asynchronous

Symptom:

`doc/audit/runs-sse-live-evidence.md` records that a real `POST /api/runs` started a Codex child process but did not return a usable `runId` within the live audit window.

Expected:

`POST /api/runs` starts the process, attaches output pumping asynchronously, and returns `StartRunResponse` quickly so the phone UI can subscribe to `/api/runs/{runId}/events`.

Actual:

`RunRuntimeConfig.processOutputPump()` constructs `ProcessOutputPump` with `Runnable::run`, so `ProcessOutputPump.attach()` waits on the child process inside the request thread.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/RunRuntimeConfig.java`
- `src/main/java/com/lqtigee/sparkai/runtime/ProcessOutputPump.java`
- `src/test/java/com/lqtigee/sparkai/runtime/ProcessOutputPumpTest.java`
- `doc/audit/runs-sse-live-evidence.md`

Failing verification:

```bash
rg "Runnable::run" src/main/java/com/lqtigee/sparkai/runtime/RunRuntimeConfig.java
```

Implementation:

1. Keep the synchronous executor available only for deterministic unit tests.
2. Add or use a production constructor that gives `ProcessOutputPump` a real asynchronous executor while preserving `RunRegistry` updates.
3. Update `RunRuntimeConfig.processOutputPump()` to use the asynchronous production path.
4. Do not change command builders.
5. Do not change API response shapes.
6. Do not add fake SSE events.
7. Update the live evidence document to say the blocking cause has a follow-up fix ticket, but do not mark live SSE evidence as `PASS` until `EVIDENCE-RUNS-M002` is re-run.

Verification:

```bash
mvn test -Dtest=ProcessOutputPumpTest
! rg "Runnable::run" src/main/java/com/lqtigee/sparkai/runtime/RunRuntimeConfig.java
rg "BUG-RUN-SSE-M001|not yet PASS|no fake events" doc/audit/runs-sse-live-evidence.md
```

### EVIDENCE-RUNS-M003 Re-run Real Run SSE Evidence After Async Pump Fix

Symptom:

`BUG-RUN-SSE-M001` fixed the synchronous output pump wiring that blocked `POST /api/runs`, but the live SSE evidence still says `FAIL`.

Expected:

The live run evidence is re-run with a real session and real process after the async pump fix.

Actual:

`doc/audit/runs-sse-live-evidence.md` is still the pre-fix failure record and must not be upgraded without a new real run.

Allowed files:

- `doc/audit/runs-sse-live-evidence.md`
- `doc/audit/release-checklist-status.md`

Implementation:

1. Require `doc/audit/run-evidence-candidate.md` to exist and be marked `PASS`.
2. Require `BUG-RUN-SSE-M001` code to be committed.
3. Confirm port `20261` is free before starting the backend.
4. Start the backend with a local audit token.
5. Call `POST /api/runs` with the candidate `source`, `sessionId`, `modelId`, and `mode=ASK`.
6. Record whether `StartRunResponse.runId` returns quickly.
7. Open `GET /api/runs/{runId}/events` with the token.
8. Record event types, run id, start timestamp, terminal event type, and terminal count.
9. If the process is still running after the evidence window, call `POST /api/runs/{runId}/stop` and record the stop result.
10. Do not fabricate events or terminal results.
11. Stop the backend process before finishing.
12. Update `doc/audit/release-checklist-status.md` only if real SSE evidence passes.

Verification:

```bash
test -f doc/audit/runs-sse-live-evidence.md
rg "EVIDENCE-RUNS-M003|PASS|FAIL|runId|terminal event|terminal count|no fake events" doc/audit/runs-sse-live-evidence.md doc/audit/release-checklist-status.md
```

### BUG-RUN-SSE-M002 Replay Terminal Event To Late SSE Subscribers

Symptom:

`EVIDENCE-RUNS-M003` proved `POST /api/runs` returns `runId` quickly after `BUG-RUN-SSE-M001`, but a real SSE subscription still received 0 bytes and the run was already terminal.

Expected:

If a run reaches a terminal event before or around the time the phone subscribes, `/api/runs/{runId}/events` sends exactly one real terminal event and completes the SSE response.

Actual:

`RunEventBus.publish()` only sends events to currently registered subscribers. Terminal events are lost for late subscribers, and the SSE response can remain open with no bytes.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/RunEventBus.java`
- `src/test/java/com/lqtigee/sparkai/runtime/RunEventBusTest.java`
- `doc/audit/runs-sse-live-evidence.md`

Failing verification:

```bash
mvn test -Dtest=RunEventBusTest
```

Implementation:

1. Add a terminal event detector for event types `done`, `error`, and `stopped`.
2. Store the latest terminal event per run id when `publish()` receives a terminal event.
3. When `subscribe(runId)` is called after a terminal event exists, send that stored event immediately and complete the emitter.
4. When `publish()` sends a terminal event to current subscribers, complete those emitters after sending.
5. Remove completed emitters from the subscriber list.
6. Do not create fake events.
7. Do not change `RunEventDto` shape.
8. Do not change `RunService` or command builders.
9. Update live evidence to say this ticket fixes the late-subscriber event delivery bug, but do not mark live SSE evidence `PASS` until a new real evidence ticket runs.

Verification:

```bash
mvn test -Dtest=RunEventBusTest
rg "terminalEvents|isTerminal|complete" src/main/java/com/lqtigee/sparkai/runtime/RunEventBus.java src/test/java/com/lqtigee/sparkai/runtime/RunEventBusTest.java
rg "BUG-RUN-SSE-M002|not yet PASS|no fake events" doc/audit/runs-sse-live-evidence.md
```

### EVIDENCE-RUNS-M004 Re-run Real Run SSE Evidence After Terminal Replay Fix

Symptom:

`BUG-RUN-SSE-M002` added terminal event replay for late SSE subscribers, but the live evidence still contains the pre-fix `FAIL`.

Expected:

A real run returns `runId`, `/api/runs/{runId}/events` receives one real terminal event, and the release checklist can be updated only if that real evidence passes.

Actual:

No real run has been audited after `BUG-RUN-SSE-M002`.

Allowed files:

- `doc/audit/runs-sse-live-evidence.md`
- `doc/audit/release-checklist-status.md`

Implementation:

1. Require `doc/audit/run-evidence-candidate.md` to exist and be marked `PASS`.
2. Require `BUG-RUN-SSE-M001` and `BUG-RUN-SSE-M002` code to be committed.
3. Confirm port `20261` is free before starting the backend.
4. Start the backend with a local audit token.
5. Call `POST /api/runs` with the candidate `source`, `sessionId`, `modelId`, and `mode=ASK`.
6. Record `StartRunResponse.runId` and elapsed start time.
7. Open `GET /api/runs/{runId}/events` with the token.
8. Record real event types, terminal event type, terminal count, and whether the SSE response completed.
9. If no terminal event arrives within the evidence window, record `FAIL` and clean up.
10. Do not fabricate events or terminal results.
11. Stop the backend process before finishing.
12. Update `doc/audit/release-checklist-status.md` only if real SSE evidence passes.

Verification:

```bash
test -f doc/audit/runs-sse-live-evidence.md
rg "EVIDENCE-RUNS-M004|PASS|FAIL|runId|terminal event|terminal count|SSE response completed|no fake events" doc/audit/runs-sse-live-evidence.md doc/audit/release-checklist-status.md
```
