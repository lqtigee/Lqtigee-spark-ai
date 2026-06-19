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

### FE-M003 Create main.tsx

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

### FE-M004 Create App Shell Placeholder

Allowed new files:

- `frontend/src/app/App.tsx`

Implementation:

1. Render app shell title `Lqtigee`.
2. Render "Not connected" state.
3. No fake sessions/models.

Verification:

```bash
cd frontend && npm run build
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
cd frontend && npm run build
```

### FE-M006 Define Frontend API Types

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

### FE-M007 Create HTTP Client Skeleton

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

### FE-M008 Add HTTP Client Token Requirement

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

### FE-M009 Add HTTP Client Error Parsing

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

### FE-M010 Create Remote API getHealth Only

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

### FE-M011 Add Remote API listModels Only

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

### FE-M012 Add Remote API listSessions Only

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

### FE-M013 Add Remote API startRun Only

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

### FE-M014 Add Remote API stopRun Only

Allowed files:

- `frontend/src/api/remoteApi.ts`

Implementation:

1. Export `stopRun(runId)`.
2. POST `/api/runs/{id}/stop`.

Verification:

```bash
cd frontend && npm run typecheck
```

### FE-M015 Add Remote API openRunEvents Only

Allowed files:

- `frontend/src/api/remoteApi.ts`

Implementation:

1. Export `openRunEvents(runId, handlers)`.
2. Use `EventSource`.
3. Close on terminal event.
4. Surface errors to handler.

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
