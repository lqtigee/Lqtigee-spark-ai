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

### AUDIT-M006 Android PWA External Deployment Audit

Goal: Document Android PWA installability rules without making public mapping a project-owned release gate.

Allowed new files:

- `doc/audit/android-pwa-secure-origin.md`

Implementation:

1. State that the project-owned server exposes port `20261`.
2. State that public IP mapping, DNS, TLS, final Android Chrome URL, and phone-side installation are external deployment responsibilities.
3. Verify local PWA assets exist.
4. Verify service worker behavior does not cache `/api/**`.
5. Do not claim Android Chrome installability unless a later external deployment audit provides real Android Chrome evidence.
6. If an external URL is plain `http://<server-ip>:20261`, state that Android PWA installability must not be claimed.

Verification:

```bash
test -f doc/audit/android-pwa-secure-origin.md
rg "20261|external deployment|manifest|service worker|Android Chrome|not claimed|PASS" doc/audit/android-pwa-secure-origin.md
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
8. Treat Android Chrome final URL and phone-side installation as external deployment evidence, not as project-owned release blockers.
9. Do not mark release ready by assumption.
10. Remove generated frontend files after verification.

Verification:

```bash
test -f doc/audit/release-checklist-status.md
rg "PASS|FAIL|NOT_RUN|release is blocked|release is ready" doc/audit/release-checklist-status.md
```

### RELEASE-AUDIT-M002 Optional Android External Deployment Audit

Goal: Verify Android installability only when the user starts external deployment verification with a real final URL.

Allowed files:

- `doc/audit/android-pwa-secure-origin.md`

Implementation:

1. Record the final Android URL without secrets as external deployment context.
2. If it is `http://<server-ip>:20261`, state that Android PWA installability must not be claimed.
3. If it is HTTPS or Android-trusted, open it on Android Chrome.
4. Verify secure context.
5. Verify manifest.
6. Verify service worker.
7. Verify install option.
8. Do not mark Android installability `PASS` from desktop-only checks.
9. Do not change project-owned release status when this external deployment evidence is missing.

Verification:

```bash
test -f doc/audit/android-pwa-secure-origin.md
rg "secure context|manifest|service worker|install option|external deployment|not claimed|PASS" doc/audit/android-pwa-secure-origin.md
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
- Android Chrome installability remains an external deployment claim; project-owned release must not wait on public mapping, DNS, TLS, final Android Chrome URL, or phone-side installation.

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
5. Add one ticket to correct the Android mapping boundary.
6. Add one ticket to retire old final-URL-as-project-blocker instructions.
7. Add one ticket to recalculate the post-audit delivery tracker.
8. Keep PostgreSQL as Lqtigee-owned persistence only.

Verification:

```bash
rg "PLANFIX-M002|EVIDENCE-RUNS-M001|EVIDENCE-RUNS-M002|EVIDENCE-FRONTEND-360-M001|ANDROID-SCOPE-M001|ANDROID-SCOPE-M002|TRACKER-M002|PostgreSQL" doc/micro-tickets.md
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
3. Treat Android Chrome final URL and phone-side installation as external deployment evidence, not as project-owned release blockers.
4. Do not convert missing live evidence into success.
5. Record exact test counts and commands run.

Verification:

```bash
rg "PASS|FAIL|NOT_RUN|release is blocked|release is ready" doc/audit/release-checklist-status.md
```

### ANDROID-FINAL-M001 External Final Android URL Note

Symptom:

Android PWA installability cannot be claimed from port `20261` alone.

Expected:

Project-owned release remains complete at port `20261`, while any final phone URL is documented only as external deployment evidence.

Actual:

Older planning text treated the missing final Android URL as a project-owned blocker.

Allowed files:

- `doc/audit/android-pwa-secure-origin.md`

Implementation:

1. If the user provides a final URL, record its origin without tokens or secrets as external deployment context.
2. If the URL is plain `http://<server-ip>:20261`, state that Android PWA installability must not be claimed.
3. If the URL is HTTPS or Android-trusted, record it as ready for optional external Android Chrome verification.
4. Do not claim installability in this ticket.
5. If no final URL is provided, keep project-owned release status unchanged and state that no external deployment evidence exists.

Verification:

```bash
rg "Final Android URL|external deployment|plain HTTP|not claimed|ready for Android Chrome" doc/audit/android-pwa-secure-origin.md
```

### ANDROID-FINAL-M002 Optional External Android Chrome Installability Verification

Symptom:

Android Chrome installability can only be proven after the user's external deployment exposes a secure final URL.

Expected:

If external deployment evidence is requested, the final Android URL is opened on Android Chrome and installability is proven or rejected with real evidence.

Actual:

No Android Chrome installability claim is part of the project-owned release boundary.

Allowed files:

- `doc/audit/android-pwa-secure-origin.md`
- `doc/audit/release-checklist-status.md`

Implementation:

1. Run this ticket only after the user asks for external deployment verification and provides an HTTPS or Android-trusted final URL.
2. On Android Chrome, open the final URL.
3. Verify `window.isSecureContext === true`.
4. Verify manifest loads with name `Lqtigee`.
5. Verify service worker registers and does not cache `/api/**`.
6. Verify the browser install option appears or Android Chrome reports the app as installable.
7. Record all results without screenshots containing secrets.
8. If any Android-only check cannot be executed, keep Android installability unclaimed and do not change project-owned release status.

Verification:

```bash
rg "secure context|manifest|service worker|install option|PASS|not claimed|external deployment" doc/audit/android-pwa-secure-origin.md doc/audit/release-checklist-status.md
```

### TRACKER-M002 Recalculate Post-Audit Delivery Tracker

Symptom:

`doc/quality/post-audit-delivery-tracker.md` still contains entries opened before later delivery and opencode fixes.

Expected:

The tracker reflects current committed implementation and remaining project-owned release evidence blockers.

Actual:

Several tracker rows still say `OPEN` even after their clearing tickets have been implemented.

Allowed files:

- `doc/quality/post-audit-delivery-tracker.md`

Implementation:

1. Recalculate each tracker row from current code and audit evidence.
2. Mark implemented backend/frontend rows `CLOSED` only when their clearing tickets are committed and verified.
3. Keep Android installability outside the project-owned tracker unless the user starts an external deployment verification ticket.
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
! rg 'Keep Android secure-origin [a-z -]+ blocked unless|Keep Android secure-origin and installability as [`]NOT_RUN[`] unless|Keep Android installability [`]OPEN[`] until|If no final URL is provided, update the audit with [`]BLOCKED[`]|If it is [`]http://<server-ip>:20261[`], keep [`]BLOCKED[`]' doc/micro-tickets.md
```

### PUBLIC-ACCESS-M001 Serve PWA On Backend Port

Symptom:

The Java service on port `20261` exposes the API but the Android phone also needs the PWA UI from the same externally mapped service.

Expected:

After `frontend` is built, the Spring Boot artifact serves the PWA shell and assets from port `20261` while preserving all `/api/**` behavior.

Actual:

The frontend build output is separate from the backend artifact, so a phone opening the mapped `20261` address may not receive the Lqtigee UI.

Allowed files:

- `pom.xml`
- `src/main/java/com/lqtigee/sparkai/web/PwaForwardController.java`
- `src/test/java/com/lqtigee/sparkai/web/PwaForwardControllerTest.java`
- `doc/audit/public-access.md`

Implementation:

1. Add a Maven step that copies `frontend/dist` into Spring Boot static resources during packaging.
2. Add a controller that forwards non-API, extensionless PWA routes to `index.html`.
3. Do not intercept `/api/**`, `/actuator/**`, `/manifest.webmanifest`, `/sw.js`, `/icons/**`, or other static asset files.
4. Add a test proving `/sessions`, `/control`, `/runs`, and `/settings` forward to the PWA shell.
5. Add an audit file recording local same-port PWA verification.
6. Do not add fake sessions, fake models, or fallback success.

Verification:

```bash
cd frontend && npm install && npm run typecheck && npm run build
mvn test
mvn package -DskipTests
jar tf target/Lqtigee-spark-ai-0.0.1-SNAPSHOT.jar | rg 'BOOT-INF/classes/static/(index.html|manifest.webmanifest|sw.js|assets/)'
LQTIGEE_API_TOKEN=test-token java -jar target/Lqtigee-spark-ai-0.0.1-SNAPSHOT.jar
curl -sS http://127.0.0.1:20261/ | rg 'id="root"|manifest.webmanifest'
curl -sS http://127.0.0.1:20261/sessions | rg 'id="root"|manifest.webmanifest'
curl -sS http://127.0.0.1:20261/api/health | rg '"port":20261'
rm -rf frontend/node_modules frontend/package-lock.json frontend/dist
```

### BUG-SESSION-TITLE-M002 Ignore Codex Environment Context For Titles

Symptom:

After `BUG-SESSION-TITLE-M001`, the latest Codex session title can be derived from a real `role=user` environment-context record such as `<environment_context>`, which is not a useful chat title.

Expected:

Codex list titles and `lastMessage` derive from visible user/assistant chat text but ignore environment-context scaffolding records. Transcript endpoint behavior is unchanged.

Actual:

`CodexJsonlParser` treats every user/assistant text message as title/preview material.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexJsonlParser.java`
- `src/test/java/com/lqtigee/sparkai/codex/CodexJsonlParserTest.java`
- `src/test/resources/samples/codex-session-sample.jsonl`
- `doc/audit/public-access.md`

Implementation:

1. Exclude normalized text starting with `<environment_context>` from title and `lastMessage` derivation.
2. Do not change `CodexTranscriptReader`.
3. Do not hide user/assistant transcript messages from the transcript endpoint.
4. Keep fallback `Codex <short id>` if no displayable user text remains.
5. Add test coverage proving environment context is skipped and the next user text becomes title.

Verification:

```bash
mvn test -Dtest=CodexJsonlParserTest
rg "environment_context|isDisplayablePreviewText" src/main/java/com/lqtigee/sparkai/codex/CodexJsonlParser.java src/test/java/com/lqtigee/sparkai/codex/CodexJsonlParserTest.java doc/audit/public-access.md
```

### PUBLIC-ACCESS-M004 Refresh Public Asset Evidence After Health Label Fix

Symptom:

`BUG-MOBILE-CONSOLE-M002` changed the Overview bundle and the public service was rebuilt, so the public shell now references a newer JS asset than the asset hash recorded by `PUBLIC-ACCESS-M003`.

Expected:

The public access audit records the latest public PWA asset hash and confirms the authenticated local-session counts still match after the rebuild.

Actual:

`doc/audit/public-access.md` still records the previous JS asset hash.

Allowed files:

- `doc/audit/public-access.md`

Implementation:

1. Fetch public `/sessions`.
2. Record the latest CSS and JS asset names.
3. Fetch authenticated public `/api/sessions`.
4. Record total, Codex, and opencode counts.
5. Do not print or store the API token.
6. Do not change code or restart services in this ticket.

Verification:

```bash
curl -sS --max-time 10 http://118.24.15.133:20261/sessions | rg 'index-Cdupm8tF.js|index-Cb9nzxpz.css|id="root"'
curl -sS --max-time 20 -H "Authorization: Bearer <token>" http://118.24.15.133:20261/api/sessions
rg "PUBLIC-ACCESS-M004|index-Cdupm8tF.js|CODEX=684|OPENCODE=480" doc/audit/public-access.md
```

### BUG-SESSION-TITLE-M001 Derive Codex Titles From Real Visible Messages

Symptom:

Codex sessions display generic titles such as `Codex 019ee090`, which is technically a title but not a useful chat title.

Expected:

Codex session list titles are derived from the first visible user chat message when present, and `lastMessage` is derived from the latest visible user or assistant chat message. The fallback `Codex <short id>` is used only when no visible user chat text exists.

Actual:

`CodexJsonlParser` always sets title to `Codex <short id>` and `lastMessage` to an empty string.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexJsonlParser.java`
- `src/test/java/com/lqtigee/sparkai/codex/CodexJsonlParserTest.java`
- `src/test/resources/samples/codex-session-sample.jsonl`
- `doc/contracts/backend-api-contract.md`

Implementation:

1. Parse only `response_item.payload.type=message` records.
2. Use only visible `role=user` and `role=assistant` message text.
3. Ignore developer, system, reasoning, tool, encrypted, and non-text records.
4. Extract text only from content items that contain textual `text`.
5. Set Codex title from first non-empty user text, normalized to one line and capped.
6. Set `lastMessage` from latest visible user or assistant text, normalized and capped.
7. Do not invent summaries or call any model.
8. Update the API contract to allow authenticated chat-derived titles and previews.

Verification:

```bash
mvn test -Dtest=CodexJsonlParserTest
rg "chat-derived|first visible user|lastMessage" doc/contracts/backend-api-contract.md src/main/java/com/lqtigee/sparkai/codex/CodexJsonlParser.java
```

### BUG-SESSION-CHAT-M001 Add Session Transcript Contract

Symptom:

The backend contract has session list endpoints but no endpoint contract for opening a session as a chat transcript.

Expected:

The contract defines a protected endpoint that returns one session plus chronological visible chat messages from the real Codex/opencode storage source.

Actual:

No transcript endpoint contract exists.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`

Implementation:

1. Add `SessionMessageDto` with `id`, `role`, `text`, and `timestamp`.
2. Add `SessionTranscriptDto` with `session` and `messages`.
3. Add `GET /api/sessions/{source}/{id}/transcript`.
4. State that developer/system/tool/reasoning messages are excluded.
5. State that empty messages are excluded.
6. State that no mock transcript or generated summary is allowed.
7. Preserve existing session list contract.

Verification:

```bash
rg "SessionMessageDto|SessionTranscriptDto|/api/sessions/.*/transcript|developer/system/tool/reasoning" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
```

### BUG-SESSION-CHAT-M002 Add Transcript DTO Records

Symptom:

Java has no typed response records for a session chat transcript.

Expected:

Backend code has typed DTO records matching the transcript contract.

Actual:

Only `RemoteSessionDto` exists.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/dto/SessionMessageDto.java`
- `src/main/java/com/lqtigee/sparkai/dto/SessionTranscriptDto.java`

Implementation:

1. Add `SessionMessageDto`.
2. Add `SessionTranscriptDto`.
3. Keep DTOs as records.
4. Do not add endpoint code.
5. Do not add parser code.

Verification:

```bash
mvn test -DskipTests
rg "record SessionMessageDto|record SessionTranscriptDto" src/main/java/com/lqtigee/sparkai/dto
```

### BUG-SESSION-CHAT-M003 Add Codex Transcript Reader

Symptom:

Codex JSONL contains real visible chat messages, but the backend cannot return them for a selected session.

Expected:

A Codex transcript reader reads a selected real JSONL file and returns chronological visible user/assistant chat messages only.

Actual:

