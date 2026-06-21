Ticket: `BUG-MOBILE-CONSOLE-M001`

Result: `PASS`

Scope:

- Rebuilt the phone UI as a mobile-first Lqtigee remote console.
- The public `20261` address remains only an access entry to the local Java service.
- The Java service data source remains the current local machine's Codex and opencode sessions.

Real-data rules checked:

- UI session data comes only from `GET /api/sessions`.
- Session chat data comes only from `GET /api/sessions/{source}/{id}/transcript`.
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

Ticket: `CHAT-UX-M003`

Build result: `PASS mobile-first 320px no mock`

Ticket: `MOBILE-UI-M001`

Result: `PASS`

Scope:

- `/sessions` selected-chat view now uses a phone chat layout instead of stacking the full session list above the chat.
- Small-screen selected-chat mode hides the session list and relies on the existing back button to return to the list.
- The chat panel has viewport-relative height, a sticky header, a scrollable message list, and a sticky bottom composer.
- Session cards are compacted for faster scanning before a chat is opened.
- 320px behavior is handled by the existing max-width media query plus chat-specific height and wrapping rules.
- no mock data, no fake sessions, no fake models, and no explanatory mock panels were added.

Verification:

```bash
cd frontend && npm run build
rg "MOBILE-UI-M001|320px|bottom composer|no mock" doc/audit/mobile-console-ui.md
```
