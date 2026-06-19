# AI Implementation Risk Assessment v2

Project: `Lqtigee-spark-ai`  
App name: `Lqtigee`  
Backend port: `20261`  
Assessment type: implementation feasibility, AI drift risk, unrecoverable loop risk, and required guardrails.

## 1. Assessment Summary

This project is feasible, but it is not safe for a general AI agent to implement as a broad "build the app" task.

The implementation has several places where the AI can accidentally create a self-reinforcing wrong system:

- unknown Codex session format
- unknown opencode session format
- unverified CLI command syntax
- frontend built before real API
- backend returning partial/fake success
- SSE lifecycle not closing correctly
- command execution appearing successful while stderr contains usage errors

The current documentation reduces the risk, but it is not yet enough by itself. The plan needs stronger risk gates and additional validation tickets before code work begins.

## 2. Final Risk Judgment

### 2.1 If implemented as one large AI task

Risk:

```text
Critical
```

Expected failure mode:

```text
The AI will likely invent session assumptions, add UI placeholder states, and then patch around integration failures.
```

This can enter a loop that is hard to correct because the wrong assumptions are spread across parser code, API DTOs, UI state, and command execution.

### 2.2 If implemented using current plan only

Risk:

```text
Medium-high
```

Reason:

- The plan is decomposed, but not every dependency gate is strict enough.
- Parser tickets exist before enough real sample evidence is guaranteed.
- Frontend tickets can still be started before backend endpoint behavior is proven.
- Runtime tickets can still start unless command syntax evidence is explicitly required.

### 2.3 If implemented using current plan plus the additional risk controls in this document

Risk:

```text
Medium-low
```

Reason:

- Every unknown runtime fact becomes a `BLOCKED` state.
- Frontend only follows real API.
- Parser work only follows sanitized real samples.
- Command builders only follow recorded CLI help, existing session evidence, and unit tests. Live CLI gates are forbidden.
- SSE terminal behavior is tested before UI success state.

### 2.4 When can risk be considered low?

Risk can be considered low only after these artifacts exist:

```text
PROJECT_SPEC.md
doc/discovery/port-20261.md
doc/discovery/codex-command.md
doc/discovery/codex-version.md
doc/discovery/codex-help.md
doc/discovery/codex-home.md
doc/discovery/opencode-command.md
doc/discovery/opencode-version.md
doc/discovery/opencode-help.md
doc/discovery/opencode-roots.md
doc/discovery/session-format-samples.md
doc/discovery/cli-command-syntax.md
doc/discovery/model-catalog.md
doc/contracts/backend-api-contract.md
doc/contracts/backend-response-fixtures.md
doc/security/command-permission-matrix.md
```

And these tests pass:

```text
ApiError contract test
Codex scanner test
Codex parser test using sanitized real sample
opencode scanner test
opencode parser test using sanitized real sample
Model config test
Command builder snapshot test
Process lifecycle test
SSE terminal event test
Frontend API error handling test
Mobile layout check
```

Until then, implementation risk remains non-trivial.

## 3. Risk Scoring Model

Each risk is scored from 1 to 5.

Severity:

```text
1 = cosmetic problem
2 = local bug
3 = feature broken but isolated
4 = cross-module corruption or unsafe behavior
5 = project-level dead loop or dangerous remote control behavior
```

Likelihood:

```text
1 = unlikely
2 = possible
3 = plausible
4 = likely
5 = very likely
```

Detectability:

```text
1 = obvious immediately
2 = caught by local test
3 = caught by integration test
4 = visible only in real runtime
5 = hidden until multiple modules depend on it
```

Risk score:

```text
severity * likelihood * detectability
```

Risk bands:

```text
1-15 = low
16-35 = medium
36-60 = high
61-125 = critical
```

## 4. Uncorrectable Loop Definition

An uncorrectable AI loop is not merely "the code has bugs." It is a state where each attempted fix reinforces a false assumption.

In this project, the loop usually looks like:

```text
Unknown real behavior
-> AI invents implementation detail
-> tests validate invented behavior
-> frontend/backend integrate around invented behavior
-> real runtime fails
-> AI patches the wrong layer
-> original false assumption remains
-> repeated corrections increase damage
```

The loop becomes hard to correct when:

- invented session fields are used in DTOs
- fake UI states are treated as expected UX
- command builder syntax is guessed
- process stderr is displayed as if run succeeded
- endpoint failures are converted into empty arrays
- tests use artificial fixtures unrelated to real samples