Only Codex session metadata parsing exists.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexTranscriptReader.java`
- `src/test/java/com/lqtigee/sparkai/codex/CodexTranscriptReaderTest.java`
- `src/test/resources/samples/codex-transcript-sample.jsonl`

Implementation:

1. Read JSONL from a provided path.
2. Select only `response_item.payload.type=message`.
3. Include only `role=user` and `role=assistant`.
4. Extract only textual `text` content.
5. Exclude empty messages.
6. Use record timestamp or fail with `CODEX_SESSION_FORMAT_UNKNOWN` on invalid timestamps.
7. Use stable message ids from payload id when present, otherwise source line number.
8. Do not include developer/system/tool/reasoning content.
9. Do not synthesize messages.

Verification:

```bash
mvn test -Dtest=CodexTranscriptReaderTest
rg "developer|system|tool|reasoning" src/test/java/com/lqtigee/sparkai/codex/CodexTranscriptReaderTest.java
```

### BUG-SESSION-CHAT-M004 Add opencode Transcript Reader

Symptom:

opencode SQLite contains real messages and parts, but the backend cannot return a selected session as chat.

Expected:

An opencode transcript reader reads `message` and `part` rows for one selected session and returns chronological visible user/assistant text messages only.

Actual:

Only opencode session metadata reading exists.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteTranscriptReader.java`
- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteTranscriptReaderTest.java`

Implementation:

1. Open the SQLite database read-only.
2. Query only rows for the selected session id.
3. Join message rows with part rows by `message_id`.
4. Include only `role=user` and `role=assistant`.
5. Extract only part JSON with `type=text` and textual `text`.
6. Exclude tool, snapshot, step-start, step-finish, and empty messages.
7. Preserve chronological order by message time then part time.
8. Do not synthesize messages or summaries.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteTranscriptReaderTest
rg "type=text|role=user|role=assistant" src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteTranscriptReader.java src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteTranscriptReaderTest.java
```

### BUG-SESSION-CHAT-M005 Add Transcript Service And Endpoint

Symptom:

The frontend cannot open a session as chat because there is no authenticated transcript endpoint.

Expected:

`GET /api/sessions/{source}/{id}/transcript` validates the selected real session, reads from the matching real source, and returns a typed transcript response.

Actual:

Only session list endpoints exist.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/SessionTranscriptService.java`
- `src/main/java/com/lqtigee/sparkai/web/SessionController.java`
- `src/test/java/com/lqtigee/sparkai/service/SessionTranscriptServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/SessionControllerTest.java`

Implementation:

1. `SessionTranscriptService` depends on `SessionService`, `CodexTranscriptReader`, and `OpencodeSqliteTranscriptReader`.
2. Validate selected session with `SessionService.getRequiredSession(source, id)`.
3. For Codex, read transcript from `RemoteSessionDto.rawFile`.
4. For opencode, read transcript from `RemoteSessionDto.rawFile` as the database path and selected id.
5. Add controller endpoint `/api/sessions/{source}/{id}/transcript`.
6. Preserve token auth.
7. Do not return fake messages for empty transcripts.

Verification:

```bash
mvn test -Dtest=SessionTranscriptServiceTest,SessionControllerTest
rg "/api/sessions/\\{source\\}/\\{id\\}/transcript|SessionTranscriptService" src/main/java src/test/java
```

### BUG-SESSION-CHAT-M006 Add Frontend Transcript API State

Symptom:

The frontend cannot request or store a real session transcript.

Expected:

Frontend types, API client, and state hook can load a real selected session transcript from the backend.

Actual:

Frontend only has session list types and API calls.

Allowed files:

- `frontend/src/types/api.ts`
- `frontend/src/api/remoteApi.ts`
- `frontend/src/state/useSessionTranscriptState.ts`

Implementation:

1. Add `SessionMessageDto` type.
2. Add `SessionTranscriptDto` type.
3. Add `getSessionTranscript(source, id)` API call.
4. Add `useSessionTranscriptState` hook with loading, loaded, error, transcript, and load method.
5. Do not add UI page code.
6. Do not add mock transcript data.

Verification:

```bash
cd frontend && npm install && npm run typecheck
rg "getSessionTranscript|useSessionTranscriptState|SessionTranscriptDto" frontend/src
rm -rf frontend/node_modules frontend/package-lock.json frontend/dist
```

### BUG-SESSION-CHAT-M007 Make Sessions Page Open Chat

Symptom:

Clicking a session only selects metadata; it does not open the chat conversation.

Expected:

On the Sessions page, selecting a session opens the real chat transcript for that session. The chat view displays the session title, source, model/workspace metadata, visible user/assistant messages, loading/error/empty states, and a back action on mobile.

Actual:

Sessions page shows cards and `SessionDetail`, not chat.

Allowed files:

- `frontend/src/components/SessionCard.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/styles/global.css`
- `doc/audit/mobile-console-ui.md`

Implementation:

1. Use `useSessionTranscriptState`.
2. On session select, persist selected session and load the real transcript.
3. Display chat messages from `transcript.messages` only.
4. Keep loading, error, empty transcript, and loaded transcript states distinct.
5. Use title from the selected real session.
6. Do not display raw transcript if API failed.
7. Do not add fake chat bubbles or sample messages.
8. Keep metadata available but make chat the selected-session main view.

Verification:

```bash
cd frontend && npm install && npm run build
rg "transcript.messages|useSessionTranscriptState|chat-message" frontend/src
! rg "sample message|fake chat|mock transcript" frontend/src
rm -rf frontend/node_modules frontend/package-lock.json frontend/dist
```

### PUBLIC-ACCESS-M005 Rebuild Public Entry With Session Chat

Symptom:

Session chat code has been added but the public `20261` service may still serve the previous jar.

Expected:

The public URL serves the rebuilt chat UI and authenticated transcript endpoint for local Codex/opencode sessions.

Actual:

Runtime must be rebuilt and restarted after chat implementation.

Allowed files:

- `doc/audit/public-access.md`

Implementation:

1. Build frontend and backend jar.
2. Restart only the local Lqtigee Java service.
3. Keep public server as mapping only.
4. Verify public `/sessions` serves the latest PWA shell.
5. Verify authenticated public `/api/sessions` returns real sessions.
6. Pick one real Codex and one real opencode session id from the API.
7. Verify each transcript endpoint returns `messages` array.
8. Record counts and asset names without recording token or transcript text.

Verification:

```bash
cd frontend && npm install && npm run build
mvn package -DskipTests
curl -sS --max-time 10 http://118.24.15.133:20261/sessions
curl -sS --max-time 20 -H "Authorization: Bearer <token>" http://118.24.15.133:20261/api/sessions
curl -sS --max-time 20 -H "Authorization: Bearer <token>" http://118.24.15.133:20261/api/sessions/CODEX/<id>/transcript
curl -sS --max-time 20 -H "Authorization: Bearer <token>" http://118.24.15.133:20261/api/sessions/OPENCODE/<id>/transcript
rm -rf frontend/node_modules frontend/package-lock.json frontend/dist
```

### BUG-MOBILE-CONSOLE-M002 Clarify Health Connection Label

Symptom:

The mobile console Overview displays the raw `/api/health.status` value as the main connection state. The current backend returns `STARTING` from `HealthController`, which can make a successful connection look suspicious even though public mapping and authenticated session reads pass.

Expected:

Overview shows API reachability as connected when `/api/health` succeeds, and displays the raw backend status as a secondary service state field.

Actual:

Overview places the raw `health.status` value in the visible Status slot.

Allowed files:

- `frontend/src/pages/OverviewPage.tsx`
- `doc/audit/mobile-console-ui.md`

Failing verification:

```bash
rg "StatusBadge status={health.status}" frontend/src/pages/OverviewPage.tsx
```

Implementation:

1. Change the Overview status strip so successful `/api/health` response displays `Connected` as the connection state.
2. Add a separate service state field that displays the raw `health.status` value.
3. Do not change the backend health contract.
4. Do not add fake health responses.
5. Update the mobile console audit note.

Verification:

```bash
cd frontend && npm install && npm run build
! rg "StatusBadge status=\\{health.status\\}" frontend/src/pages/OverviewPage.tsx
rm -rf frontend/node_modules frontend/package-lock.json frontend/dist
```

### PUBLIC-ACCESS-M002 Verify Public Server Mapping

Symptom:

`public_server_info` provides a public server, but `http://118.24.15.133:20261/api/health` is currently unreachable.

Expected:

The public server forwards external port `20261` to the real Lqtigee service that owns live local Codex/opencode sessions.

Actual:

SSH evidence shows the public server has no listener on `20261`, firewalld does not list `20261/tcp`, and the server does not contain live local Codex/opencode session data for this project.

Allowed files:

- `doc/audit/public-access.md`

Implementation:

1. Keep the Java service running on the machine that owns the real Codex/opencode session files.
2. Configure only the public mapping layer on the public server.
3. Open `20261/tcp` on the public server only if the mapping service is ready.
4. Verify external `GET /api/health` returns the Lqtigee service with `port=20261`.
5. Verify external `/` and `/sessions` return the PWA shell from the same port.
6. Record whether the final URL is plain HTTP or a secure Android-trusted origin.
7. Do not claim Android Chrome installability over plain HTTP.
8. Do not run live Codex/opencode commands as part of this verification.

Verification:

```bash
curl -sS -i --max-time 10 http://118.24.15.133:20261/api/health
curl -sS --max-time 10 http://118.24.15.133:20261/ | rg 'id="root"|manifest.webmanifest'
curl -sS --max-time 10 http://118.24.15.133:20261/sessions | rg 'id="root"|manifest.webmanifest'
rg "PUBLIC-ACCESS-M002|118.24.15.133|20261|PASS|NOT_CLAIMED|Android Chrome" doc/audit/public-access.md
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

### BUG-MOBILE-CONSOLE-M001 Repair Mobile Console UI And Connection State

Symptom:

The phone UI at the mapped `20261` address looks like a skeleton page and does not make it obvious that the app is connected to the live Lqtigee Java service that reads current Codex/opencode sessions.

Expected:

The phone first screen behaves like a compact remote operator console: it shows real connection state, requires a real token before protected session/model calls, loads only real API data, displays current Codex/opencode sessions in a mobile-friendly layout, and keeps run/control actions tied to selected real sessions and real models.

Actual:

The UI uses bare browser-like sections, lists, selects, and forms. Settings does not prefill the same-origin public service URL. The Overview page only checks health and does not surface whether protected live session/model data can be loaded.

Allowed files:

- `frontend/src/components/AppShell.tsx`
- `frontend/src/components/BottomNav.tsx`
- `frontend/src/components/SideNav.tsx`
- `frontend/src/components/ErrorPanel.tsx`
- `frontend/src/components/LoadingBlock.tsx`
- `frontend/src/components/StatusBadge.tsx`
- `frontend/src/components/SessionCard.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/components/ModelSelect.tsx`
- `frontend/src/components/PromptComposer.tsx`
- `frontend/src/components/RunTimeline.tsx`
- `frontend/src/pages/OverviewPage.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/pages/ControlPage.tsx`
- `frontend/src/pages/RunsPage.tsx`
- `frontend/src/pages/SettingsPage.tsx`
- `frontend/src/state/useSessionsState.ts`
- `frontend/src/styles/global.css`
- `doc/audit/mobile-console-ui.md`

Failing verification:

```bash
cd frontend && npm run build
```

Implementation:

1. Keep all UI data sourced from existing API calls only: `getHealth`, `listSessions`, `listModels`, `startRun`, `stopRun`, and real SSE events.
2. Do not add fake sessions, fake models, fake run ids, cached successful responses, or fallback successful arrays.
3. Settings must prefill Base URL with `window.location.origin` when no saved base URL exists.
4. Overview must run real health check on mount.
5. Overview must show token-missing state before protected API calls when `lqtigee_token` is absent.
6. Overview may call `listSessions` and `listModels` only when a token is stored.
7. Overview must display counts and source breakdown only from successful real API responses.
8. Sessions page must keep loading, error, empty, and success states distinct.
9. Sessions page must add source filters and text search that operate only on loaded real sessions.
10. Session selection must persist locally so Control can continue from the selected real session after navigation.
11. Control page must keep model choices filtered by the selected real session source.
12. Control page must not call `startRun` unless validation passes.
13. Runs page must render only actual SSE events or the no-run state.
14. CSS must be mobile-first, fit at 320px width, avoid horizontal overflow, avoid nested UI cards, avoid mock explanatory panels, and avoid a one-hue palette.
15. Add a short audit note with the selected ticket, real-data rules, and verification result.

Verification:

```bash
cd frontend && npm run build
rg "mock|fake|placeholder|sample session|sample model" frontend/src
```

### PUBLIC-ACCESS-M003 Rebuild Public Entry With Mobile Console UI

Symptom:

`BUG-MOBILE-CONSOLE-M001` repaired the mobile console source, but the currently running public `20261` service may still serve the old bundled PWA assets.

Expected:

The public URL `http://118.24.15.133:20261` serves the rebuilt mobile console UI while forwarding to the local machine's Java service that reads the current local Codex/opencode sessions.

Actual:

The running service was started before the latest frontend build and must be rebuilt/restarted before the phone sees the repaired UI.

Allowed files:

- `doc/audit/public-access.md`

Implementation:

1. Build `frontend/dist` from the committed frontend source.
2. Package the Spring Boot jar so the repaired PWA assets are served from backend port `20261`.
3. Restart only the local Lqtigee Java service that owns the current local Codex/opencode session files.
4. Keep the public server as an access mapping layer only.
5. Do not switch the backend data source to the public server.
6. Do not run live Codex/opencode commands.
7. Verify public `/api/health`.
8. Verify public `/sessions` returns the PWA shell.
9. Verify authenticated public `/api/sessions` returns real session data and source counts from the local service.
10. Record the evidence without printing the API token.

Verification:

```bash
cd frontend && npm install && npm run build
mvn package -DskipTests
systemctl --user status lqtigee-spark-ai-public-test.service
curl -sS --max-time 10 http://118.24.15.133:20261/api/health
curl -sS --max-time 10 http://118.24.15.133:20261/sessions | rg 'id="root"|manifest.webmanifest'
curl -sS --max-time 20 -H "Authorization: Bearer <token>" http://118.24.15.133:20261/api/sessions
rm -rf frontend/node_modules frontend/package-lock.json frontend/dist
```

### PLAN-CHAT-CONTROL-M001 Create Next-Phase Session Chat Control Plan

Symptom:

The app can list real local Codex/opencode sessions and open a selected session transcript, but the next implementation phase is not decomposed tightly enough for future AI agents to continue without broad edits.

Expected:

A detailed next-phase plan exists that turns the selected session chat screen into the primary phone control surface through small, ordered tickets. The plan must name exact existing files, exact existing API functions, non-negotiable no-mock rules, stop conditions, verification gates, and the safe next ticket.

Actual:

The current documents describe the implemented transcript UI and runtime API, but they do not yet provide a new method-level plan for continuing from a chat session, streaming the real run inline, refreshing the real transcript, and keeping public `20261` mapped to this local machine.

Allowed files:

- `doc/next-phase-chat-control-plan.md`
- `doc/micro-tickets.md`

Implementation:

1. Create `doc/next-phase-chat-control-plan.md`.
2. State that phone UI data must come only from the real Java service on port `20261`.
3. State that the public server is only a mapping layer and must not become the Codex/opencode data source.
4. State that current session discovery remains local Codex JSONL and local opencode SQLite.
5. State that PostgreSQL stores only Lqtigee-owned persistent data and must not replace live session discovery in this phase.
6. Anchor the plan to existing frontend functions: `listSessions`, `getSessionTranscript`, `listModels`, `startRun`, `openRunEvents`, and `stopRun`.
7. Anchor the plan to existing frontend state/components: `useSessionsState`, `useSessionTranscriptState`, `useModelsState`, `PromptComposer`, `RunTimeline`, and `SessionDetail`.
8. Anchor the plan to existing backend endpoints: `GET /api/sessions`, `GET /api/sessions/{source}/{id}/transcript`, `GET /api/models`, `POST /api/runs`, `GET /api/runs/{runId}/events`, and `POST /api/runs/{runId}/stop`.
9. Split follow-up work into micro tickets where each ticket touches only one narrow layer.
10. For each follow-up ticket, write purpose, allowed files, exact method/function changes, stop conditions, and verification.
11. Add the follow-up micro tickets to `doc/micro-tickets.md`.
12. Do not add mock data, fake transcripts, fake model lists, fake SSE events, or generated sample sessions.
13. Do not change application code in this planning ticket.

Verification:

```bash
test -f doc/next-phase-chat-control-plan.md
rg "PLAN-CHAT-CONTROL-M001|CHAT-UX-M001|CHAT-RUN-M001|CHAT-RUN-M006|PUBLIC-ACCESS-M006|No mock|startRun|openRunEvents|stopRun|PostgreSQL|118.24.15.133|20261" doc/next-phase-chat-control-plan.md doc/micro-tickets.md
```

### CHAT-UX-M001 Persist Selected Session As Source/ID Tuple

Symptom:

The frontend currently persists the selected session by id only. The next chat-control phase needs source/id identity so the phone cannot accidentally open or run a session from the wrong source if Codex and opencode ids collide.

Expected:

Selection is represented by `{ source, id }`, persisted locally as source plus id, and cleared when the loaded real session list does not contain that exact source/id pair.

Actual:

`useSessionsState` stores `selectedSessionId` with one localStorage key and pages search only by id.

Allowed files:

- `frontend/src/types/api.ts`
- `frontend/src/state/useSessionsState.ts`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/pages/ControlPage.tsx`

Implementation:

1. In `frontend/src/types/api.ts`, add `SelectedSessionRef` with fields `source: AgentSource` and `id: string`.
2. In `useSessionsState`, replace id-only selection state with `selectedSessionRef: SelectedSessionRef | null`.
3. Use localStorage keys `lqtigee_selected_session_source` and `lqtigee_selected_session_id`.
4. On initial state load, return `null` unless both persisted source and id exist and source is `CODEX` or `OPENCODE`.
5. In `loadSessions()`, keep the selected ref only if `response.sessions.some(session => session.source === ref.source && session.id === ref.id)`.
6. If the selected ref is stale, remove both localStorage keys.
7. Expose `selectSession(session: RemoteSession | null): void`.
8. Expose `clearSelectedSession(): void` if needed by the pages.
9. Update `SessionsPage` to find the selected session by both source and id.
10. Update `SessionsPage.handleSelectSession` to pass the real `RemoteSession`, not only the id.
11. Update `ControlPage` to find and select sessions by source/id.
12. Do not change API response shape.
13. Do not change backend code.
14. Do not add any fake selected session when the persisted session is stale.

Stop conditions:

- Stop if another frontend file must be changed to compile.
- Stop if a selected session cannot be represented without changing backend DTOs.
- Stop if any implementation path would keep an id-only fallback as the selected source of truth.

Verification:

```bash
cd frontend && npm run build
rg "SelectedSessionRef|lqtigee_selected_session_source|lqtigee_selected_session_id|selectedSessionRef|selectSession" frontend/src/types/api.ts frontend/src/state/useSessionsState.ts frontend/src/pages/SessionsPage.tsx frontend/src/pages/ControlPage.tsx
```

### CHAT-UX-M002 Sync Selected Chat With URL Query

Symptom:

The phone can open a chat by tapping a session, but reloading or sharing the page does not preserve the selected chat in the browser URL.

Expected:

`/sessions?source=CODEX&sessionId=<id>` or `/sessions?source=OPENCODE&sessionId=<id>` restores the selected real chat after sessions load. Invalid query values do not create fake selections.

Actual:

Selection is local state/localStorage only.

Allowed files:

- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Add a helper `readSelectedSessionQuery(search: string): SelectedSessionRef | null`.
2. `readSelectedSessionQuery` must accept only `source=CODEX` or `source=OPENCODE`.
3. `readSelectedSessionQuery` must return `null` if `sessionId` is missing or blank.
4. Add a helper `writeSelectedSessionQuery(ref: SelectedSessionRef | null): void`.
5. `writeSelectedSessionQuery` must use `window.history.replaceState`, not full navigation.
6. After sessions are loaded, if the URL query contains a valid source/id and the loaded real sessions contain that exact session, call `sessionsState.selectSession(realSession)`.
7. If the URL query points to no loaded real session, keep no selected chat and do not invent a session.
8. When selecting a session from the list, update the URL query with the session source/id.
9. When backing out of chat, clear both selected state and URL query.
10. Do not change `ControlPage`.
11. Do not call `getSessionTranscript` until a real selected session object exists.

Stop conditions:

- Stop if URL restoration would select a session before `/api/sessions` succeeds.
- Stop if URL query parsing requires backend changes.
- Stop if invalid query values are being converted into a success selection.

Verification:

```bash
cd frontend && npm run build
rg "readSelectedSessionQuery|writeSelectedSessionQuery|URLSearchParams|replaceState|sessionId" frontend/src/pages/SessionsPage.tsx
```

### CHAT-UX-M003 Make Session Chat Panel Mobile-First Control Surface

Symptom:

The current chat panel works functionally but still reads like a basic page section instead of a focused phone control surface for real Codex/opencode sessions.

Expected:

The selected session chat is visually clear on phone: compact header, source/model metadata, readable message bubbles, stable back action, no horizontal overflow, and desktop two-column layout preserved.

Actual:

The chat view is present, but spacing, visual hierarchy, and mobile control-surface polish need a dedicated narrow UI pass.

Allowed files:

- `frontend/src/components/SessionCard.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/styles/global.css`
- `doc/audit/mobile-console-ui.md`

Implementation:

1. Keep all displayed business data from props only.
2. In `SessionCard`, keep source, title, model, workspace, updated time, and last message preview visible without adding fake copy.
3. In `SessionDetail`, keep the selected session title as the primary chat header.
4. Render source/model as compact metadata, not as a large marketing header.
5. Keep workspace and updated time readable but secondary.
6. Keep chat messages in the existing `transcript.messages.map` loop only.
7. Use distinct user and assistant message alignment or tone through CSS classes.
8. Ensure long paths, model ids, and message text wrap instead of overflowing.
9. Preserve the mobile back action.
10. Preserve the desktop list-plus-chat layout.
11. Do not add visible text that explains features, keyboard shortcuts, or implementation details.
12. Do not add decorative orbs, gradients, or fake preview blocks.
13. Update `doc/audit/mobile-console-ui.md` with the ticket id and build result only; do not record transcript text.

Stop conditions:

- Stop if the UI change would require backend/API changes.
- Stop if any placeholder business text is needed to make the layout look full.
- Stop if the CSS causes horizontal overflow at 320px by inspection.

Verification:

```bash
cd frontend && npm run build
rg "CHAT-UX-M003|mobile-first|320px|no mock" doc/audit/mobile-console-ui.md
! rg "sample message|fake chat|mock transcript|placeholder session|demo session" frontend/src
```

### CHAT-RUN-M001 Add Session Chat Run State Hook

Symptom:

`RunsPage` can subscribe to run events, but the selected chat panel does not have isolated state for starting a real run, tracking real SSE events, stopping the run, and detecting terminal events inline.

Expected:

A frontend hook exists for chat-owned runs. It reuses `startRun`, `openRunEvents`, and `stopRun`, stores only real run ids and real events, and exposes a small state surface for the chat composer.

Actual:

Chat UI has transcript state only. Run state lives only in `RunsPage` and `useRunEvents`.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`

Implementation:

1. Create `useSessionChatRunState.ts`.
2. Import `startRun`, `openRunEvents`, and `stopRun` from `../api/remoteApi`.
3. Import `RunEventDto` and `StartRunRequest` from `../types/api`.
4. Define `TERMINAL_EVENT_TYPES = new Set(["done", "error", "stopped"])`.
5. Define state fields: `runId`, `events`, `error`, `starting`, `stopping`, and `terminal`.
6. Implement `startSessionRun(request: StartRunRequest, onTerminal?: (event: RunEventDto) => void): Promise<void>`.
7. `startSessionRun` must clear previous events and errors only after frontend validation has called it.
8. `startSessionRun` must call `startRun(request)` and store the returned real `runId`.
9. `startSessionRun` must call `openRunEvents(response.runId, handlers)`.
10. `handlers.onEvent` must append the real event object to `events`.
11. If the event type is terminal, mark terminal state and call `onTerminal` with the real event.
12. `handlers.onError` must store the real caught error.
13. Implement `stopActiveRun(): Promise<void>` that calls `stopRun(runId)` only when a real `runId` exists.
14. Implement `clearRun(): void` that closes the current stream if it exists and clears run state.
15. Close the active stream when the hook unmounts or starts a replacement run.
16. Do not synthesize run events.
17. Do not append prompt text as an event.
18. Do not navigate to `/runs`.

Stop conditions:

- Stop if `openRunEvents` must change response parsing to support this hook.
- Stop if TypeScript cannot type the stream without changing `remoteApi.ts`.
- Stop if frontend code would need fake events to test rendering.

Verification:

```bash
cd frontend && npm run build
rg "useSessionChatRunState|startSessionRun|openRunEvents|stopActiveRun|TERMINAL_EVENT_TYPES|done|error|stopped" frontend/src/state/useSessionChatRunState.ts
```

### CHAT-RUN-M002 Add Session Chat Composer Component

Symptom:

The app has `PromptComposer`, `ModelSelect`, and `RunTimeline`, but no component that combines them for a selected session chat without redirecting to `/runs`.

Expected:

`SessionChatComposer` renders model selection, prompt/mode controls, validation, run button, stop button, and inline real run events for one selected real session.

Actual:

The only command form lives in `ControlPage`.

Allowed files:

- `frontend/src/components/SessionChatComposer.tsx`

Implementation:

1. Create `SessionChatComposer.tsx`.
2. Import `ModelSelect`, `PromptComposer`, and `RunTimeline`.
3. Import `CommandMode`, `ModelDto`, `RemoteSession`, `RunEventDto`, and `StartRunRequest`.
4. Define props:
   - `session: RemoteSession`
   - `models: ModelDto[]`
   - `disabled: boolean`
   - `runId: string`
   - `events: RunEventDto[]`
   - `runError: unknown`
   - `starting: boolean`
   - `stopping: boolean`
   - `terminal: boolean`
   - `onStart(request: StartRunRequest): void`
   - `onStop(): void`
5. Keep local form state for `modelId`, `prompt`, `mode`, and `confirmDangerous`.
6. Compute `availableModels` with `model.enabled && model.sources.includes(session.source)`.
7. Clear `modelId` when the selected model is no longer available for the current session source.
8. Implement `validateSessionChatForm`.
9. Validation errors must include missing model, unsupported model, blank prompt, and missing dangerous confirmation for `SHELL`.
10. `handleSubmit` must build `StartRunRequest` from the real `session`, selected real model id, mode, prompt, and confirmation.
11. `handleSubmit` must call `onStart(request)`.
12. Render `RunTimeline` only from `events`.
13. Render stop button only when `runId` exists and `terminal` is false.
14. Render real run errors through `ErrorPanel`.
15. Do not call backend APIs directly in this component.
16. Do not create sample models, sample prompts, sample events, or sample messages.

Stop conditions:

- Stop if `PromptComposer` must be changed to support this component.
- Stop if a fake disabled run state is needed to compile.
- Stop if component props require a backend contract change.

Verification:

```bash
cd frontend && npm run build
rg "SessionChatComposer|validateSessionChatForm|StartRunRequest|RunTimeline|onStart|onStop" frontend/src/components/SessionChatComposer.tsx
! rg "sample|fake|mock|demo" frontend/src/components/SessionChatComposer.tsx
```

### CHAT-RUN-M003 Mount Real Chat Composer In Session Detail

Symptom:

The selected session chat renders real transcript messages but still cannot start a real selected-session run from inside the chat panel.

Expected:

`SessionsPage` loads real models, owns a chat run hook, passes real model/run state into `SessionDetail`, and `SessionDetail` renders `SessionChatComposer` for the selected real session. Starting a run from chat calls the existing `/api/runs` backend through `startRun`.

Actual:

`SessionDetail` is transcript-only.

Allowed files:

- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. In `SessionsPage`, import `useModelsState`.
2. In `SessionsPage`, import `useSessionChatRunState`.
3. In `SessionsPage`, load models with `modelsState.loadModels()` when a token exists, alongside sessions.
4. Keep sessions and models errors distinct.
5. Create `chatRunState = useSessionChatRunState()`.
6. Pass selected `modelsState.models`, model loading/error state, and `chatRunState` fields into `SessionDetail`.
7. Pass an `onStartChatRun(request)` function that calls `chatRunState.startSessionRun(request)`.
8. Pass an `onStopChatRun()` function that calls `chatRunState.stopActiveRun()`.
9. In `SessionDetail`, import and render `SessionChatComposer` only when `session` exists.
10. `SessionDetail` must keep transcript loading/error/empty/success states distinct.
11. `SessionDetail` must not navigate to `/runs` after starting a chat run.
12. `SessionDetail` must not append prompt text or run events to `transcript.messages`.
13. Add CSS only for composer placement, run timeline containment, and mobile wrapping.
14. Do not change `ControlPage`.
15. Do not change backend code.

Stop conditions:

- Stop if model loading requires a backend contract change.
- Stop if `SessionDetail` cannot receive run state without changing `SessionTranscriptDto`.
- Stop if inline run display would require fake events.

Verification:

```bash
cd frontend && npm run build
rg "useModelsState|useSessionChatRunState|SessionChatComposer|onStartChatRun|onStopChatRun" frontend/src/pages/SessionsPage.tsx frontend/src/components/SessionDetail.tsx
! rg "fake event|mock event|sample event|demo run" frontend/src
```

### CHAT-RUN-M004 Refresh Real Transcript After Terminal Chat Run

Symptom:

After a chat-started run reaches a terminal event, the inline event timeline can show completion, but the chat transcript must refresh from the real backend so the phone sees the updated session conversation.

Expected:

When a real terminal run event arrives for the currently selected source/id, the frontend reloads the transcript through `getSessionTranscript` and reloads the session list through `listSessions` for updated title/preview/time.

Actual:

The current transcript hook reloads only on selection changes.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Update `startSessionRun` so callers can pass terminal metadata or a callback without changing backend API shape.
2. When `handlers.onEvent` receives a terminal event, call the callback exactly once for that run.
3. In `SessionsPage`, capture the selected session source/id at run start.
4. In the terminal callback, compare the captured source/id with the current selected session source/id.
5. If they match, call `transcriptState.loadTranscript(source, id)`.
6. If they match, call `sessionsState.loadSessions()` after or alongside transcript reload.
7. If selected session changed, do not refresh the old transcript into the new chat.
8. Do not append the prompt, stdout, stderr, or terminal message to `transcript.messages`.
9. Do not hide transcript reload errors.
10. Do not synthesize assistant messages when transcript reload returns no new messages.

Stop conditions:

