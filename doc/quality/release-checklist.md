# Release Checklist

Release means the server can be started on port `20261`, local project-owned checks pass, real sessions are visible, and a selected session can receive a real command. The service is ready for the user's external mapping; public IP mapping, DNS, TLS certificates, and final phone-side Android Chrome installation are external deployment responsibilities.

## 1. Documentation Gate

- `PROJECT_SPEC.md` exists.
- `README.md` links all required docs.
- `AGENTS.md` requires one micro ticket at a time.
- `doc/contracts/backend-api-contract.md` exists.
- `doc/contracts/backend-response-fixtures.md` exists.
- `doc/security/command-permission-matrix.md` exists.
- `doc/quality/definition-of-done.md` exists.
- `doc/quality/e2e-matrix.md` exists.
- `doc/quality/readiness-95.md` exists.

## 2. Backend Gate

- `mvn test` passes.
- Backend starts on port `20261`.
- `/api/health` returns JSON without token.
- Protected `/api/**` routes reject missing token.
- Protected `/api/**` routes reject wrong token.
- Errors return `ApiErrorDto`, not HTML.

## 3. Session Gate

- Codex sessions come from real JSONL files under `~/.codex/sessions`.
- opencode sessions come from read-only SQLite access to `opencode.db`.
- Parser tests include success fixture and missing-field failure.
- API does not expose prompt transcript content.
- Scanner/parser failure is not returned as empty success.

## 4. Runtime Gate

- Codex selected-session static evidence exists.
- opencode selected-session static evidence exists.
- Codex command builder unit test passes.
- opencode command builder unit test passes.
- Commands are built as argument arrays.
- No runtime command uses `sh -c`.
- Non-zero process exit becomes failed run state.
- SSE emits exactly one terminal event.
- Stop command terminates or reports already terminal.

## 5. Frontend Gate

- `cd frontend && npm run typecheck` passes.
- `cd frontend && npm run build` passes.
- No frontend business mock data exists.
- Settings can save backend URL and token.
- Sessions page renders real API data or real API error.
- Control page cannot submit without session, model, and prompt.
- Runs page streams real SSE events.

## 6. PWA / Android Gate

- Manifest name is `Lqtigee`.
- Service worker bypasses `/api/**`.
- 360px viewport has no horizontal scroll.
- Project documents that Android Chrome installability requires a secure external deployment origin.
- Project does not claim Android Chrome installability over plain HTTP server IP.

## 7. Blockers

Release is blocked by any of these:

- Any runtime endpoint returns mock data.
- Any CLI source is guessed instead of discovered.
- Any parser fills required fields from fallback values.
- Any service worker caches API responses.
- Any frontend hardcodes sessions/models.
- Any selected-session command lacks static evidence or command builder test coverage.
- Android installability is claimed over plain HTTP server IP.
- External public mapping, DNS, TLS certificate, or phone-side Android Chrome installation is treated as project-owned release evidence.
