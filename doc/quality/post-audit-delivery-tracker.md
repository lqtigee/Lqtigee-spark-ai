# Post-Audit Delivery Tracker

Ticket: `TRACKER-M002`

Source audit: `doc/audit/release-checklist-status.md`

This tracker was recalculated from current committed code and audit evidence after `RELEASE-AUDIT-M003`, `ANDROID-FINAL-M001`, and `ANDROID-FINAL-M002`.

Status rules:

- `CLOSED`: the related implementation is committed and the release checklist or audit evidence verifies it.
- `OPEN`: the related release blocker still lacks required evidence.

| Blocker | Status | Clearing Tickets |
| --- | --- | --- |
| `CodexAdapter.discoverSessions()` is not implemented. | CLOSED | `RELEASE-AUDIT-M003` verifies Codex sessions come from real JSONL files and live `/api/codex/sessions` returned a sessions array. |
| `OpencodeAdapter.discoverSessions()` is not implemented. | CLOSED | `RELEASE-AUDIT-M003` verifies opencode sessions come from read-only SQLite and live `/api/opencode/sessions` returned a sessions array. |
| `OpencodeSqliteSessionReader.readSessions()` is not implemented. | CLOSED | `RELEASE-AUDIT-M003` verifies read-only SQLite access; `OpencodeSqliteSessionReaderTest` covers success, missing required fields, empty model exclusion, and metadata recovery. |
| `SessionService.getRequiredSession()` is not implemented. | CLOSED | `RELEASE-AUDIT-M003` verifies run/session tests passed; `SessionServiceTest` covers exact source/id lookup and missing-session failure. |
| `/api/runs`, `/api/runs/{runId}/events`, and `/api/runs/{runId}/stop` backend controllers are missing. | CLOSED | `RELEASE-AUDIT-M003` verifies run endpoints exist with typed validation/not-found responses; `EVIDENCE-RUNS-M004` verifies real run SSE terminal delivery. |
| Frontend `App.tsx` does not mount the implemented shell, navigation, or pages. | CLOSED | `RELEASE-AUDIT-M003` verifies `resolvePage` maps `/`, `/sessions`, `/control`, `/runs`, and `/settings` inside `AppShell`. |
| Sessions API cannot yet return successful real Codex/opencode session data. | CLOSED | `RELEASE-AUDIT-M003` verifies live `/api/sessions` returned combined real Codex and opencode session data with no empty model values. |
| Runs API cannot yet start, stream, or stop a real process from the phone UI. | CLOSED | `EVIDENCE-RUNS-M004` verifies a real Codex run started and `/api/runs/{runId}/events` returned exactly one real `done` terminal event; stop/not-found behavior is verified by the release checklist. |
| Android PWA secure-origin and installability checks have not been run. | OPEN | `ANDROID-FINAL-M002` is blocked because `ANDROID-FINAL-M001` did not record an HTTPS or Android-trusted final Android Chrome URL. |

## Database Boundary

- Lqtigee-owned persistence remains PostgreSQL.
- opencode session discovery reads opencode-owned SQLite as an external read-only source.
- Codex session discovery reads Codex-owned JSONL files.
- No delivery ticket may replace real session discovery with PostgreSQL cached rows.

## Closure Rule

Backend, frontend, session, and Runs API tracker items are closed by current committed evidence. Release remains blocked only by Android final secure-origin and installability evidence until a final Android Chrome URL is provided and `ANDROID-FINAL-M002` passes.