- Stop if terminal event callback can run more than once for one run.
- Stop if stale session comparison cannot be implemented without global mutable state.
- Stop if transcript refresh would require backend contract changes.

Verification:

```bash
cd frontend && npm run build
rg "onTerminal|loadTranscript|loadSessions|terminal" frontend/src/state/useSessionChatRunState.ts frontend/src/pages/SessionsPage.tsx
! rg "transcript\\.messages.*push|setTranscript\\(.*events|fake chat|mock transcript" frontend/src
```

### CHAT-RUN-M005 Guard Inline Chat Run Against Double Start And Stale Session

Symptom:

Inline chat control can become confusing if the user taps Run twice quickly or switches sessions while a run stream is active.

Expected:

The chat run hook and composer prevent double starts for a non-terminal active run and make stale-session behavior explicit without silently applying events to another selected chat.

Actual:

The first chat run hook ticket creates base state only.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. In `useSessionChatRunState`, add `activeSessionRef` if not already present.
2. `startSessionRun` must reject or no-op with a real frontend error if `runId` exists and `terminal` is false.
3. Store the source/id of the session used to start the run.
4. Expose the active source/id to consumers.
5. In `SessionChatComposer`, disable Run when a non-terminal run is active.
6. In `SessionsPage`, when selected session changes while a run is active, do not move that run's events into the new session chat.
7. If clearing the selected session while a run is active, leave the run state consistent and do not fabricate stop success.
8. Keep Stop available only for the active real run id.
9. Do not auto-stop a process just because the user changed selected session.
10. Do not create fake stale-session warning events.

Stop conditions:

- Stop if preventing double start requires backend changes.
- Stop if stale-session handling would require an invented run ownership endpoint.
- Stop if a UI workaround would hide real SSE errors.

Verification:

```bash
cd frontend && npm run build
rg "activeSessionRef|non-terminal|terminal|runId|disabled" frontend/src/state/useSessionChatRunState.ts frontend/src/components/SessionChatComposer.tsx frontend/src/pages/SessionsPage.tsx
! rg "fake.*stale|mock.*run|demo.*run" frontend/src
```

### CHAT-RUN-M006 Capture Real Inline Chat Control Evidence

Symptom:

After inline chat control is implemented, there must be real evidence that the phone-facing flow uses current local Codex/opencode sessions, real backend models, real `/api/runs`, real SSE, and real transcript refresh.

Expected:

An audit document records real command/API evidence without printing the token, prompt text, full transcript text, or secrets.

Actual:

No post-inline-chat-control evidence exists.

Allowed files:

- `doc/audit/chat-control-live-evidence.md`
- `doc/audit/release-checklist-status.md`

Implementation:

1. Require the inline chat control code tickets to be committed first.
2. Read the token from the local configured token source without printing it.
3. Verify public or local `/api/health` returns the Lqtigee Java service on port `20261`.
4. Call authenticated `/api/sessions`.
5. Record total real session count and source breakdown only.
6. Select one real candidate session from the API response; record source, id prefix, model, workspace, and updated time only.
7. Call authenticated `/api/sessions/{source}/{id}/transcript`.
8. Record message count only; do not record transcript text.
9. Call authenticated `/api/models`.
10. Record configured enabled model ids and supported sources.
11. Start one real run through `/api/runs` only if the selected session has a source-supported enabled model.
12. Record returned `runId`, status, and start elapsed time.
13. Subscribe to `/api/runs/{runId}/events`.
14. Record real event types, terminal type, terminal count, and whether the response completed.
15. If no terminal event arrives in the evidence window, call `/api/runs/{runId}/stop` and record the real stop result.
16. After terminal or stop, call transcript endpoint again and record message count only.
17. Mark `PASS` only if all real calls succeed according to the ticket criteria.
18. Mark `FAIL` with exact failing endpoint and status if any call fails.
19. Do not fabricate events or terminal results.
20. Update release checklist status only if evidence passes.

Stop conditions:

- Stop if token is missing.
- Stop if `/api/sessions` fails.
- Stop if no real source-supported model exists for the selected real session.
- Stop if starting a real run would require a prompt that exposes secret data.
- Stop if SSE receives no terminal event after one stop attempt.

Verification:

```bash
test -f doc/audit/chat-control-live-evidence.md
rg "CHAT-RUN-M006|PASS|FAIL|session count|source breakdown|runId|terminal type|terminal count|transcript message count|no fake events|no transcript text" doc/audit/chat-control-live-evidence.md doc/audit/release-checklist-status.md
```

### PUBLIC-ACCESS-M006 Rebuild Public Entry With Inline Chat Control

Symptom:

After inline chat control is built, the public URL may still serve an older frontend bundle or a service instance that does not include the chat composer and inline run behavior.

Expected:

`http://118.24.15.133:20261` serves the rebuilt app bundle and forwards to the local Java service that reads current local Codex/opencode sessions.

Actual:

No public evidence exists for the inline chat-control bundle.

Allowed files:

- `doc/audit/public-access.md`

Implementation:

1. Build the frontend bundle from committed source.
2. Package the Spring Boot jar with the rebuilt frontend assets.
3. Restart only the local user service that owns the current local Codex/opencode session files.
4. Keep the public server as a mapping layer only.
5. Do not move Codex/opencode discovery to the public server.
6. Verify public `/api/health`.
7. Verify public `/sessions` serves the current app shell and current asset names.
8. Verify authenticated public `/api/sessions` returns real session counts from this local machine.
9. Verify authenticated public transcript endpoint returns a `messages` array for one real Codex or opencode session.
10. Verify the served JS bundle contains the chat composer symbols or other deterministic inline-chat-control asset evidence.
11. Record asset names, source counts, and endpoint status only.
12. Do not print the API token.
13. Do not record transcript text.
14. Do not run live Codex/opencode commands in this public rebuild ticket; live run evidence belongs to `CHAT-RUN-M006`.

Stop conditions:

- Stop if the service restart points to a backend not running on this local machine.
- Stop if public `/api/sessions` returns a source breakdown inconsistent with the local service.
- Stop if token would need to be printed to document the result.
- Stop if public URL is mistaken for Android installability evidence; plain HTTP fixed IP is browser access only.

Verification:

```bash
cd frontend && npm install && npm run build
mvn package -DskipTests
systemctl --user status lqtigee-spark-ai-public-test.service
curl -sS --max-time 10 http://118.24.15.133:20261/api/health
curl -sS --max-time 10 http://118.24.15.133:20261/sessions | rg 'id="root"|manifest.webmanifest'
curl -sS --max-time 20 -H "Authorization: Bearer <token>" http://118.24.15.133:20261/api/sessions
rg "PUBLIC-ACCESS-M006|118.24.15.133|20261|inline chat control|session count|CODEX|OPENCODE|no transcript text|NOT_ANDROID_INSTALLABILITY" doc/audit/public-access.md
```

### PLAN-MOBILE-CHAT-M001 Create Full Mobile Chat Console Microtask Backlog

Symptom:

The current next-phase plan still does not enumerate every user-visible function needed for a usable phone chat console. It also does not fully cover the missing chat input, inline streaming, newest-10 transcript paging, Codex CLI option surface, opencode CLI option surface, session management, attachments, mobile ergonomics, and public verification tasks.

Expected:

A dedicated doc file exists under `doc/` that decomposes the full mobile Codex/opencode chat console into microtasks small enough for future AI agents to execute one at a time without broad edits.

Actual:

`doc/next-phase-chat-control-plan.md` defines the immediate chat-control direction, but it is not a full feature backlog for the app.

Allowed files:

- `doc/mobile-chat-console-microtasks.md`

Implementation:

1. Create `doc/mobile-chat-console-microtasks.md`.
2. State that this is a task backlog only and must not change application code.
3. State that no mock data, fake sessions, fake models, fake messages, fake SSE events, or smoke-only verification may be used.
4. Include a source-of-truth section for local Codex JSONL, local opencode SQLite, backend port `20261`, public mapping, and PostgreSQL boundary.
5. Include a verified CLI capability section based on local `codex`, `codex exec resume`, `opencode run`, `opencode session`, and `opencode agent` help.
6. Split backend contract work into separate microtasks before implementation tasks.
7. Split transcript paging into backend cursor, frontend state, and UI tasks.
8. Split chat composer into input, model, mode, permission, attachment, and send tasks.
9. Split inline streaming into run state, SSE timeline, terminal handling, stop, and stale-session tasks.
10. Split Codex-specific controls into image, profile, sandbox, approval, search, add-dir, config override, and dangerous confirmation tasks.
11. Split opencode-specific controls into agent, file attachment, fork, variant, thinking, replay-limit, share, and dangerous confirmation tasks.
12. Split session management into archive/delete/fork/export/import tasks with confirmations and CLI evidence gates.
13. Split mobile UI into keyboard, safe area, bottom composer, scroll anchoring, and 320px layout tasks.
14. Split public verification and release evidence into real endpoint tasks that do not print tokens or transcript text.
15. For every microtask, include allowed files, exact implementation points, stop conditions, and verification.

Verification:

```bash
test -f doc/mobile-chat-console-microtasks.md
rg "PLAN-MOBILE-CHAT-M001|Newest 10|bottom composer|inline SSE|Codex controls|opencode controls|attachments|session management|public 20261|No mock|No smoke" doc/mobile-chat-console-microtasks.md
```

### MOBILE-CONTRACT-M001 Add Paged Transcript Contract

Purpose:

Define the API contract for newest-10 transcript paging.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`

Implementation:

1. Add `GET /api/sessions/{source}/{id}/transcript?limit=10`.
2. Add optional `before=<cursor>` for older messages.
3. Add `TranscriptPageInfoDto` with `oldestCursor`, `newestCursor`, and `hasMoreBefore`.
4. Add `messages` sorted oldest-to-newest within the returned page.
5. State default limit is `10`.
6. State maximum limit is backend-configured and must not be unbounded.
7. State no generated summary and no fake messages.

Stop conditions:

- Stop if cursor cannot be defined without source-specific assumptions.
- Stop if contract would expose raw prompt text in audit docs.

Verification:

```bash
rg "TranscriptPageInfoDto|limit=10|before=|hasMoreBefore|oldestCursor|newestCursor" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
```

### MOBILE-CONTRACT-M004 Add Source Capability Contract

Purpose:

Let the frontend know which controls are available for Codex and opencode without hardcoding fake capability switches.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`

Implementation:

1. Add `GET /api/capabilities`.
2. Add `SourceCapabilityDto` with `source`, `runOptions`, `attachments`, `sessionActions`, and `dangerousOptions`.
3. Populate contract examples only from verified local CLI help.
4. State runtime code must not claim a capability until backend validation and command builder tests exist.

Stop conditions:

- Stop if capabilities would be hardcoded in frontend.
- Stop if a capability has no local CLI evidence.

Verification:

```bash
rg "/api/capabilities|SourceCapabilityDto|runOptions|sessionActions|dangerousOptions" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
```

### MOBILE-BE-PAGE-M001 Add Transcript Page DTOs

Purpose:

Create backend DTOs for paged transcript responses.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/dto/SessionTranscriptDto.java`
- `src/main/java/com/lqtigee/sparkai/dto/SessionMessageDto.java`
- `src/test/java/com/lqtigee/sparkai/dto/SessionTranscriptDtoTest.java`

Implementation:

1. Add `TranscriptPageInfoDto`.
2. Add page info to `SessionTranscriptDto`.
3. Keep message fields unchanged.
4. Do not add generated summaries.

Stop conditions:

- Stop if frontend contract was not updated first.

Verification:

```bash
mvn test -Dtest=SessionTranscriptDtoTest
rg "TranscriptPageInfoDto|hasMoreBefore|oldestCursor|newestCursor" src/main/java src/test/java
```

### MOBILE-BE-PAGE-M002 Page Codex Transcript Reader

Purpose:

Return newest 10 visible Codex messages and older pages from a real JSONL file.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexTranscriptReader.java`
- `src/test/java/com/lqtigee/sparkai/codex/CodexTranscriptReaderTest.java`
- `src/test/resources/samples/codex-transcript-sample.jsonl`

Implementation:

1. Add method `readPage(Path rawFile, int limit, String beforeCursor)`.
2. Cursor must be derived from real line/message position, not from fake ids.
3. Default to newest 10 messages when `beforeCursor` is absent.
4. Exclude developer, system, tool, reasoning, encrypted, snapshot, and empty text records.
5. Return messages oldest-to-newest within the page.
6. Set `hasMoreBefore` from real available older visible messages.

Stop conditions:

- Stop if Codex cursor cannot be stable from the parsed JSONL structure.
- Stop if a missing field would be filled from filename fallback.

Verification:

```bash
mvn test -Dtest=CodexTranscriptReaderTest
rg "readPage|beforeCursor|hasMoreBefore|limit" src/main/java/com/lqtigee/sparkai/codex src/test/java/com/lqtigee/sparkai/codex
```

### MOBILE-BE-PAGE-M003 Page opencode Transcript Reader

Purpose:

Return newest 10 visible opencode messages and older pages from the real SQLite tables.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteTranscriptReader.java`
- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteTranscriptReaderTest.java`

Implementation:

1. Add method `readPage(Path database, String sessionId, int limit, String beforeCursor)`.
2. Cursor must come from real message/part ordering fields.
3. Default to newest 10 visible user/assistant text messages.
4. Exclude non-text parts and empty text.
5. Return messages oldest-to-newest within the page.
6. Set `hasMoreBefore` only from real older visible rows.

Stop conditions:

- Stop if SQLite schema does not expose stable ordering.
- Stop if reader would need fake cursor values.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteTranscriptReaderTest
rg "readPage|beforeCursor|hasMoreBefore|limit" src/main/java/com/lqtigee/sparkai/opencode src/test/java/com/lqtigee/sparkai/opencode
```

### MOBILE-BE-PAGE-M004 Add Paged Transcript Endpoint

Purpose:

Wire query params into the transcript service and controller.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/SessionTranscriptService.java`
- `src/main/java/com/lqtigee/sparkai/web/SessionController.java`
- `src/test/java/com/lqtigee/sparkai/service/SessionTranscriptServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/SessionControllerTest.java`

Implementation:

1. Accept `limit` and `before`.
2. Default `limit` to 10.
3. Reject limit less than 1.
4. Reject limit greater than configured maximum.
5. Validate selected real session before reading transcript.
6. Dispatch to source-specific paged reader.
7. Do not convert reader failures into empty success.

Stop conditions:

- Stop if service needs controller filesystem access.
- Stop if a failed source would return an empty page.

Verification:

```bash
mvn test -Dtest=SessionTranscriptServiceTest,SessionControllerTest
rg "RequestParam|limit|before|TranscriptPageInfoDto" src/main/java/com/lqtigee/sparkai/service src/main/java/com/lqtigee/sparkai/web src/test/java
```

### MOBILE-FE-PAGE-M001 Add Paged Transcript Types And API

Purpose:

Teach the frontend client about page info and cursor query params.

Allowed files:

- `frontend/src/types/api.ts`
- `frontend/src/api/remoteApi.ts`

Implementation:

1. Add `TranscriptPageInfoDto`.
2. Add `pageInfo` to `SessionTranscriptDto`.
3. Change `getSessionTranscript(source, id, options)` to accept `limit` and `before`.
4. Default caller behavior must still request real backend data.

Stop conditions:

- Stop if backend contract is not updated.

Verification:

```bash
cd frontend && npm run build
rg "TranscriptPageInfoDto|pageInfo|before|limit" frontend/src/types/api.ts frontend/src/api/remoteApi.ts
```

### MOBILE-FE-PAGE-M002 Add Transcript Paging State Hook

Purpose:

Load newest 10 messages first and prepend older pages when the user scrolls upward.

Allowed files:

- `frontend/src/state/useSessionTranscriptState.ts`

Implementation:

1. Replace single `loadTranscript` state with page-aware state.
2. Add `loadNewestTranscript(source, id)`.
3. Add `loadOlderMessages()`.
4. Store `messages`, `pageInfo`, `loadingNewest`, `loadingOlder`, `error`.
5. Prevent concurrent older-page requests.
6. Do not create local fake messages.

Stop conditions:

- Stop if API response lacks page info.

Verification:

```bash
cd frontend && npm run build
rg "loadNewestTranscript|loadOlderMessages|loadingOlder|hasMoreBefore|pageInfo" frontend/src/state/useSessionTranscriptState.ts
```

### MOBILE-FE-PAGE-M003 Show Newest 10 And Load Older At Top

Purpose:

Stop dumping the whole transcript and use chat-style history loading.

Allowed files:

- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Render only messages from paged transcript state.
2. Add a top load-older control when `hasMoreBefore` is true.
3. Trigger `loadOlderMessages` when the user scrolls near the top.
4. Keep loading older distinct from loading newest.
5. Preserve selected session title and metadata.
6. Do not hide API errors.

Stop conditions:

- Stop if all messages are still rendered on first load.

Verification:

```bash
cd frontend && npm run build
rg "loadOlderMessages|hasMoreBefore|loadingOlder|chat-history-top" frontend/src/components/SessionDetail.tsx frontend/src/pages/SessionsPage.tsx frontend/src/styles/global.css
```

### MOBILE-FE-PAGE-M004 Preserve Scroll Anchor When Older Messages Prepend

Purpose:

Prevent the chat viewport from jumping when older messages are loaded.

Allowed files:

- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Track scroll container before older load.
2. After prepending messages, restore visual anchor based on scrollHeight delta.
3. Keep newest page initially scrolled to bottom.
4. Do not force bottom scroll while the user is reading older messages.

Stop conditions:

- Stop if anchor restore requires changing backend API.

Verification:

```bash
cd frontend && npm run build
rg "scrollHeight|scrollTop|loadOlder|chat-scroll" frontend/src/components/SessionDetail.tsx frontend/src/styles/global.css
```

### MOBILE-COMPOSER-M001 Add Bottom Composer Shell

Purpose:

Add the always-accessible bottom composer in the selected session chat.

Allowed files:

- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Render composer at bottom of chat panel when a real session is selected.
2. Include textarea, send button, stop button slot, and options button slot.
3. Keep composer above mobile safe area.
4. Keep textarea usable with mobile keyboard.
5. Do not call APIs in this shell ticket.

Stop conditions:

- Stop if composer appears when no real session is selected.

Verification:

```bash
cd frontend && npm run build
rg "SessionChatComposer|bottom composer|chat-composer|safe-area-inset-bottom" frontend/src/components frontend/src/styles/global.css
```

### MOBILE-COMPOSER-M002 Add Prompt Draft State

Purpose:

Keep one draft prompt per selected source/id.

Allowed files:

- `frontend/src/state/useChatDraftState.ts`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/SessionDetail.tsx`

Implementation:

1. Create `useChatDraftState`.
2. Key drafts by `source:id`.
3. Store draft in localStorage.
4. Clear draft only after successful `POST /api/runs` returns a real run id.
5. Do not store API tokens with drafts.
6. Pass selected session source/id from the selected-session detail into the composer.

Stop conditions:

- Stop if draft key uses id only.

Verification:

```bash
cd frontend && npm run build
rg "useChatDraftState|source.*id|localStorage|clearDraft" frontend/src/state frontend/src/components/SessionChatComposer.tsx
```

### MOBILE-COMPOSER-M003 Add Model And Mode Controls

Purpose:

Let the user select real backend model and command mode directly in the chat composer.

Allowed files:

- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/ModelSelect.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Use models from `GET /api/models` only.
2. Filter by selected session source.
3. Add mode selector for `ASK`, `REVIEW`, `EDIT`, `SHELL`.
4. Require dangerous confirmation for `SHELL`.
5. Disable send when no real supported model exists.

Stop conditions:

- Stop if any model is hardcoded.

Verification:

```bash
cd frontend && npm run build
rg "ASK|REVIEW|EDIT|SHELL|confirmDangerous|ModelSelect" frontend/src/components/SessionChatComposer.tsx frontend/src/components/ModelSelect.tsx
! rg "sample model|fake model|mock model" frontend/src
```

### MOBILE-COMPOSER-M004 Wire Send To Real Run API

Purpose:

Send the composer prompt to the selected real session using `POST /api/runs`.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Build `StartRunRequest` from selected `RemoteSession`.
2. Include selected real `modelId`.
3. Include selected mode.
4. Include prompt.
5. Include dangerous confirmation.
6. Call `startRun`.
7. Store only returned real `runId`.
8. Do not navigate away from chat.

Stop conditions:

- Stop if prompt would be appended locally as a fake user message.

Verification:

```bash
cd frontend && npm run build
rg "startRun|StartRunRequest|runId|onStart" frontend/src/state/useSessionChatRunState.ts frontend/src/components/SessionChatComposer.tsx frontend/src/pages/SessionsPage.tsx
! rg "fake message|mock message|sample prompt" frontend/src
```

### MOBILE-COMPOSER-M005 Add Keyboard And Safe-Area Behavior

Purpose:

Make the composer usable on Android phone keyboards.

Allowed files:

- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Keep composer sticky at bottom of chat panel.
2. Use `env(safe-area-inset-bottom)`.
3. Ensure message list bottom padding accounts for composer height.
4. Keep send button visible at 320px width.
5. Prevent horizontal overflow.

Stop conditions:

- Stop if layout requires hiding the prompt input.

Verification:

```bash
cd frontend && npm run build
rg "safe-area-inset-bottom|position: sticky|chat-composer|padding-bottom" frontend/src/styles/global.css
```

### MOBILE-STREAM-M001 Add Chat Run State Hook

Purpose:

Own active run id, SSE stream, events, terminal state, and errors for one selected chat.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`

Implementation:

1. Call `startRun`.
2. Open `openRunEvents`.
3. Append only real `RunEventDto` events.
4. Detect `done`, `error`, and `stopped`.
5. Close stream on unmount.
6. Keep `starting`, `streaming`, `stopping`, `terminal`, and `error`.

Stop conditions:

- Stop if fake events are needed for UI state.

Verification:

```bash
cd frontend && npm run build
rg "useSessionChatRunState|openRunEvents|RunEventDto|done|error|stopped" frontend/src/state/useSessionChatRunState.ts
```

### MOBILE-STREAM-M002 Render Inline SSE Output

Purpose:

Show streaming output on the same chat screen so the phone can see live progress.

Allowed files:

- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/RunTimeline.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Render run events below or above composer inside chat panel.
2. Show event type and message.
3. Auto-scroll streaming area while user is at bottom.
4. Keep output scrollable on mobile.
5. Do not merge run events into transcript messages.

Stop conditions:

- Stop if stream output is only visible on `/runs`.

Verification:

```bash
cd frontend && npm run build
rg "RunTimeline|run-timeline|streaming|events" frontend/src/components/SessionChatComposer.tsx frontend/src/components/RunTimeline.tsx frontend/src/styles/global.css
```

### MOBILE-STREAM-M003 Add Stop Button For Active Run

Purpose:

Let phone stop the real running process from the chat screen.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/components/SessionChatComposer.tsx`

Implementation:

1. Enable stop only when real run id exists and no terminal event exists.
2. Call `stopRun(runId)`.
3. Show stopping state.
4. Do not synthesize stopped events.

Stop conditions:

- Stop if no real run id exists.

Verification:

```bash
cd frontend && npm run build
rg "stopRun|stopActiveRun|stopping|terminal" frontend/src/state/useSessionChatRunState.ts frontend/src/components/SessionChatComposer.tsx
```

### MOBILE-STREAM-M004 Refresh Transcript After Terminal Event

Purpose:

Reload real transcript after the selected session run completes or stops.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Capture source/id at send time.
2. On terminal event, compare captured source/id to current selected source/id.
3. Reload transcript if still selected.
4. Reload session list for updated title/preview/time.
5. Do not fake assistant messages.

Stop conditions:

- Stop if selected session changed and code would refresh the wrong chat.

Verification:

```bash
cd frontend && npm run build
rg "onTerminal|loadNewestTranscript|loadSessions|selectedSessionRef" frontend/src/state/useSessionChatRunState.ts frontend/src/pages/SessionsPage.tsx
! rg "setTranscript.*event|fake assistant|mock transcript" frontend/src
```

### MOBILE-STREAM-M005 Guard Double Start And Stale Stream

Purpose:

Prevent double sends and keep active stream tied to the session that started it.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Store active session ref with the run.
2. Disable send while a non-terminal run is active.
3. Keep stop bound to active run.
4. Do not auto-stop when switching sessions.
5. Do not show old stream as current session output.

Stop conditions:

- Stop if run ownership would require an invented backend endpoint.

Verification:

```bash
cd frontend && npm run build
rg "activeSessionRef|nonTerminal|terminal|disabled" frontend/src/state/useSessionChatRunState.ts frontend/src/components/SessionChatComposer.tsx frontend/src/pages/SessionsPage.tsx
```

### MOBILE-CODEX-M001 Record Codex CLI Option Evidence

Purpose:

Record current local Codex help evidence before adding UI controls.

Allowed files:

- `doc/discovery/codex-chat-controls.md`

Implementation:

1. Record command used: `codex exec resume --help`.
2. Record command used: `codex resume --help`.
3. Summarize option names only.
4. Do not record auth files or secrets.

Stop conditions:

- Stop if Codex command is unavailable.

Verification:

```bash
test -f doc/discovery/codex-chat-controls.md
rg "exec resume|--image|--model|--config|--json|--sandbox|--ask-for-approval|--search|--add-dir" doc/discovery/codex-chat-controls.md
```

### MOBILE-CODEX-M002 Add Codex Options DTO

Purpose:

Create typed backend request fields for Codex-only options.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `src/main/java/com/lqtigee/sparkai/dto/StartRunRequest.java`
- `src/main/java/com/lqtigee/sparkai/dto/CodexRunOptionsDto.java`
- `src/test/java/com/lqtigee/sparkai/dto/StartRunRequestTest.java`

Implementation:

1. Add `CodexRunOptionsDto`.
2. Include image attachment ids.
3. Include profile.
4. Include sandbox.
5. Include approval policy.
6. Include search enabled.
7. Include add-dir list.
8. Include config overrides as structured key/value list.
9. Include output schema attachment id only after attachment contract exists.
10. Validate wrong-source options elsewhere.

Stop conditions:

- Stop if contract was not updated.

Verification:

```bash
mvn test -Dtest=StartRunRequestTest
rg "CodexRunOptionsDto|profile|sandbox|approval|search|addDir|config" src/main/java src/test/java
```

### MOBILE-CODEX-M003 Validate Codex Options

Purpose:

Reject unsafe or unsupported Codex options before command building.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`
- `src/test/java/com/lqtigee/sparkai/service/RunServiceTest.java`

Implementation:

1. Reject `codexOptions` when source is not `CODEX`.
2. Reject `opencodeOptions` when source is `CODEX`.
3. Validate sandbox values against Codex help.
4. Validate approval policy values against Codex help.
5. Require confirmation for dangerous bypass flags.
6. Reject direct filesystem paths from frontend.

Stop conditions:

- Stop if frontend-supplied paths would be trusted directly.

Verification:

```bash
mvn test -Dtest=RunServiceTest
rg "codexOptions|approval|sandbox|DANGER_CONFIRM_REQUIRED|wrong source" src/main/java/com/lqtigee/sparkai/service src/test/java/com/lqtigee/sparkai/service
```

### MOBILE-CODEX-M004 Map Codex Safe Runtime Options

Purpose:

Map safe Codex options to `codex exec resume` argument arrays.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java`

Implementation:

1. Map model to `-m`.
2. Map profile to `-p` only if supported by selected command path.
3. Map sandbox to `-s` only when using a command path that supports it.
4. Map `--json`.
5. Preserve selected session id.
6. Preserve prompt as one argument.
7. Do not use `sh -c`.

Stop conditions:

- Stop if local help shows selected command path does not support an option.

Verification:

```bash
mvn test -Dtest=CodexCommandBuilderTest
rg "exec|resume|--json|-m|sessionId|prompt" src/main/java/com/lqtigee/sparkai/runtime src/test/java/com/lqtigee/sparkai/runtime
```

### MOBILE-CODEX-M005 Map Codex Image Attachments

Purpose:

Support Codex `--image` from uploaded phone attachments.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java`
- `src/main/java/com/lqtigee/sparkai/service/AttachmentService.java`
- `src/main/java/com/lqtigee/sparkai/runtime/RunRuntimeConfig.java`
- `src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java`

Implementation:

1. Resolve attachment ids server-side.
2. Require image content types for Codex `--image`.
3. Add one `--image` argument per resolved safe image path.
4. Reject missing attachment ids.
5. Do not accept raw paths from frontend.

Stop conditions:

- Stop if attachment service is not implemented.

Verification:

```bash
mvn test -Dtest=CodexCommandBuilderTest
rg "--image|attachmentIds|AttachmentService" src/main/java src/test/java
```

### MOBILE-CAP-CODEX-M001 Enable Codex Image Attachment Capability

Purpose:

Expose Codex image attachment support only after `MOBILE-CODEX-M005` maps uploaded attachment ids to real `--image` arguments.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`
- `src/main/java/com/lqtigee/sparkai/service/CapabilityService.java`
- `src/test/java/com/lqtigee/sparkai/service/CapabilityServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/CapabilityControllerTest.java`

Implementation:

1. Update source capability contract so `CODEX attachments` includes `image`.
2. Update response fixture so Codex capability returns `attachments: ["image"]`.
3. Update `CapabilityService` to expose only the implemented Codex image attachment capability.
4. Update capability tests.
5. Do not expose Codex `addDir`, output schema, or opencode file attachments.

Stop conditions:

- Stop if `MOBILE-CODEX-M005` has not been implemented.
- Stop if capability would expose unimplemented attachment types.

Verification:

```bash
mvn test -Dtest=CapabilityServiceTest,CapabilityControllerTest
rg "attachments.*image|CODEX|SourceCapabilityDto" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md src/main/java src/test/java
```

### MOBILE-CODEX-FE-IMAGE-M001 Wire Codex Image Attachment UI

Purpose:

Make the currently enabled Codex image attachment capability usable from the phone chat composer without exposing unsupported Codex controls.

Allowed files:

- `frontend/src/components/CodexOptionsSheet.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/AttachmentPicker.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Show the Codex image attachment affordance only when `capability.attachments` includes `image`.
2. Limit the Codex attachment picker to image file input accept values.
3. Build `codexOptions.imageAttachmentIds` only from uploaded attachment ids.
4. Do not expose Codex profile, sandbox, approval, search, add-dir, config, or output schema controls in this ticket.
5. Do not accept raw frontend file paths.
6. Do not upload or attach files if the backend capability is absent.