## 5. Top-Level Risk Register

| ID | Risk | Severity | Likelihood | Detectability | Score | Band |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| R-001 | AI guesses Codex session format | 5 | 4 | 5 | 100 | Critical |
| R-002 | AI guesses opencode session format | 5 | 4 | 5 | 100 | Critical |
| R-003 | UI is built with fake session/model data | 5 | 4 | 4 | 80 | Critical |
| R-004 | CLI command syntax is guessed | 5 | 3 | 4 | 60 | High |
| R-005 | Scanner failure returns empty success | 5 | 3 | 5 | 75 | Critical |
| R-006 | Partial adapter failure hidden in unified sessions | 4 | 3 | 4 | 48 | High |
| R-007 | SSE terminal event missing | 4 | 3 | 4 | 48 | High |
| R-008 | Process lifecycle state becomes inconsistent | 4 | 3 | 4 | 48 | High |
| R-009 | Token auth accidentally bypassed | 5 | 2 | 3 | 30 | Medium |
| R-010 | Service worker caches API failure or stale data | 3 | 3 | 4 | 36 | High |
| R-011 | Frontend/backend DTO drift | 4 | 3 | 4 | 48 | High |
| R-012 | Allowed workspace validation too loose | 5 | 2 | 4 | 40 | High |
| R-013 | AI broad-refactors after one test failure | 4 | 3 | 4 | 48 | High |
| R-014 | Error codes are inconsistent | 3 | 3 | 3 | 27 | Medium |
| R-015 | Android layout breaks at 360px | 2 | 3 | 2 | 12 | Low |

The critical risks are all about false success:

```text
guessed parser
fake UI data
empty success on failure
```

Those must be treated as release blockers and implementation blockers.

## 6. Phase-by-Phase Risk Audit

## 6.1 Phase P0: Discovery

Purpose:

- Record real machine facts before implementation.

Primary risk:

- AI treats discovery as optional and starts coding.

Failure chain:

```text
No discovery artifacts
-> parser tickets start
-> field names guessed
-> parser tests use invented fixtures
-> API looks complete
-> real sessions fail
```

Risk score:

```text
severity 5 * likelihood 4 * detectability 5 = 100 Critical
```

Required gate:

- No backend parser task may start before discovery files exist.

Required artifacts:

```text
doc/discovery/port-20261.md
doc/discovery/codex-command.md
doc/discovery/codex-version.md
doc/discovery/codex-help.md
doc/discovery/codex-home.md
doc/discovery/opencode-command.md
doc/discovery/opencode-version.md
doc/discovery/opencode-help.md
doc/discovery/opencode-roots.md
doc/discovery/session-format-samples.md
doc/discovery/cli-command-syntax.md
doc/discovery/model-catalog.md
```

Required detection:

- CI or manual checklist rejects parser tickets if discovery files are missing.

Required correction:

- Stop implementation.
- Run discovery tickets.
- Update parser requirements from real samples.

## 6.2 Phase B1: Backend Skeleton

Primary risk:

- AI adds business logic too early.

Failure chain:

```text
Skeleton task
-> AI adds scanner/controller shortcuts
-> controllers read files directly
-> later service abstractions conflict
```

Risk score:

```text
severity 3 * likelihood 3 * detectability 3 = 27 Medium
```

Required gate:

- Skeleton phase may only create application entrypoint, config, basic health, and error handling.

Detection signals:

- `Codex`, `opencode`, `ProcessBuilder`, or filesystem scanner appears in skeleton controller.

Correction:

- Revert the out-of-scope code.
- Re-run skeleton ticket with allowed files only.

## 6.3 Phase B2: Security

Primary risk:

- Auth is added inconsistently or bypassed for convenience.

Failure chain:

```text
Frontend wants quick API calls
-> AI makes endpoints public
-> later token added to some routes only
-> remote control endpoint exposed
```

Risk score:

```text
severity 5 * likelihood 2 * detectability 3 = 30 Medium
```

Required gate:

- Every `/api/**` route except `/api/health` must be protected before command execution exists.

Detection:

```bash
curl -i http://127.0.0.1:20261/api/models
curl -i http://127.0.0.1:20261/api/runs
```

Both must return JSON 401 without token.

Correction:

- Fix filter mapping before any further endpoint work.

## 6.4 Phase C3: Contract DTOs

