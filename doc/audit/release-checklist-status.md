# Release Checklist Status

Ticket: `AUDIT-M005`

Audit date: 2026-06-20

Result: release is blocked.

This audit marks every item in `doc/quality/release-checklist.md` as `PASS`, `FAIL`, or `NOT_RUN`.

Status rules:

- `PASS`: verified by current command output, live backend check, code audit, or existing committed audit evidence.
- `FAIL`: current implementation contradicts the release requirement.
- `NOT_RUN`: the requirement needs a browser, phone, secure deployment URL, or end-to-end run that was not executed in this audit.

## Verification Evidence

- `mvn test`: `PASS`, 40 tests run, 0 failures, 0 errors.
- Live backend spot check: `PASS`, server started on `127.0.0.1:20261` with `LQTIGEE_API_TOKEN=audit-token`.
- `GET /api/health`: `PASS`, returned HTTP 200 JSON without a token.
- `GET /api/sessions` without token: `PASS`, returned HTTP 401 with `code=AUTH_TOKEN_MISSING`.
- `GET /api/sessions` with wrong token: `PASS`, returned HTTP 401 with `code=AUTH_TOKEN_INVALID`.
- `GET /api/sessions` with valid token: returned HTTP 424 with `code=CODEX_SESSION_SCAN_FAILED`, proving release is blocked until real session discovery is wired.
- `cd frontend && npm run typecheck`: `PASS`.
- `cd frontend && npm run build`: `PASS`.
- Frontend generated files were removed after verification: `frontend/node_modules`, `frontend/package-lock.json`, `frontend/dist`.

## 1. Documentation Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| `PROJECT_SPEC.md` exists. | PASS | File exists. |
| `README.md` links all required docs. | PASS | README links requirements, architecture, implementation design, API contract, fixtures, security matrix, PWA installability, plan, micro tickets, risk docs, quality docs, release checklist, and `AGENTS.md`. |
| `AGENTS.md` requires one micro ticket at a time. | PASS | `AGENTS.md` says to select exactly one micro ticket and implement only that ticket. |
| `doc/contracts/backend-api-contract.md` exists. | PASS | File exists. |
| `doc/contracts/backend-response-fixtures.md` exists. | PASS | File exists. |
| `doc/security/command-permission-matrix.md` exists. | PASS | File exists. |
| `doc/quality/definition-of-done.md` exists. | PASS | File exists. |
| `doc/quality/e2e-matrix.md` exists. | PASS | File exists. |
| `doc/quality/readiness-95.md` exists. | PASS | File exists. |

## 2. Backend Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| `mvn test` passes. | PASS | Maven test run completed with 40 tests, 0 failures, 0 errors. |
| Backend starts on port `20261`. | PASS | Live audit started Spring Boot and reached `http://127.0.0.1:20261/api/health`. |
| `/api/health` returns JSON without token. | PASS | Live audit received HTTP 200 JSON body with service name, app name, port, status, and timestamp. |
| Protected `/api/**` routes reject missing token. | PASS | Live audit received HTTP 401 JSON with `code=AUTH_TOKEN_MISSING` for `/api/sessions`. |
| Protected `/api/**` routes reject wrong token. | PASS | Live audit received HTTP 401 JSON with `code=AUTH_TOKEN_INVALID` for `/api/sessions`. |
| Errors return `ApiErrorDto`, not HTML. | PASS | Live audit and `ErrorShapeTest` both verify JSON error bodies with `code`, `message`, `timestamp`, and `path`. |

## 3. Session Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| Codex sessions come from real JSONL files under `~/.codex/sessions`. | FAIL | `CodexAdapter.discoverSessions()` still throws `UnsupportedOperationException`; live `/api/sessions` returned `CODEX_SESSION_SCAN_FAILED`. |
| opencode sessions come from read-only SQLite access to `opencode.db`. | FAIL | `OpencodeAdapter.discoverSessions()` and `OpencodeSqliteSessionReader.readSessions()` still throw `UnsupportedOperationException`. |
| Parser tests include success fixture and missing-field failure. | FAIL | Codex parser has both tests; opencode SQLite parser/reader tests are not implemented yet. |
| API does not expose prompt transcript content. | NOT_RUN | No successful real sessions API response exists yet, so transcript exclusion cannot be verified end to end. |
| Scanner/parser failure is not returned as empty success. | PASS | Valid-token `/api/sessions` returned HTTP 424 instead of an empty success list. |