Stop conditions:

- Stop if CODEX `attachments` does not include `image` at runtime.
- Stop if the implementation would require enabling unsupported Codex run options.

Verification:

```bash
cd frontend && npm run build
rg "accept|image/|imageAttachmentIds|CodexOptionsSheet|attachments.includes\\(\"image\"\\)" frontend/src/components
! rg "profile|sandbox|approval|search|addDir|outputSchema" frontend/src/components/CodexOptionsSheet.tsx frontend/src/components/SessionChatComposer.tsx
```

### BUG-CODEX-PERMISSION-M001 Restore Safe Sandbox Args

Symptom:

`CodexCommandBuilder` currently validates `ASK`, `REVIEW`, and `EDIT` modes but does not add the sandbox arguments required by `doc/security/command-permission-matrix.md`.

Expected:

- `ASK` uses `-s read-only`.
- `REVIEW` uses `-s read-only`.
- `EDIT` uses `-s workspace-write`.
- `SHELL` remains rejected until a separately verified dangerous Codex path is enabled.

Actual:

Safe Codex commands omit `-s`, and tests assert that omission.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java`

Failing verification:

```bash
mvn test -Dtest=CodexCommandBuilderTest
rg "doesNotContain\\(\"-s\"|buildOmitsUnsupportedSandbox" src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java
```

Implementation:

1. Replace permission validation with safe permission argument mapping.
2. Add `-s read-only` before `exec` for `ASK` and `REVIEW`.
3. Add `-s workspace-write` before `exec` for `EDIT`.
4. Keep `SHELL` rejected with `DANGER_CONFIRM_REQUIRED`.
5. Do not add `--dangerously-bypass-approvals-and-sandbox` in this ticket.
6. Keep command as an argument array and preserve prompt as one argument.

Verification:

```bash
mvn test -Dtest=CodexCommandBuilderTest
rg "buildUsesReadOnlySandboxForAsk|buildUsesReadOnlySandboxForReview|buildUsesWorkspaceWriteSandboxForEdit|-s|workspace-write|read-only" src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java
! rg "doesNotContain\\(\"-s\"|buildOmitsUnsupportedSandbox|dangerously-bypass-approvals-and-sandbox" src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java
```

### MOBILE-OPENCODE-M001 Record opencode CLI Option Evidence

Purpose:

Record current local opencode help evidence before adding UI controls.

Allowed files:

- `doc/discovery/opencode-chat-controls.md`

Implementation:

1. Record command used: `opencode run --help`.
2. Record command used: `opencode session --help`.
3. Record command used: `opencode agent --help`.
4. Summarize option names only.
5. Do not record secrets.

Stop conditions:

- Stop if opencode command is unavailable.

Verification:

```bash
test -f doc/discovery/opencode-chat-controls.md
rg "opencode run|--session|--fork|--file|--agent|--variant|--thinking|--replay-limit|session delete|agent list" doc/discovery/opencode-chat-controls.md
```

### MOBILE-OPENCODE-M002 Add opencode Options DTO

Purpose:

Create typed backend request fields for opencode-only options.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `src/main/java/com/lqtigee/sparkai/dto/StartRunRequest.java`
- `src/main/java/com/lqtigee/sparkai/dto/OpencodeRunOptionsDto.java`
- `src/test/java/com/lqtigee/sparkai/dto/StartRunRequestTest.java`

Implementation:

1. Add `OpencodeRunOptionsDto`.
2. Include agent id/name.
3. Include fork boolean.
4. Include share boolean.
5. Include variant string.
6. Include thinking boolean.
7. Include replay boolean.
8. Include replay limit number.
9. Include file attachment ids.
10. Include title only for new-session future tasks, not selected-session continue.

Stop conditions:

- Stop if selected-session continue requires a title.

Verification:

```bash
mvn test -Dtest=StartRunRequestTest
rg "OpencodeRunOptionsDto|agent|fork|share|variant|thinking|replayLimit|attachmentIds" src/main/java src/test/java
```

### MOBILE-OPENCODE-M003 Validate opencode Options

Purpose:

Reject unsafe or unsupported opencode options before command building.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/dto/OpencodeRunOptionsDto.java`
- `src/main/java/com/lqtigee/sparkai/service/RunService.java`
- `src/test/java/com/lqtigee/sparkai/dto/StartRunRequestTest.java`
- `src/test/java/com/lqtigee/sparkai/service/RunServiceTest.java`

Implementation:

1. Reject `opencodeOptions` when source is not `OPENCODE`.
2. Reject `codexOptions` when source is `OPENCODE`.
3. Require confirmation for `dangerously-skip-permissions`.
4. Validate replay limit bounds.
5. Validate agent exists only after real agent list endpoint exists.
6. Reject direct frontend file paths.

Stop conditions:

- Stop if agent validation would use fake agent list.

Verification:

```bash
mvn test -Dtest=RunServiceTest
rg "opencodeOptions|replayLimit|dangerously|wrong source|agent" src/main/java/com/lqtigee/sparkai/service src/test/java/com/lqtigee/sparkai/service
```

### MOBILE-OPENCODE-CONTRACT-M001 Add opencode Agent List Contract

Purpose:

Define the real opencode agent list API contract before implementing `/api/opencode/agents`.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`

Implementation:

1. Add `GET /api/opencode/agents`.
2. State bearer token is required.
3. Add `OpencodeAgentDto`.
4. Include `id`, `name`, and `source` fields.
5. State `source` must be the real opencode source name from CLI/config evidence, not a fake default.
6. Add success response fixture with at least two real-style agents.
7. Add failure list entry for typed errors when agents cannot be listed.
8. State no fake agents and no fallback empty success array.

Stop conditions:

- Stop if the response shape cannot be defined without guessing opencode semantics.

Verification:

```bash
rg "GET /api/opencode/agents|OpencodeAgentDto|opencodeAgents|agent list|no fake agents|fallback empty success" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
```

### MOBILE-OPENCODE-ERROR-M001 Add opencode Agent Error Codes

Purpose:

Add stable typed error codes required by the opencode agent list contract.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/error/ErrorCode.java`
- `src/test/java/com/lqtigee/sparkai/error/ErrorCodeTest.java`

Implementation:

1. Add `OPENCODE_AGENT_LIST_FAILED`.
2. Add `OPENCODE_AGENT_OUTPUT_INVALID`.
3. Add `OPENCODE_AGENT_SOURCE_UNAVAILABLE`.
4. Do not remove or rename existing error codes.

Stop conditions:

- Stop if any existing error code must be renamed.

Verification:

```bash
mvn test -Dtest=ErrorCodeTest
rg "OPENCODE_AGENT_LIST_FAILED|OPENCODE_AGENT_OUTPUT_INVALID|OPENCODE_AGENT_SOURCE_UNAVAILABLE" src/main/java/com/lqtigee/sparkai/error src/test/java/com/lqtigee/sparkai/error
```

### MOBILE-OPENCODE-M004 Add Real opencode Agent List Endpoint

Purpose:

Expose real opencode agents for the phone options sheet.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeAgentService.java`
- `src/main/java/com/lqtigee/sparkai/web/OpencodeController.java`
- `src/main/java/com/lqtigee/sparkai/dto/OpencodeAgentDto.java`
- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeAgentServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/OpencodeControllerTest.java`

Implementation:

1. Use real opencode source/config or CLI evidence.
2. Return typed error if agents cannot be listed.
3. Do not return fake default agents.
4. Protect endpoint with bearer token.

Stop conditions:

- Stop if no real agent source is discovered.

Verification:

```bash
mvn test -Dtest=OpencodeAgentServiceTest,OpencodeControllerTest
rg "OpencodeAgentDto|/api/opencode/agents|agent list" src/main/java src/test/java
```

### MOBILE-OPENCODE-M005 Map opencode Runtime Options

Purpose:

Map opencode options to `opencode run --format json --session <id>`.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilderTest.java`

Implementation:

1. Keep `--format json`.
2. Keep `--session`.
3. Map model to `--model`.
4. Map workspace to `--dir`.
5. Map agent to `--agent`.
6. Map fork to `--fork`.
7. Map share to `--share`.
8. Map variant to `--variant`.
9. Map thinking to `--thinking`.
10. Map replay false as `--no-replay` if local help supports it.
11. Map replay limit to `--replay-limit`.
12. Preserve prompt as argument array item.
13. Do not use `sh -c`.

Stop conditions:

- Stop if a flag is not confirmed by local help.

Verification:

```bash
mvn test -Dtest=OpencodeCommandBuilderTest
rg "--format|--session|--model|--dir|--agent|--fork|--variant|--replay-limit" src/main/java/com/lqtigee/sparkai/runtime src/test/java/com/lqtigee/sparkai/runtime
```

### MOBILE-OPENCODE-M006 Map opencode File Attachments

Purpose:

Support opencode `--file` from uploaded phone attachments.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilder.java`
- `src/main/java/com/lqtigee/sparkai/service/AttachmentService.java`
- `src/main/java/com/lqtigee/sparkai/runtime/RunRuntimeConfig.java`
- `src/test/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilderTest.java`

Implementation:

1. Resolve attachment ids server-side.
2. Add one `--file` per resolved safe attachment path.
3. Reject missing attachment ids.
4. Do not accept raw frontend paths.

Stop conditions:

- Stop if attachment service is not implemented.

Verification:

```bash
mvn test -Dtest=OpencodeCommandBuilderTest
rg "--file|attachmentIds|AttachmentService" src/main/java src/test/java
```

### MOBILE-CAP-OPENCODE-M001 Enable opencode File Attachment Capability

Purpose:

Expose opencode file attachment support only after `MOBILE-OPENCODE-M006` maps uploaded attachment ids to real `--file` arguments.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`
- `src/main/java/com/lqtigee/sparkai/service/CapabilityService.java`
- `src/test/java/com/lqtigee/sparkai/service/CapabilityServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/CapabilityControllerTest.java`

Implementation:

1. Update source capability contract so `OPENCODE attachments` includes `file`.
2. Update response fixture so opencode capability returns `attachments: ["file"]`.
3. Update `CapabilityService` to expose only the implemented opencode file attachment capability.
4. Update capability tests.
5. Do not expose unimplemented attachment types.

Stop conditions:

- Stop if `MOBILE-OPENCODE-M006` has not been implemented.
- Stop if capability would expose unimplemented attachment types.

Verification:

```bash
mvn test -Dtest=CapabilityServiceTest,CapabilityControllerTest
rg "attachments.*file|OPENCODE|SourceCapabilityDto" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md src/main/java src/test/java
```

### MOBILE-ATTACH-M001 Add Attachment Storage Properties

Purpose:

Configure where uploaded phone files are stored before CLI execution.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/config/RemoteProperties.java`
- `src/main/resources/application.yml`
- `src/test/java/com/lqtigee/sparkai/config/RemotePropertiesTest.java`

Implementation:

1. Add attachment root property.
2. Add max file size property.
3. Add allowed content type config.
4. Validate root is configured and inside allowed service-owned path.

Stop conditions:

- Stop if attachment root would be arbitrary user input.

Verification:

```bash
mvn test -Dtest=RemotePropertiesTest
rg "attachment|upload|max" src/main/java/com/lqtigee/sparkai/config src/main/resources/application.yml src/test/java
```

### MOBILE-CONTRACT-M003 Add Attachment Contract

Purpose:

Define how the phone uploads files/images for Codex `--image` and opencode `--file`.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`
- `doc/security/command-permission-matrix.md`

Implementation:

1. Add `POST /api/attachments`.
2. Add `DELETE /api/attachments/{id}`.
3. Add `AttachmentDto` with `id`, `filename`, `contentType`, `sizeBytes`, `createdAt`.
4. Add `attachmentIds` to `StartRunRequest`.
5. State attachment files are stored only under a configured Lqtigee temp directory.
6. State attachment ids must be resolved server-side to safe paths.
7. State Codex image attachments may map to `--image`.
8. State opencode file attachments may map to `--file`.

Stop conditions:

- Stop if file path is accepted from the frontend.
- Stop if uploaded files can escape the configured attachment directory.

Verification:

```bash
rg "AttachmentDto|/api/attachments|attachmentIds|--image|--file|configured Lqtigee temp" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md doc/security/command-permission-matrix.md
```

### MOBILE-ATTACH-M002 Add Attachment Upload Endpoint

Purpose:

Let the phone upload images/files securely.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/AttachmentService.java`
- `src/main/java/com/lqtigee/sparkai/web/AttachmentController.java`
- `src/main/java/com/lqtigee/sparkai/dto/AttachmentDto.java`
- `src/test/java/com/lqtigee/sparkai/service/AttachmentServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/AttachmentControllerTest.java`

Implementation:

1. Accept multipart upload.
2. Require bearer token.
3. Store file under configured attachment root.
4. Generate server-owned attachment id.
5. Return `AttachmentDto`.
6. Reject oversized files.
7. Reject forbidden content types.
8. Do not expose raw filesystem path.

Stop conditions:

- Stop if upload can overwrite existing files.

Verification:

```bash
mvn test -Dtest=AttachmentServiceTest,AttachmentControllerTest
rg "/api/attachments|MultipartFile|AttachmentDto|sizeBytes|contentType" src/main/java src/test/java
```

### MOBILE-ATTACH-M003 Add Attachment Delete Endpoint

Purpose:

Allow removing uploaded files before sending a prompt.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/AttachmentService.java`
- `src/main/java/com/lqtigee/sparkai/web/AttachmentController.java`
- `src/test/java/com/lqtigee/sparkai/service/AttachmentServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/AttachmentControllerTest.java`

Implementation:

1. Add `DELETE /api/attachments/{id}`.
2. Require bearer token.
3. Delete only files owned by attachment service.
4. Return typed success or typed not found error.

Stop conditions:

- Stop if path deletion can escape attachment root.

Verification:

```bash
mvn test -Dtest=AttachmentServiceTest,AttachmentControllerTest
rg "DELETE|attachments|PathGuard|attachment root" src/main/java src/test/java
```

### MOBILE-ATTACH-M004 Add Frontend Attachment Picker

Purpose:

Let phone users attach images/files to the next prompt.

Allowed files:

- `frontend/src/api/remoteApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/state/useAttachmentsState.ts`
- `frontend/src/components/AttachmentPicker.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Add attachment API types.
2. Add upload function.
3. Add delete function.
4. Add state hook.
5. Add file input UI.
6. Show uploaded attachment chips.
7. Pass attachment ids to `StartRunRequest`.
8. Do not upload until user selects real local phone file.

Stop conditions:

- Stop if backend attachment endpoint is missing.

Verification:

```bash
cd frontend && npm run build
rg "AttachmentPicker|useAttachmentsState|uploadAttachment|deleteAttachment|attachmentIds" frontend/src
```

### MOBILE-ATTACH-ERROR-M001 Add Attachment Error Codes

Purpose:

