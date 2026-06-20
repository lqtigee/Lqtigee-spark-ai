# PWA API Cache Audit

Ticket: AUDIT-M003

Scope:

- `frontend/public/sw.js`

Inspection command:

```bash
rg -n "/api/|caches\.match|cache\.addAll|fetch\(event\.request\)|return" frontend/public/sw.js
```

Result:

- PASS
- `frontend/public/sw.js` checks `url.pathname.startsWith("/api/")`.
- API requests call `fetch(event.request)` directly.
- API branch returns before `caches.match(...)`.
- Static shell cache only includes `/`, `/manifest.webmanifest`, and icon files.

Blocked items:

- None.