Primary risk:

- DTOs include guessed fields from unknown session files.

Failure chain:

```text
AI defines rich DTO
-> parser forced to produce fields it cannot know
-> fallback fields invented
```

Risk score:

```text
severity 4 * likelihood 3 * detectability 4 = 48 High
```

Required gate:

- DTOs include only fields that are either required by UI or proven available.

Required correction:

- Keep raw source-specific data in `metadata` only after parser proves it.
- Do not promote guessed fields to top-level DTO.

## 6.5 Phase D4: Codex Discovery

Primary risk:

- Codex session format is inferred from memory or filename.

Failure chain:

```text
No real Codex sample
-> parser written from memory
-> returns session id/workspace from guessed fields
-> command builder receives bad workspace
-> runtime fails
-> AI patches runtime instead of parser
```

Risk score:

```text
severity 5 * likelihood 4 * detectability 5 = 100 Critical
```

Required gate:

- `CodexJsonParser` and `CodexJsonlParser` cannot be written until sanitized real sample structure is documented.

Required parser rule:

- Missing required field throws `CODEX_SESSION_FIELD_MISSING`.
- Unknown format throws `CODEX_SESSION_FORMAT_UNKNOWN`.
- No fallback to filename for required fields.

Detection:

- Parser test must include a sanitized real sample copied from discovery.
- Parser test must include missing-field failure case.

Correction:

- If parser fails on real sample, update format document first, then parser.

## 6.6 Phase D4: opencode Discovery

Primary risk:

- Before discovery, opencode state looked ambiguous. Discovery has now fixed v1 session source to SQLite database `/home/lqtiger/.local/share/opencode/opencode.db`, table `session`. Any later implementation that uses JSONL, logs, or prompt history as the first session source is wrong.

Failure chain:

```text
Scanner finds files
-> AI parses convenient config file as session
-> UI displays config as session
-> command resumes wrong context or starts unrelated run
```

Risk score:

```text
severity 5 * likelihood 4 * detectability 5 = 100 Critical
```

Required gate:

- Must distinguish config files from session files.
- SQLite must not be parsed casually. The discovery phase found that opencode v1.17.8 stores real sessions in `/home/lqtiger/.local/share/opencode/opencode.db`, so opencode session implementation requires dedicated SQLite reader tickets.

Required detection:

- Discovery document lists candidate file path, file type, and why it is a session file.

Correction:

- If no session file can be proven, opencode sessions endpoint must return typed failure, not empty success.

## 6.7 Phase M5: Model Catalog

Primary risk:

- Frontend or backend hardcodes models casually.

Failure chain:

```text
Model hardcoded in UI
-> backend config differs
-> user selects unsupported model
-> command fails
-> AI patches command builder
```

Risk score:

```text
severity 4 * likelihood 3 * detectability 3 = 36 High
```

Required gate:

- Model list must come only from backend config.
- Frontend model selector must be empty/error until `/api/models` succeeds.

Detection:

- Search frontend for model ids.
- Search backend outside config for model ids.

Correction:

- Move all model ids to configuration.

## 6.8 Phase R6: Runtime and Command Execution

Primary risk:

- AI guesses command syntax or hides command failure.

Failure chain:

```text
Command builder guessed
-> CLI prints usage to stderr
-> output pump streams stderr
-> run marked complete
-> UI displays output as if success
```

Risk score:

```text
severity 5 * likelihood 3 * detectability 4 = 60 High
```

Required gate:

- `doc/discovery/cli-command-syntax.md` must exist and include exact command examples.
- Command builder tests must reference the documented syntax.

Required detection:

- Command builder unit tests assert exact argument arrays.
- Non-zero exit marks run failed.
- Usage/help output from stderr with non-zero exit is not success.

Correction:

- Stop runtime work.
- Re-run CLI syntax discovery.

## 6.9 Phase E7: SSE Event Streaming

Primary risk:

- The UI never receives terminal state or receives multiple terminal states.

Failure chain:

```text
Process exits
-> output pump misses exit handler
-> EventSource stays open
-> user presses stop
-> stop reports not found/finished
-> AI patches stop path
-> stream bug remains
```

Risk score:

```text
severity 4 * likelihood 3 * detectability 4 = 48 High
```

Required gate:

- ProcessOutputPump test must prove exactly one terminal event.

Required terminal event rules:

- exit code 0 -> `done`
- non-zero exit -> `error`
- stopped by user -> `stopped`
- process start failure -> `error`

Correction:

- Fix output pump and registry before UI timeline work.

## 6.10 Phase F6/U7: Frontend API and UI

Primary risk:

- Frontend is built around imaginary success data.

Failure chain:

```text
API not ready
-> AI adds mock data to build UI
-> UI components assume mock shape
-> backend later differs
-> API client patched repeatedly
```

Risk score:

```text
severity 5 * likelihood 4 * detectability 4 = 80 Critical
```

Required gate:

- Frontend business page may only be implemented after its API endpoint exists.

Page-specific gates:

```text
Overview requires /api/health
Sessions requires /api/sessions
Control requires /api/sessions and /api/models
Runs requires /api/runs/{id}/events
Settings can exist earlier because it only stores local connection settings
```

Detection:

- Search frontend for arrays named `mock`, `sample`, `demo`, or hardcoded sessions/models.
- UI tests must cover error states.

Correction:

- Remove fake data.
- Show API error state.

## 6.11 Phase P8: PWA

Primary risk:

- Service worker caches API responses and hides backend failures.

Failure chain:

```text
API returns old success
-> backend now fails
-> service worker serves cached success
-> user sees stale sessions
```

Risk score:

```text
severity 3 * likelihood 3 * detectability 4 = 36 High
```

Required gate:

- Service worker must explicitly exclude `/api/`.

Detection:

- Test failed backend after service worker install.
- UI must show network failure, not cached API data.

Correction:

- Remove API caching.
- Bump service worker cache version.

## 7. Cross-Cutting AI Drift Risks

## 7.1 Allowed-Files Drift

Risk:

- AI changes files outside the ticket to "make tests pass."

Score:

```text
severity 4 * likelihood 3 * detectability 3 = 36 High
```

Control:

- Every ticket must list allowed files.
- After each ticket, run a changed-files audit.

Detection:

```bash
find . -type f -newer <ticket-start-marker>
```

or git status when repository is initialized.

Correction:

- Revert unrelated changes.
- Split required extra changes into new ticket.

## 7.2 Test Fixture Drift

Risk:

- Tests validate simplified invented fixtures, not real sanitized samples.

Score:

```text
severity 5 * likelihood 3 * detectability 5 = 75 Critical
```

Control:

- Parser tests must include sanitized real sample structures from discovery.
- Synthetic tests are allowed only for missing-field and malformed-file cases.

Correction:

- Replace invented parser success fixture with sanitized real sample.

## 7.3 Contract Drift

Risk:

- Backend DTO changes but frontend type does not.

Score:

```text
severity 4 * likelihood 3 * detectability 4 = 48 High
```

Control:

- Maintain `doc/contracts/backend-api-contract.md`.
- Add a ticket to generate or manually update JSON examples from backend DTOs.
- Frontend API client must be updated only after contract update.

Correction:

- Update contract first.
- Update backend/frontend separately.

## 7.4 Error Handling Drift

Risk:

- Some endpoints return plain strings, empty arrays, or HTML errors.

Score:

```text
severity 3 * likelihood 4 * detectability 3 = 36 High
```

Control:

- Global exception handler.
- Controller tests for error shape.

Correction:

- Add missing ApiException conversion.

## 7.5 Security Drift

Risk:

- Command endpoint is left public during development.

Score:

```text
severity 5 * likelihood 2 * detectability 4 = 40 High
```

Control:

- Auth tests must exist before runtime endpoints.
- Every endpoint test includes unauthorized check.

Correction:

- No command runtime tickets until auth tests pass.

## 8. Dead Loop Trigger Catalog

These are the exact signals that AI implementation is entering a dangerous loop.

### Trigger A: "Temporarily use mock data"

Required action:

```text
Stop. Reject. Replace with API error/loading state.
```

### Trigger B: "Fallback to file name when field missing"

Required action:

```text
Stop. Reject. Parser must return typed field-missing error.
```

### Trigger C: "Return empty list when scan fails"

Required action:

```text
Stop. Reject. Return typed scan error.
```

### Trigger D: "Let's infer CLI args"

Required action:

```text
Stop. Run CLI syntax discovery ticket.
```

### Trigger E: "Frontend can be built first with placeholders"

Required action:

```text
Stop. Only shell/settings/error-state pages can precede real APIs.
```

### Trigger F: "Catch all exceptions and continue"

Required action:

```text
Stop. Convert to typed ApiException or BLOCKED.
```

### Trigger G: "Make controller call scanner directly"

Required action:

```text
Stop. Route must be controller -> service -> adapter/scanner.
```

### Trigger H: "Service worker caches everything"

Required action:

```text
Stop. API routes must be excluded from cache.
```

### Trigger I: "Run marked complete despite non-zero exit"

Required action:

```text
Stop. Runtime state machine is wrong.
```

### Trigger J: "AI modifies plan to fit code"

Required action:

```text
Stop. Plan changes must be explicit documentation ticket, not hidden in implementation.
```

## 9. Required Additional Tickets

The current `plan.md` should be extended with the following tickets before implementation begins.

## 9.1 RISK-001 Add Ticket Start/End Audit Rule

Goal:

- Prevent allowed-files drift.

Required document update:

- Add a rule that every ticket begins by recording current file list or git status.
- Every ticket ends by listing changed files.

Verification:

```bash
rg "changed files audit|allowed-files audit" doc/plan.md
```

## 9.2 RISK-002 Add Discovery Gate Checklist

Goal:

- Prevent parser work before real samples.

Required document update:

- Add a checklist before Phase D4.
- D4 cannot start unless all P0 discovery docs exist.

Verification:

```bash
rg "Discovery Gate|D4 cannot start" doc/plan.md
```

## 9.3 RISK-003 Add CLI Syntax Evidence Ticket

Goal:

- Prevent command builder guessing.

Required new ticket:

- `P0-006 Verify CLI Command Syntax`

Required output:

- `doc/discovery/cli-command-syntax.md`

Required content:

- exact `codex --help` relevant command lines
- exact `opencode --help` relevant command lines
- one static selected-session command evidence document per supported CLI

## 9.4 RISK-004 Add Model Catalog Discovery Ticket

Goal:

- Prevent frontend/hardcoded model drift.

Required output:

- `doc/discovery/model-catalog.md`

Required content:

- model id
- label
- supported source
- command model name
- enabled flag

## 9.5 RISK-005 Add Parser Sample Fixture Gate

Goal:

- Ensure parser tests use sanitized real samples.

Required rule:

- Parser success tests cannot use invented fixtures.
- They must reference sanitized sample files stored under `src/test/resources/samples/`.

## 9.6 RISK-006 Add Unified Error Contract Tests

Goal:

- Ensure all errors are JSON.

Required tests:

- missing token
- wrong token
- missing session
- scanner failure
- parser unknown format
- model unsupported
- run not found

## 9.7 RISK-007 Add Process Non-Zero Exit Test

Goal:

- Ensure failed commands do not look successful.

Required behavior:

- non-zero exit emits `error`
- run state becomes `FAILED`
- frontend displays failed state

## 9.8 RISK-008 Add Service Worker API Exclusion Test

Goal:

- Ensure PWA never caches API data.

Required behavior:

- `/api/health` and `/api/sessions` bypass cache.

## 10. Required Corrections to Current Plan

The current `plan.md` is a solid start but needs these corrections.

### 10.1 Move frontend business pages later

Current concern:

- Frontend pages are planned before all real backend APIs are proven.

Correction:

- Allow `SettingsPage` and `OverviewPage` connection state early.
- Delay `SessionsPage` until `/api/sessions` exists.
- Delay `ControlPage` until `/api/sessions` and `/api/models` exist.
- Delay `RunsPage` until SSE terminal event tests pass.

### 10.2 Add parser implementation prerequisites

Current concern:

- Scanner/detector/parser tickets are close together, but parser tickets need a hard prerequisite.

Correction:

- Parser tickets must name the exact discovery sample file they support.
- If no sample exists, parser ticket is blocked.

### 10.3 Add specific source-failure behavior

Current concern:

- Unified `/api/sessions` behavior is documented, but tests are not yet explicit enough.

Correction:

- Add test where Codex fails and opencode succeeds.
- Expected result for unified endpoint: 424 with Codex error.
- Source-specific opencode endpoint may still succeed.

### 10.4 Add static command construction gates

Current concern:

- Command builder tickets can compile without proving their argument arrays match documented CLI syntax and existing session evidence.

Correction:

- Add command builder unit tests after static command evidence.
- Do not execute live Codex or opencode commands as a gate.
- If static evidence or command builder tests fail, command execution for that source remains disabled.

### 10.5 Add frontend no-hardcode checks