Add stable typed error codes required by the attachment contract.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/error/ErrorCode.java`
- `src/test/java/com/lqtigee/sparkai/error/ErrorCodeTest.java`

Implementation:

1. Add `ATTACHMENT_MISSING`.
2. Add `ATTACHMENT_TOO_LARGE`.
3. Add `ATTACHMENT_CONTENT_TYPE_FORBIDDEN`.
4. Add `ATTACHMENT_STORAGE_FAILED`.
5. Add `ATTACHMENT_NOT_FOUND`.
6. Add `ATTACHMENT_DELETE_FAILED`.
7. Add `ATTACHMENT_PATH_INVALID`.
8. Do not remove or rename existing error codes.

Stop conditions:

- Stop if any existing error code must be renamed.

Verification:

```bash
mvn test -Dtest=ErrorCodeTest
rg "ATTACHMENT_MISSING|ATTACHMENT_TOO_LARGE|ATTACHMENT_CONTENT_TYPE_FORBIDDEN|ATTACHMENT_STORAGE_FAILED|ATTACHMENT_NOT_FOUND|ATTACHMENT_DELETE_FAILED|ATTACHMENT_PATH_INVALID" src/main/java/com/lqtigee/sparkai/error src/test/java/com/lqtigee/sparkai/error
```

### MOBILE-CAP-M001 Backend Capabilities Service

Purpose:

Provide real enabled UI controls based on implemented backend support.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/CapabilityService.java`
- `src/main/java/com/lqtigee/sparkai/web/CapabilityController.java`
- `src/main/java/com/lqtigee/sparkai/dto/SourceCapabilityDto.java`
- `src/test/java/com/lqtigee/sparkai/service/CapabilityServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/CapabilityControllerTest.java`

Implementation:

1. Return `CODEX` capabilities only for implemented Codex options.
2. Return `OPENCODE` capabilities only for implemented opencode options.
3. Do not claim unimplemented controls.
4. Require bearer token.

Stop conditions:

- Stop if capability is inferred from frontend hardcoding.

Verification:

```bash
mvn test -Dtest=CapabilityServiceTest,CapabilityControllerTest
rg "/api/capabilities|SourceCapabilityDto|CODEX|OPENCODE" src/main/java src/test/java
```

### MOBILE-CAP-M002 Frontend Capabilities State

Purpose:

Load capability data and hide unavailable controls.

Allowed files:

- `frontend/src/types/api.ts`
- `frontend/src/api/remoteApi.ts`
- `frontend/src/state/useCapabilitiesState.ts`
- `frontend/src/components/SessionChatComposer.tsx`

Implementation:

1. Add capability types.
2. Add `getCapabilities()`.
3. Add state hook.
4. Load only when token exists.
5. Hide controls not enabled by backend.
6. Do not hardcode controls as enabled.

Stop conditions:

- Stop if backend endpoint is missing.

Verification:

```bash
cd frontend && npm run build
rg "useCapabilitiesState|getCapabilities|SourceCapabilityDto|capabilities" frontend/src
```

### MOBILE-CAP-SESSION-ACTIONS-M001 Enable Verified Session Action Capabilities

Purpose:

Expose source-specific session action ids only after their command builders and tests exist, so the phone session action menu can display real available actions without hardcoding them.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/CapabilityService.java`
- `src/test/java/com/lqtigee/sparkai/service/CapabilityServiceTest.java`

Implementation:

1. Add CODEX session action ids that already have command builder tests: `archive`, `delete`, `unarchive`, `fork`.
2. Add OPENCODE session action ids that already have command builder tests: `delete`, `export`.
3. Keep run options, attachment capabilities, and dangerous options unchanged.
4. Do not add session action execution endpoints in this ticket.
5. Do not add UI hardcoding or fake capability data.
6. Do not expose action ids without existing command builder tests.

Stop conditions:

- Stop if any action id lacks a passing source-specific command builder test.
- Stop if enabling an action would require changing API response shape.

Verification:

```bash
mvn test -Dtest=CapabilityServiceTest,CodexSessionActionCommandBuilderTest,OpencodeSessionActionCommandBuilderTest
rg "archive|delete|unarchive|fork|export|sessionActions" src/main/java/com/lqtigee/sparkai/service src/test/java/com/lqtigee/sparkai/service src/test/java/com/lqtigee/sparkai/runtime
```

### MOBILE-MGMT-ACTION-ENDPOINT-M001 Start Verified Session Action Process

Purpose:

Add the smallest source-scoped backend endpoint for starting a verified session action process, using existing command builders and capability gating, without returning process output or transcript text.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/dto/SessionActionRequest.java`
- `src/main/java/com/lqtigee/sparkai/dto/SessionActionResponse.java`
- `src/main/java/com/lqtigee/sparkai/service/SessionActionService.java`
- `src/main/java/com/lqtigee/sparkai/web/SessionActionController.java`
- `src/main/java/com/lqtigee/sparkai/runtime/RunRuntimeConfig.java`
- `src/test/java/com/lqtigee/sparkai/service/SessionActionServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/SessionActionControllerTest.java`

Implementation:

1. Add `SessionActionRequest` with `action` and `confirmDestructive`.
2. Add `SessionActionResponse` with `actionId`, `source`, `sessionId`, `action`, `status`, and `startedAt`.
3. Add `SessionActionService.startAction(source, sessionId, request)`.
4. Validate request and reject blank action.
5. Verify the selected real session exists through `SessionService.getRequiredSession`.
6. Verify the requested action is present in `CapabilityService` for the URL source.
7. Build command specs using `CodexSessionActionCommandBuilder` or `OpencodeSessionActionCommandBuilder`.
8. Launch command with `ProcessLauncher` as an argument array.
9. Return `STARTED` without transcript text, exported content, process id, raw filesystem path, token, or prompt.
10. Add `POST /api/sessions/{source}/{id}/actions`.
11. Do not add frontend click handlers in this ticket.
12. Do not stream or store export output in this ticket.
13. Do not use shell strings or fake action success.

Stop conditions:

- Stop if the action is not present in runtime capabilities.
- Stop if selected-session existence cannot be checked without inventing data.
- Stop if export output must be returned to satisfy the implementation.

Verification:

```bash
mvn test -Dtest=SessionActionServiceTest,SessionActionControllerTest
rg "SessionActionRequest|SessionActionResponse|SessionActionService|/api/sessions/\\{source\\}/\\{id\\}/actions|STARTED|confirmDestructive" src/main/java src/test/java
```

### MOBILE-FE-ACTION-API-M001 Add Session Action API Client

Purpose:

Expose the real source-scoped session action endpoint to the frontend without wiring any UI click behavior yet.

Allowed files:

- `frontend/src/types/api.ts`
- `frontend/src/api/remoteApi.ts`

Implementation:

1. Add `SessionActionRequest` frontend type with `action` and `confirmDestructive`.
2. Add `SessionActionResponse` frontend type with `actionId`, `source`, `sessionId`, `action`, `status`, and `startedAt`.
3. Add `startSessionAction(source, id, request)` to `remoteApi.ts`.
4. Build the URL as `/api/sessions/{source}/{id}/actions` with `encodeURIComponent` for both path variables.
5. Send JSON with `Content-Type: application/json`.
6. Do not add menu click handlers in this ticket.
7. Do not add fake action success or local-only state updates.

Stop conditions:

- Stop if the backend action endpoint does not exist.
- Stop if the frontend response shape would differ from `doc/contracts/backend-api-contract.md`.

Verification:

```bash
cd frontend && npm run build
rg "SessionActionRequest|SessionActionResponse|startSessionAction|/api/sessions/\\$\\{encodeURIComponent\\(source\\)\\}/\\$\\{encodeURIComponent\\(id\\)\\}/actions" frontend/src
```

### MOBILE-FE-ACTION-UI-M001 Wire Session Action Menu To Real API

Purpose:

Let the phone session action menu start a real backend session action after capability-gated selection and explicit destructive confirmation.

Allowed files:

- `frontend/src/components/SessionActionMenu.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Add an `onStartAction(action, confirmDestructive)` prop to `SessionActionMenu`.
2. Non-destructive actions call `onStartAction(action, false)` once.
3. Destructive actions first show confirmation UI and only call `onStartAction(action, true)` on the second explicit confirm.
4. Disable action buttons while a session action request is in flight.
5. Show the started action status returned by the backend.
6. Show typed API errors without treating them as success.
7. Refresh the real session list after a successful action.
8. Do not add hardcoded actions outside backend capabilities.
9. Do not invent export output or transcript changes.

Stop conditions:

- Stop if `startSessionAction` from `MOBILE-FE-ACTION-API-M001` is missing.
- Stop if refreshing sessions would require fake session data.

Verification:

```bash
cd frontend && npm run build
rg "onStartAction|startSessionAction|confirmDestructive|actionInFlight|SessionActionMenu" frontend/src
```

### BUG-CONTRACT-CAP-ACTIONS-M001 Align Session Action Capability Contract

Symptom:

`CapabilityService` now exposes verified session action ids and the frontend action menu consumes them, but `doc/contracts/backend-api-contract.md` and `doc/contracts/backend-response-fixtures.md` still show empty `sessionActions` arrays.

Expected:

Capability contract and fixture list the currently implemented runtime session action ids:

- `CODEX`: `archive`, `delete`, `unarchive`, `fork`
- `OPENCODE`: `delete`, `export`

Actual:

Contract examples still say `sessionActions: []` and the text still says no session action capabilities are enabled.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`

Failing verification:

```bash
rg "CODEX sessionActions: none|OPENCODE sessionActions: none|\"sessionActions\": \\[\\]" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
```

Implementation:

1. Update capability example JSON to include the real enabled session actions.
2. Update currently enabled capability text to list the same action ids.
3. Keep the rule that only backend-validated, tested action ids may be exposed.
4. Do not change backend code or frontend code in this ticket.

Verification:

```bash
rg "CODEX sessionActions: archive, delete, unarchive, fork|OPENCODE sessionActions: delete, export|\"sessionActions\": \\[\"archive\", \"delete\", \"unarchive\", \"fork\"\\]|\"sessionActions\": \\[\"delete\", \"export\"\\]" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
! rg "CODEX sessionActions: none|OPENCODE sessionActions: none|sessionActions\\\": \\[\\]" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
```

### MOBILE-PLAN-PG-M001 Split Run Record Lifecycle Tickets

Purpose:

Prevent PG run lifecycle implementation from becoming a broad, untestable change by splitting repository SQL, Spring wiring, service transitions, asynchronous terminal transitions, and schema alignment into separate micro tickets.

Allowed files:

- `doc/mobile-chat-console-microtasks.md`
- `doc/micro-tickets.md`

Implementation:

1. Replace the broad `MOBILE-PG-RUN-M002` task with a repository-only task.
2. Add a Spring wiring task for `DatabaseProperties`, `PostgresConnectionFactory`, and `RunRecordRepository`.
3. Add a `RunService` start/stop persistence task.
4. Add a `ProcessOutputPump` terminal persistence task.
5. Add a schema alignment task for migration, test schema, and PostgreSQL init schema.
6. Each new task must list exact allowed files, stop conditions, and verification commands.
7. Do not change Java, frontend, schema, or runtime behavior in this planning ticket.

Stop conditions:

- Stop if any resulting task still requires files outside its own Allowed files.

Verification:

```bash
rg "MOBILE-PG-RUN-M002|MOBILE-PG-RUN-M003|MOBILE-PG-RUN-M004|MOBILE-PG-RUN-M005|MOBILE-PG-RUN-M006|RunRuntimeConfig|ProcessOutputPump|001_init.sql" doc/mobile-chat-console-microtasks.md doc/micro-tickets.md
```

### MOBILE-I18N-M001 Localize Frontend Visible Text To Chinese

Purpose:

Make the phone web UI use Chinese for static visible labels, buttons, empty states, loading labels, validation messages, and common error panel labels.

Allowed files:

- `frontend/src/components/AppShell.tsx`
- `frontend/src/components/BottomNav.tsx`
- `frontend/src/components/ErrorPanel.tsx`
- `frontend/src/components/ModelSelect.tsx`
- `frontend/src/components/PromptComposer.tsx`
- `frontend/src/components/RunTimeline.tsx`
- `frontend/src/components/SessionCard.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/components/SideNav.tsx`
- `frontend/src/components/StatusBadge.tsx`
- `frontend/src/pages/ControlPage.tsx`
- `frontend/src/pages/OverviewPage.tsx`
- `frontend/src/pages/RunsPage.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/pages/SettingsPage.tsx`

Implementation:

1. Translate navigation labels to Chinese.
2. Translate app header token state text to Chinese.
3. Translate session list labels, filters, empty states, loading labels, and error panel titles to Chinese.
4. Translate session detail labels, back button, load-older button, and chat run state labels to Chinese.
5. Translate chat composer labels, buttons, placeholders, command mode display labels, and danger confirmation text to Chinese.
6. Translate shared model selector labels and empty option text to Chinese.
7. Translate shared prompt composer labels and danger confirmation text to Chinese.
8. Translate status badge labels for known statuses to Chinese without changing status enum values.
9. Translate overview, control, runs, and settings page visible text to Chinese.
10. Translate common error panel field labels and fallback text to Chinese.
11. Do not translate protocol values, enum values, API payload fields, storage keys, CSS class names, route paths, or brand names.
12. Do not add mock data, fake sessions, fallback sessions, or fake model labels.

Stop conditions:

- Stop if translating a string would change an API contract value or route path.

Verification:

```bash
cd frontend && npm run build
rg "会话|发送|停止|模型|设置|运行|刷新|保存|令牌|错误|加载|工作目录|更新时间" frontend/src/components frontend/src/pages
rg "Sessions|Reload|Token required|Live sessions cannot load|No session selected|Load earlier messages|Continue this session|Models failed to load|No models available|Remote Console|Selected-session run|Loading control data|No run selected|Refresh seconds" frontend/src/components frontend/src/pages
```

### BUG-RUN-STOP-RACE-M001 Preserve Stopped Run State Against Output Pump

Symptom:

Public inline SSE verification stopped a real Codex run and the SSE client received exactly one `stopped` terminal event, but the PostgreSQL `run_records` row later showed `EXITED`; the Java service also logged `RUN_ALREADY_FINISHED` from `ProcessOutputPump` after the stop path completed.

Expected:

After `POST /api/runs/{runId}/stop` succeeds, the in-memory run state and PostgreSQL run record remain `STOPPED`, and `ProcessOutputPump` does not publish or persist a second terminal state for the same run.

Actual:

`RunService.stop()` marks the run stopped and publishes `stopped`, then `ProcessOutputPump` observes the process exit and attempts a terminal transition; this can overwrite PostgreSQL status and can log `RUN_ALREADY_FINISHED`.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/RunRegistry.java`
- `src/main/java/com/lqtigee/sparkai/runtime/ProcessOutputPump.java`
- `src/main/java/com/lqtigee/sparkai/service/RunService.java`
- `src/test/java/com/lqtigee/sparkai/runtime/ProcessOutputPumpTest.java`
- `src/test/java/com/lqtigee/sparkai/service/RunServiceTest.java`

Failing verification:

`MOBILE-PUBLIC-M003` observed real runId `5e373f51-e81d-4f92-aeb7-4887000c7fbe`: SSE terminal type `stopped`, terminal count `1`, but PostgreSQL status later became `EXITED`.

Implementation:

1. Add an idempotent terminal-state guard to `RunRegistry` or expose a current-status check that lets the output pump detect already terminal runs without throwing.
2. Make `ProcessOutputPump` skip persistence and event publication when the run is already `STOPPED`, `EXITED`, or `FAILED`.
3. Preserve the existing rule that a normally finishing process still records exactly one terminal event.
4. Preserve the existing rule that a failed process still records exactly one terminal event.
5. Keep `RunService.stop()` responsible for the explicit `STOPPED` transition and `stopped` SSE event.
6. Do not change run API request/response shapes.
7. Do not add retry loops, mock events, fake runs, or fallback success states.

Verification:

```bash
mvn test -Dtest=RunServiceTest,ProcessOutputPumpTest
rg "isTerminal|already terminal|STOPPED|RUN_ALREADY_FINISHED|markExited|markStopped" src/main/java/com/lqtigee/sparkai/runtime src/main/java/com/lqtigee/sparkai/service src/test/java/com/lqtigee/sparkai/runtime src/test/java/com/lqtigee/sparkai/service
```

### BUG-OPENCODE-FE-DANGER-M001 Scope Opencode Permission Skip To Shell Mode

Symptom:

The opencode options drawer exposes a persistent `dangerouslySkipPermissions` toggle labeled `跳过权限确认`, and the chat composer sends that stored value for non-`SHELL` requests. This can make normal ASK/EDIT/REVIEW messages require dangerous confirmation even though the backend command builder only maps `--dangerously-skip-permissions` for `SHELL`.

Expected:

Only the chat composer `SHELL` mode confirmation checkbox may trigger dangerous opencode execution. ASK, EDIT, and REVIEW must never inherit a stored dangerous permission skip option.

Actual:

`OpencodeOptionsSheet` stores `dangerouslySkipPermissions`, `SessionChatComposer` reads it, sends it in `opencodeOptions`, and auto-sets `confirmDangerous` from local storage.

Allowed files:

- `frontend/src/components/OpencodeOptionsSheet.tsx`
- `frontend/src/components/SessionChatComposer.tsx`

Failing verification:

```bash
rg "跳过权限确认|shouldConfirmOpencodeDanger|dangerouslySkipPermissions" frontend/src/components/OpencodeOptionsSheet.tsx frontend/src/components/SessionChatComposer.tsx
```

Implementation:

1. Remove `dangerouslySkipPermissions` from the opencode options drawer stored options type.
2. Remove the `跳过权限确认` toggle from `OpencodeOptionsSheet`.
3. Remove stored dangerous option auto-confirm logic from `SessionChatComposer`.
4. Stop sending `opencodeOptions.dangerouslySkipPermissions` from the frontend composer.
5. Preserve the existing `SHELL` mode radio option and the visible `确认危险终端模式` checkbox.
6. Preserve attachment, agent, fork, share, thinking, replay, replay limit, and variant behavior.
7. Do not change backend DTOs, backend validation, backend command builder behavior, API contracts, or response shapes.
8. Do not add fallback behavior, fake capability values, mock sessions, or inferred CLI flags.

Verification:

```bash
cd frontend && npm run build
rg "确认危险终端模式|SHELL|confirmDangerous|shellDangerouslySkipPermissions" frontend/src/components/SessionChatComposer.tsx frontend/src/components/OpencodeOptionsSheet.tsx
! rg "跳过权限确认|shouldConfirmOpencodeDanger|dangerouslySkipPermissions" frontend/src/components/OpencodeOptionsSheet.tsx frontend/src/components/SessionChatComposer.tsx
```

### BUG-CONTRACT-HEALTH-FIXTURE-M001 Align Health Fixture With Health Contract

Symptom:

`doc/contracts/backend-api-contract.md` defines `GET /api/health` as `serviceName`, `appName`, `port`, `status`, `timestamp`, and `adapters`, with status values `OK`, `DEGRADED`, or `FAILED`. `doc/contracts/backend-response-fixtures.md` still documents an old `HealthResponse` shape with `status: "UP"`, `app`, and `version`.

Expected:

The test fixture for `HealthResponse` matches the API contract before backend implementation is changed.

Actual:

The fixture disagrees with the API contract and would let tests assert the wrong health response shape.

Allowed files:

- `doc/contracts/backend-response-fixtures.md`

Failing verification:

```bash
rg '"status": "UP"|app": "Lqtigee"|version": "dev"' doc/contracts/backend-response-fixtures.md
```

Implementation:

1. Replace the old health JSON fixture with the API-contract shape.
2. Include `serviceName`, `appName`, `port`, `status`, `timestamp`, and two `adapters`.
3. Use `OK` as the top-level healthy status in the fixture.
4. Add required assertions for the adapter array and `OK`, `DEGRADED`, `FAILED` status rule.
5. Do not change runtime code, tests, or the API contract in this ticket.
6. Do not add mock runtime data or fake health semantics.

Verification:

```bash
rg "serviceName|appName|adapters|OK|DEGRADED|FAILED|HealthResponse" doc/contracts/backend-response-fixtures.md
! rg '"status": "UP"|app": "Lqtigee"|version": "dev"' doc/contracts/backend-response-fixtures.md
```

### BUG-HEALTH-BE-M001 Return Contract Health With Adapter Probes

Symptom:

`GET /api/health` returns a hardcoded `status: "STARTING"` and omits `adapters`, even though `doc/contracts/backend-api-contract.md` and `doc/contracts/backend-response-fixtures.md` define `status` as `OK`, `DEGRADED`, or `FAILED` with real adapter probe results.

Expected:

`GET /api/health` returns the contract shape and computes top-level status from real `CodexAdapter.probe()` and `OpencodeAdapter.probe()` results.

Actual:

`HealthController` constructs a fixed response with no adapter probe calls.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/dto/HealthDto.java`
- `src/main/java/com/lqtigee/sparkai/web/HealthController.java`
- `src/test/java/com/lqtigee/sparkai/web/HealthControllerTest.java`

Failing verification:

```bash
curl -sS http://127.0.0.1:20261/api/health | rg '"status":"STARTING"'
```

Implementation:

1. Extend `HealthDto` with `List<AdapterHealthDto> adapters`.
2. Inject `CodexAdapter` and `OpencodeAdapter` into `HealthController`.
3. Call both adapters' real `probe()` methods for each health request.
4. Return top-level `OK` when both adapters are available.
5. Return top-level `DEGRADED` when exactly one adapter is available.
6. Return top-level `FAILED` when no adapter is available.
7. Preserve `serviceName`, `appName`, `port`, and `timestamp`.
8. Update `HealthControllerTest` to assert the contract shape and adapter status aggregation with Mockito beans.
9. Do not add mock runtime health data outside tests.
10. Do not hide adapter probe failures as empty success.
11. Do not require bearer auth for `/api/health`.

Verification:

```bash
mvn test -Dtest=HealthControllerTest
rg "adapters|DEGRADED|FAILED|probe|STARTING" src/main/java/com/lqtigee/sparkai/dto/HealthDto.java src/main/java/com/lqtigee/sparkai/web/HealthController.java src/test/java/com/lqtigee/sparkai/web/HealthControllerTest.java
! rg '"STARTING"|status\\(\"STARTING\"\\)|value\\(\"STARTING\"\\)' src/main/java/com/lqtigee/sparkai/web/HealthController.java src/test/java/com/lqtigee/sparkai/web/HealthControllerTest.java
```

### BUG-HEALTH-FE-ADAPTERS-M001 Show Adapter Health On Overview

Symptom:

The backend `/api/health` now returns real `adapters` for Codex and opencode, but the frontend `HealthDto` type in `remoteApi.ts` omits `adapters` and Overview only shows the top-level service status. The phone cannot see whether Codex or opencode is individually available from the first screen.

Expected:

Overview displays the real Codex and opencode adapter health returned by `/api/health`, including source, availability/status, and version or error code.

Actual:

The adapter array is not typed or rendered in the frontend.

Allowed files:

- `frontend/src/api/remoteApi.ts`
- `frontend/src/pages/OverviewPage.tsx`
- `frontend/src/components/StatusBadge.tsx`
- `frontend/src/styles/global.css`

Failing verification:

```bash
rg "adapters" frontend/src/api/remoteApi.ts frontend/src/pages/OverviewPage.tsx
```

Implementation:

1. Add a frontend `AdapterHealthDto` interface matching the backend health adapter shape.
2. Add `adapters: AdapterHealthDto[]` to the frontend `HealthDto`.
3. Render an adapter health section on Overview only when `health.adapters` is present.
4. Show each adapter `source`.
5. Show a Chinese availability label derived from the real `available` boolean and `status`.
6. Show adapter `version` when available.
7. Show adapter `lastErrorCode` when unavailable.
8. Add StatusBadge styling/labels for `OK`, `DEGRADED`, `UNAVAILABLE`, and adapter unavailable states if needed.
9. Do not synthesize adapter rows when backend omits them.
10. Do not add mock health data or fake adapter states.

Verification:

```bash
cd frontend && npm run build
rg "AdapterHealthDto|adapters|适配器|CODEX|OPENCODE|DEGRADED|UNAVAILABLE" frontend/src/api/remoteApi.ts frontend/src/pages/OverviewPage.tsx frontend/src/components/StatusBadge.tsx frontend/src/styles/global.css
```

### EVIDENCE-RUN-STOP-PG-M001 Reverify Public Stopped Run Persistence

Symptom:

`BUG-RUN-STOP-RACE-M001` changed `RunService.stop()` and `ProcessOutputPump` so stopped runs remain terminal `STOPPED`, but `doc/audit/chat-control-live-evidence.md` and `doc/audit/release-checklist-status.md` still record the old `MOBILE-PUBLIC-M003` follow-up risk where PostgreSQL later showed `EXITED`.

Expected:

A new real public stop-run verification proves whether the current code keeps the SSE terminal event and PostgreSQL `run_records.status` aligned as `STOPPED`.

Actual:

The audit documents still describe the pre-fix persistence race and can mislead later agents.

Allowed files:

- `doc/audit/chat-control-live-evidence.md`
- `doc/audit/release-checklist-status.md`

Failing verification:

```bash
rg "PostgreSQL.*EXITED|persistence race|RUN_ALREADY_FINISHED|sole stopped-run truth" doc/audit/chat-control-live-evidence.md doc/audit/release-checklist-status.md
```

Implementation:

1. Start a real run through the public `/api/runs` route using an existing real Codex session and valid model.
2. Subscribe to public `/api/runs/{runId}/events`.
3. Stop the run through public `/api/runs/{runId}/stop`.
4. Record only non-secret evidence: run id, terminal event type/count, stop response status, PostgreSQL status, and whether no prompt/transcript text was recorded.
5. Query PostgreSQL `run_records.status` for the new run id after the process exits.
6. If PostgreSQL status remains `STOPPED`, update the old follow-up risk to state it has been reverified fixed by the current code.
7. If PostgreSQL status is not `STOPPED`, record `FAIL` without changing code in this ticket.
8. Do not record API token, prompt text, transcript text, or stdout/stderr content.
9. Do not fabricate SSE events, terminal states, PostgreSQL rows, or cleanup results.

Verification:

```bash
rg "EVIDENCE-RUN-STOP-PG-M001|STOPPED|PostgreSQL status|terminal count|no prompt text|no transcript text" doc/audit/chat-control-live-evidence.md doc/audit/release-checklist-status.md
! rg "sole stopped-run truth until|persistence race before using PostgreSQL|RUN_ALREADY_FINISHED from ProcessOutputPump" doc/audit/chat-control-live-evidence.md doc/audit/release-checklist-status.md
```

### BUG-FE-STALE-RUN-GUARD-M001 Surface Active Run On Other Session

Symptom:

If a non-terminal chat run belongs to session A and the user switches to session B, session B renders an enabled composer because `chatRunNonTerminal` is only passed when the run belongs to the selected session. Pressing send on session B is rejected inside `useSessionChatRunState`, but `chatRunError` is hidden because it also only renders when the run belongs to the selected session.

Expected:

When any non-terminal chat run exists, every other selected session clearly shows that another session is running and disables its composer until that run reaches a terminal event.

Actual:

The composer for another session can appear sendable, and the rejection error can be hidden.

Allowed files:

- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/components/SessionDetail.tsx`

Failing verification:

```bash
rg "chatRunBelongsToSelectedSession \\? chatRunState.nonTerminal|chatRunBelongsToSelectedSession \\? chatRunState.error" frontend/src/pages/SessionsPage.tsx
```

Implementation:

1. Compute whether a non-terminal chat run exists for another session.
2. Pass that state from `SessionsPage` into `SessionDetail`.
3. In `SessionDetail`, display a Chinese notice that another session has an active run.
4. Disable the composer when another session has a non-terminal run.
5. Preserve existing behavior for the selected session that owns the run: show inline events, stop button, and terminal state.
6. Do not auto-stop the other session run.
7. Do not show stale run events in the newly selected session.
8. Do not invent backend run ownership endpoints, fake run events, or mock errors.

Verification:

```bash
cd frontend && npm run build
rg "chatRunOtherSessionNonTerminal|其他会话正在运行|composerDisabled|chatRunBelongsToSelectedSession" frontend/src/pages/SessionsPage.tsx frontend/src/components/SessionDetail.tsx
! rg "chatRunBelongsToSelectedSession \\? chatRunState.nonTerminal|chatRunBelongsToSelectedSession \\? chatRunState.error" frontend/src/pages/SessionsPage.tsx
```

### BUG-FE-COMPOSER-SESSION-SCOPE-M001 Reset Session-Scoped Composer State On Selection Change

Symptom:

`SessionChatComposer` is reused when the selected real Codex/opencode session changes. The prompt draft is already keyed by `source` and `sessionId`, but local composer state such as uploaded attachment references, selected command mode, and `confirmDangerous` can remain from the previously selected session.

Expected:

When the selected `source` or `sessionId` changes, the composer starts that selected session with no selected attachments, `ASK` mode, and `confirmDangerous=false`. Existing per-session draft loading remains unchanged.

Actual:

The composer can carry attachments or dangerous terminal confirmation from one real session into another selected real session.

Allowed files:

- `frontend/src/components/SessionChatComposer.tsx`

Failing verification:

```bash
rg "composerSessionKeyRef|setMode\\(\"ASK\"\\)|setConfirmDangerous\\(false\\)" frontend/src/components/SessionChatComposer.tsx
```

Implementation:

1. Add a `useRef` that stores the current composer session key as `${source}:${sessionId}`.
2. Add a `useEffect` that runs on `source` or `sessionId` changes.
3. Inside the effect, build the next session key.
4. If the next key equals the stored key, return without changing state.
5. If the next key differs, update the stored key.
6. Clear `attachmentsState` through `attachmentsState.clearAttachments()`.
7. Reset command mode with `setMode("ASK")`.
8. Reset dangerous confirmation with `setConfirmDangerous(false)`.
9. Do not clear the prompt draft in this effect because `useChatDraftState(source, sessionId)` owns per-session draft loading.
10. Do not delete attachment files from the backend in this ticket.
11. Do not change model loading, capabilities loading, run start, run stop, transcript loading, or session selection behavior.
12. Do not add mock sessions, fake attachment ids, fake events, or fallback success.

Verification:

```bash
cd frontend && npm run build
rg "composerSessionKeyRef|clearAttachments\\(\\)|setMode\\(\"ASK\"\\)|setConfirmDangerous\\(false\\)" frontend/src/components/SessionChatComposer.tsx
! rg "sample|fake|mock|demo" frontend/src/components/SessionChatComposer.tsx
```
