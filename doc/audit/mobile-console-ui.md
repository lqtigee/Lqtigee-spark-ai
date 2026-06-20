Ticket: `BUG-MOBILE-CONSOLE-M001`

Result: `PASS`

Scope:

- Rebuilt the phone UI as a mobile-first Lqtigee remote console.
- The public `20261` address remains only an access entry to the local Java service.
- The Java service data source remains the current local machine's Codex and opencode sessions.

Real-data rules checked:

- UI session data comes only from `GET /api/sessions`.
- UI model data comes only from `GET /api/models`.
- UI health state comes only from `GET /api/health`.
- Overview displays API reachability as `Connected` after a successful health response, and shows the raw backend `status` field separately as service state.
- Run output comes only from real `/api/runs/{runId}/events` SSE events.
- No fake sessions, fake models, fake run ids, sample rows, or fallback successful arrays were added.

Verification:

```bash
cd frontend && npm run build
rg -n "mock|fake|placeholder|sample session|sample model" frontend/src || true
```

Build output:

```text
✓ 50 modules transformed.
✓ built in 777ms
```

No-mock search:

```text
No matches in frontend/src.
```
