# Post-Audit Delivery Tracker

Ticket: `DELIVERY-M001`

Source audit: `doc/audit/release-checklist-status.md`

All entries start as `OPEN`. A later ticket may close one entry only after its verification passes and the related release checklist row can be re-audited.

| Blocker | Status | Clearing Tickets |
| --- | --- | --- |
| `CodexAdapter.discoverSessions()` is not implemented. | OPEN | `CODEX-ADAPTER-M001`, `CODEX-ADAPTER-M002`, `CODEX-ADAPTER-M003` |
| `OpencodeAdapter.discoverSessions()` is not implemented. | OPEN | `OPENCODE-JDBC-M001`, `OPENCODE-ERR-M001`, `OPENCODE-READ-M001`, `OPENCODE-READ-M002`, `OPENCODE-READ-M003`, `OPENCODE-READ-M004`, `OPENCODE-READ-M005`, `OPENCODE-READ-M006`, `OPENCODE-ADAPTER-M001`, `OPENCODE-ADAPTER-M002`, `OPENCODE-ADAPTER-M003` |
| `OpencodeSqliteSessionReader.readSessions()` is not implemented. | OPEN | `OPENCODE-JDBC-M001`, `OPENCODE-ERR-M001`, `OPENCODE-READ-M001`, `OPENCODE-READ-M002`, `OPENCODE-READ-M003`, `OPENCODE-READ-M004`, `OPENCODE-READ-M005`, `OPENCODE-READ-M006` |
| `SessionService.getRequiredSession()` is not implemented. | OPEN | `SESSION-FIX-M001`, `SESSION-FIX-M002` |
| `/api/runs`, `/api/runs/{runId}/events`, and `/api/runs/{runId}/stop` backend controllers are missing. | OPEN | `RUN-DTO-M001`, `RUN-REGISTRY-M001`, `RUN-REGISTRY-M002`, `RUN-SERVICE-M001`, `RUN-SERVICE-M002`, `RUN-SERVICE-M003`, `RUN-SERVICE-M004`, `RUN-SERVICE-M005`, `RUN-SERVICE-M006`, `RUN-SERVICE-M007`, `RUN-API-M001`, `RUN-API-M002`, `RUN-API-M003`, `RUN-API-M004` |
| Frontend `App.tsx` does not mount the implemented shell, navigation, or pages. | OPEN | `APP-WIRE-M001`, `APP-WIRE-M002`, `APP-WIRE-M003` |
| Sessions API cannot yet return successful real Codex/opencode session data. | OPEN | `CODEX-ADAPTER-M001`, `CODEX-ADAPTER-M002`, `CODEX-ADAPTER-M003`, `OPENCODE-JDBC-M001`, `OPENCODE-READ-M001`, `OPENCODE-READ-M002`, `OPENCODE-READ-M003`, `OPENCODE-READ-M004`, `OPENCODE-READ-M005`, `OPENCODE-READ-M006`, `OPENCODE-ADAPTER-M001`, `OPENCODE-ADAPTER-M002`, `OPENCODE-ADAPTER-M003` |
| Runs API cannot yet start, stream, or stop a real process from the phone UI. | OPEN | `SESSION-FIX-M001`, `SESSION-FIX-M002`, `RUN-DTO-M001`, `RUN-REGISTRY-M001`, `RUN-REGISTRY-M002`, `RUN-SERVICE-M001`, `RUN-SERVICE-M002`, `RUN-SERVICE-M003`, `RUN-SERVICE-M004`, `RUN-SERVICE-M005`, `RUN-SERVICE-M006`, `RUN-SERVICE-M007`, `RUN-API-M001`, `RUN-API-M002`, `RUN-API-M003`, `RUN-API-M004`, `APP-WIRE-M001`, `APP-WIRE-M002` |
| Android PWA secure-origin and installability checks have not been run. | OPEN | `APP-WIRE-M004`, `RELEASE-AUDIT-M002` |

## Database Boundary

- Lqtigee-owned persistence remains PostgreSQL.
- opencode session discovery reads opencode-owned SQLite as an external read-only source.
- Codex session discovery reads Codex-owned JSONL files.
- No delivery ticket may replace real session discovery with PostgreSQL cached rows.

## Closure Rule

After the tickets above are implemented, run `RELEASE-AUDIT-M001`. If Android installation is still not verified with a final secure URL, release remains blocked until `RELEASE-AUDIT-M002` passes.