## 4. Runtime Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| Codex selected-session static evidence exists. | PASS | `doc/discovery/codex-resume-static-evidence.md` exists and is marked `PASS`. |
| opencode selected-session static evidence exists. | PASS | `doc/discovery/opencode-session-static-evidence.md` exists and is marked `PASS`. |
| Codex command builder unit test passes. | PASS | `CodexCommandBuilderTest` passed during `mvn test`. |
| opencode command builder unit test passes. | PASS | `OpencodeCommandBuilderTest` passed during `mvn test`. |
| Commands are built as argument arrays. | PASS | `CommandSpec.command()` is `List<String>` and `ProcessLauncher` uses `new ProcessBuilder(spec.command())`. |
| No runtime command uses `sh -c`. | PASS | Command builder tests assert no `sh`, `bash`, or `-c`; source command execution uses argument arrays. |
| Non-zero process exit becomes failed run state. | PASS | `ProcessOutputPump` marks non-zero exit as failed and emits an `error` terminal event; tests passed. |
| SSE emits exactly one terminal event. | PASS | `ProcessOutputPump` publishes one terminal event after `waitFor()`; tests passed. |
| Stop command terminates or reports already terminal. | FAIL | No `/api/runs/{runId}/stop` controller endpoint exists yet. |

## 5. Frontend Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| `cd frontend && npm run typecheck` passes. | PASS | TypeScript verification passed. |
| `cd frontend && npm run build` passes. | PASS | Vite production build passed. |
| No frontend business mock data exists. | PASS | `doc/audit/frontend-no-mock.md` is marked `PASS`. |
| Settings can save backend URL and token. | FAIL | `SettingsPage` has localStorage logic, but `App.tsx` still renders only the placeholder main view, so the page is not reachable as an app workflow. |
| Sessions page renders real API data or real API error. | FAIL | `SessionsPage` exists, but `App.tsx` does not mount it; backend session discovery also currently returns HTTP 424. |
| Control page cannot submit without session, model, and prompt. | FAIL | `ControlPage` contains validation code, but `App.tsx` does not mount it, so the app workflow is unavailable. |
| Runs page streams real SSE events. | FAIL | `RunsPage` exists, but `App.tsx` does not mount it and backend `/api/runs/{runId}/events` is not implemented. |

## 6. PWA / Android Gate

| Checklist Item | Status | Evidence |
| --- | --- | --- |
| Manifest name is `Lqtigee`. | PASS | `frontend/public/manifest.webmanifest` contains `name` and `short_name` set to `Lqtigee`. |
| Service worker bypasses `/api/**`. | PASS | `frontend/public/sw.js` fetch handler returns direct `fetch(request)` for paths beginning with `/api/`. |
| 360px viewport has no horizontal scroll. | NOT_RUN | No browser viewport verification was executed during this audit. |
| Final Android URL is a secure context. | NOT_RUN | No final HTTPS or trusted origin URL was provided or tested. |
| App is installable in Android Chrome. | NOT_RUN | No Android Chrome installability test was executed. |

## 7. Release Blockers

Release remains blocked by these concrete items:

- `CodexAdapter.discoverSessions()` is not implemented.
- `OpencodeAdapter.discoverSessions()` is not implemented.
- `OpencodeSqliteSessionReader.readSessions()` is not implemented.
- `SessionService.getRequiredSession()` is not implemented.
- `/api/runs`, `/api/runs/{runId}/events`, and `/api/runs/{runId}/stop` backend controllers are missing.
- Frontend `App.tsx` does not mount the implemented shell, navigation, or pages.
- Sessions API cannot yet return successful real Codex/opencode session data.
- Runs API cannot yet start, stream, or stop a real process from the phone UI.
- Android PWA secure-origin and installability checks have not been run.

Until all `FAIL` and `NOT_RUN` rows above are cleared, release is blocked.