Current concern:

- "No mock" is documented, but no explicit verification command exists.

Correction:

- Add search checks for `mock`, `sample`, `demo`, hardcoded model ids, hardcoded sessions.

## 11. Revised Implementation Go/No-Go Matrix

| Work Item | Required Before Start | If Missing |
| --- | --- | --- |
| Backend skeleton | PROJECT_SPEC constants | BLOCKED |
| Auth | token config rule | BLOCKED |
| Codex scanner | codex runtime discovery | BLOCKED |
| Codex parser | codex sanitized sample | BLOCKED |
| opencode scanner | opencode runtime discovery | BLOCKED |
| opencode parser | opencode sanitized sample | BLOCKED |
| Model API | model catalog discovery/config | BLOCKED |
| Command builder | CLI syntax evidence | BLOCKED |
| Run API | command builder test + auth test | BLOCKED |
| SSE API | process lifecycle test | BLOCKED |
| Overview UI | `/api/health` real endpoint | BLOCKED |
| Sessions UI | `/api/sessions` real endpoint | BLOCKED |
| Control UI | sessions + models APIs | BLOCKED |
| Runs UI | SSE terminal event test | BLOCKED |
| PWA | frontend shell builds | BLOCKED |

## 12. Manual Review Checkpoints

Manual review is required at these points:

1. After P0 discovery.
2. After contract DTOs.
3. After first Codex parser.
4. After first opencode parser.
5. Before command execution.
6. Before frontend Control page.
7. Before exposing server beyond localhost.

Manual review questions:

- Did this ticket use real data?
- Did it add fallback success?
- Did it modify only allowed files?
- Did it preserve typed errors?
- Did it introduce a guessed field?
- Did it add a hardcoded model/session?
- Did verification prove real behavior?

If any answer is bad, stop the next ticket.

## 13. Reassessment of Dead Loop Possibility

### 13.1 Can the project still enter an unrecoverable AI loop?

Yes, if the implementation ignores the gates.

Most likely unrecoverable loop:

```text
Codex/opencode format unknown
-> AI guesses parser
-> API returns plausible sessions
-> UI displays them
-> command execution fails
-> AI patches runtime and UI
-> parser remains wrong
-> more patches accumulate
```

This is the highest-probability destructive loop.

### 13.2 Can the project avoid that loop?

Yes, if these rules are enforced:

```text
No parser without sample.
No command builder without CLI syntax evidence.
No business UI without real endpoint.
No endpoint failure converted to empty success.
No mock data.
No cross-file opportunistic fixes.
```

### 13.3 Current documentation status

Current documentation is directionally correct but still incomplete as a safety system.

What is good:

- Project constants are fixed.
- No mock/no fallback is documented.
- Architecture is decoupled.
- Plan is ticket-based.
- Basic risk assessment exists.

What is not yet enough:

- Plan needs more explicit gates.
- Plan needs more negative tests.
- Plan needs CLI syntax evidence ticket.
- Plan needs parser sample fixture gate.
- Plan needs frontend no-hardcode verification.
- Plan needs service worker API cache exclusion test.

Therefore the current state should be rated:

```text
Design foundation: good
Implementation safety: not yet sufficient
Dead-loop prevention: partial, needs added gates
```

## 14. Recommended Immediate Next Step

Before writing application code, update `doc/plan.md` with the risk-control tickets from section 9.

Do not start Spring Boot code yet if the goal is maximum AI reliability.

The next documentation task should be:

```text
Add mandatory gate tickets RISK-001 through RISK-008 into doc/plan.md.
```

After that, run the first discovery ticket only.

## 15. Final Conservative Conclusion

The project is feasible, but the current plan is still too permissive for unattended AI implementation.

The main risk is not that AI cannot write Java or React. The main risk is that AI will continue past unknown local runtime facts and create a plausible but false implementation.

If the added risk gates are implemented, the project becomes suitable for AI-driven incremental development.

If the gates are not implemented, there is a meaningful chance of entering a self-reinforcing dead loop where:

```text
wrong parser assumptions
-> wrong API data
-> wrong UI state
-> wrong command execution
-> repeated patches in the wrong layer
```

Final risk rating right now:

```text
Medium-high
```

Final risk rating after adding gates and completing discovery:

```text
Medium-low
```

Final risk rating after real parser samples, command syntax evidence, and endpoint contract tests:

```text
Low to medium-low
```
