# Release Checklist Status

Ticket: `RELEASE-AUDIT-M001`

Audit date: 2026-06-20

Result: release is blocked.

This audit recalculates every item in `doc/quality/release-checklist.md` after the session, run, and frontend wiring fixes already committed.

Status rules:

- `PASS`: verified by current command output, live backend check, code audit, or committed audit evidence.
- `FAIL`: current implementation or current local dependency data contradicts the release requirement.
- `NOT_RUN`: the requirement needs a browser, Android phone, secure deployment URL, or real end-to-end command run that was not executed in this audit.

## Verification Evidence

- `mvn test`: `PASS`, 61 tests run, 0 failures, 0 errors, 0 skipped.
- Frontend `npm install && npm run typecheck && npm run build`: `PASS`.
- Frontend generated files were removed after verification: `frontend/node_modules`, `frontend/package-lock.json`, `frontend/dist`.
- Live backend started on `127.0.0.1:20261` with `LQTIGEE_API_TOKEN=audit-token`: `PASS`.
- `GET /api/health`: `PASS`, returned HTTP 200 JSON without a token.
- `GET /api/sessions` without token: `PASS`, returned HTTP 401 with `code=AUTH_TOKEN_MISSING`.
- `GET /api/sessions` with wrong token: `PASS`, returned HTTP 401 with `code=AUTH_TOKEN_INVALID`.
- `GET /api/codex/sessions` with valid token: `PASS`, returned HTTP 200 with a `sessions` array from live Codex session files.
- `GET /api/opencode/sessions` with valid token: `PASS`, returned HTTP 200 with a `sessions` array; 4 non-runnable empty-model rows were excluded and no empty model values were returned.
- `GET /api/sessions` with valid token: `PASS`, returned HTTP 200 with combined Codex and opencode session data; no empty model values were returned.
- `POST /api/runs` with valid token and `{}` body: `PASS`, endpoint exists and returned HTTP 400 JSON with `code=VALIDATION_FAILED`.
- `GET /api/runs/not-a-real-run/events` with valid token: `PASS`, endpoint exists and returned HTTP 404 JSON with `code=RUN_NOT_FOUND`.
- `POST /api/runs/not-a-real-run/stop` with valid token: `PASS`, endpoint exists and returned HTTP 404 JSON with `code=RUN_NOT_FOUND`.
- Frontend page reachability through `App.tsx`: `PASS`, `resolvePage` maps `/`, `/sessions`, `/control`, `/runs`, and `/settings`, and renders inside `AppShell`.
- Android secure-origin and installability items remain blocked because no final HTTPS Android URL was provided or tested.

## 1. Documentation Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| `PROJECT_SPEC.md` exists. | PASS | File exists. |
| `README.md` links all required docs. | PASS | README links requirements, architecture, implementation design, API contract, fixtures, security matrix, PWA installability, plan, micro tickets, risk docs, quality docs, release checklist, and `AGENTS.md`. |
| `AGENTS.md` requires one micro ticket at a time. | PASS | `AGENTS.md` requires selecting exactly one micro ticket and implementing only that ticket. |
| `doc/contracts/backend-api-contract.md` exists. | PASS | File exists. |
| `doc/contracts/backend-response-fixtures.md` exists. | PASS | File exists. |
| `doc/security/command-permission-matrix.md` exists. | PASS | File exists. |
| `doc/quality/definition-of-done.md` exists. | PASS | File exists. |
| `doc/quality/e2e-matrix.md` exists. | PASS | File exists. |
| `doc/quality/readiness-95.md` exists. | PASS | File exists. |

## 2. Backend Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| `mvn test` passes. | PASS | Maven test run completed with 61 tests, 0 failures, 0 errors, 0 skipped. |
| Backend starts on port `20261`. | PASS | Live audit started Spring Boot and reached `http://127.0.0.1:20261/api/health`. |
| `/api/health` returns JSON without token. | PASS | Live audit received HTTP 200 JSON with service name, app name, port, status, and timestamp. |
| Protected `/api/**` routes reject missing token. | PASS | Live audit received HTTP 401 JSON with `code=AUTH_TOKEN_MISSING` for `/api/sessions`. |
| Protected `/api/**` routes reject wrong token. | PASS | Live audit received HTTP 401 JSON with `code=AUTH_TOKEN_INVALID` for `/api/sessions`. |
| Errors return `ApiErrorDto`, not HTML. | PASS | Live audit received JSON error bodies with `code`, `message`, `timestamp`, and `path`; `ErrorShapeTest` passed. |

## 3. Session Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| Codex sessions come from real JSONL files under `~/.codex/sessions`. | PASS | `CodexAdapter.discoverSessions()` uses `CodexFileScanner.scan(CODEX_HOME)` and `CodexJsonlParser.parse(path)`; live `/api/codex/sessions` returned HTTP 200 with a `sessions` array. |
| opencode sessions come from read-only SQLite access to `opencode.db`. | PASS | `OpencodeSqliteSessionReader.openReadOnly()` uses SQLite read-only mode; live `/api/opencode/sessions` returned HTTP 200. Audit evidence reports 4 non-runnable rows with empty `session.model.id` excluded. |
| Parser tests include success fixture and missing-field failure. | PASS | `CodexJsonlParserTest`, `OpencodeSqliteSessionReaderTest`, and `OpencodeSqliteSchemaGuardTest` passed during `mvn test`. |
| API does not expose prompt transcript content. | PASS | Readers still set `lastMessage` to an empty string and opencode metadata checks do not read prompt, part text, or transcript fields. |
| Scanner/parser failure is not returned as empty success. | PASS | Empty-model opencode rows are excluded only when no commandable model id exists; other missing required fields still throw `OPENCODE_SESSION_FIELD_MISSING`. |

## 4. Runtime Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| Codex selected-session static evidence exists. | PASS | `doc/discovery/codex-resume-static-evidence.md` exists and is marked `PASS`. |
| opencode selected-session static evidence exists. | PASS | `doc/discovery/opencode-session-static-evidence.md` exists and is marked `PASS`. |
| Codex command builder unit test passes. | PASS | `CodexCommandBuilderTest` passed during `mvn test`. |
| opencode command builder unit test passes. | PASS | `OpencodeCommandBuilderTest` passed during `mvn test`. |
| Commands are built as argument arrays. | PASS | `CommandSpec.command()` is `List<String>` and `ProcessLauncher` uses `new ProcessBuilder(spec.command())`. |
| No runtime command uses `sh -c`. | PASS | Command builder tests assert no `sh`, `bash`, or `-c`; source command execution uses argument arrays. |
| Non-zero process exit becomes failed run state. | PASS | `ProcessOutputPumpTest` passed and verifies failed process state/event behavior. |
| SSE emits exactly one terminal event. | PASS | `ProcessOutputPumpTest` passed and verifies one terminal event after process completion. |
| Stop command terminates or reports already terminal. | PASS | `RunServiceTest` passed and live `POST /api/runs/not-a-real-run/stop` returned typed `RUN_NOT_FOUND`; `/api/runs/{runId}/stop` controller exists. |

## 5. Frontend Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| `cd frontend && npm run typecheck` passes. | PASS | TypeScript verification passed. |
| `cd frontend && npm run build` passes. | PASS | Vite production build passed. |
| No frontend business mock data exists. | PASS | `doc/audit/frontend-no-mock.md` is marked `PASS`. |
| Settings can save backend URL and token. | PASS | `SettingsPage` persists base URL, token, and refresh seconds to localStorage; `App.tsx` maps `/settings` and renders through `AppShell`. |
| Sessions page renders real API data or real API error. | PASS | `SessionsPage` uses `useSessionsState`; live backend returned real combined session data, and `App.tsx` maps `/sessions`. |
| Control page cannot submit without session, model, and prompt. | PASS | `ControlPage.validateControlForm()` blocks missing session, model, prompt, unsupported model, and unconfirmed shell mode; `App.tsx` maps `/control`. |
| Runs page streams real SSE events. | NOT_RUN | `RunsPage` uses `useRunEvents` and the live events endpoint exists, but no real run was started during this audit. |

## 6. PWA / Android Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| Manifest name is `Lqtigee`. | PASS | `frontend/public/manifest.webmanifest` contains `name` and `short_name` set to `Lqtigee`. |
| Service worker bypasses `/api/**`. | PASS | `doc/audit/pwa-api-cache.md` is marked `PASS`. |
| 360px viewport has no horizontal scroll. | NOT_RUN | `doc/audit/frontend-360-layout.md` has CSS-level `PASS`, but browser viewport screenshot was `NOT_RUN` because no local browser tooling was available. |
| Final Android URL is a secure context. | NOT_RUN | No final HTTPS or Android-trusted origin URL was provided or tested. |
| App is installable in Android Chrome. | NOT_RUN | No Android Chrome installability test was executed. |

## 7. Release Blockers

Release remains blocked by these concrete items:

- Runs page SSE was not verified with a real started Codex or opencode process in this audit.
- 360px layout has CSS-level evidence only; browser viewport evidence is still `NOT_RUN`.
- Android final secure-origin and installability checks have not been run with a real final URL.

Until all `FAIL` and `NOT_RUN` rows above are cleared by evidence, release is blocked.
