# Public Access Audit

Ticket: `PUBLIC-ACCESS-M001`

Audit date: 2026-06-20

Result: `PASS` for `PUBLIC-ACCESS-M001`

## Public Server Precheck

Source: `/home/lqtiger/public_server_info/README.md`

- Public host: `118.24.15.133`.
- Desired external Lqtigee port: `20261`.
- Public server SSH check: `PASS`, host accepted key auth after local key permissions were changed to `0600`.
- Public server listener check: `20261` was not listening.
- Public server firewalld check: `20261/tcp` was not listed.
- Public server Java check: Java 17 was installed, not Java 21.
- Public server project directory check: no `Lqtigee-spark-ai` deployment directory was found under `/root`, `/opt`, `/srv`, or `/home`.

Conclusion: the public server is not currently the correct runtime host for the real Codex/opencode session data. Public mapping should forward to the machine that owns the live local Codex JSONL files and opencode SQLite database.

## Same-Port PWA Requirement

The backend artifact must serve the PWA shell from the same port as the API:

- `/api/**` remains API-owned.
- `/` returns the PWA shell.
- `/sessions`, `/control`, `/runs`, and `/settings` return the PWA shell.
- `/manifest.webmanifest`, `/sw.js`, `/icons/**`, `/assets/**`, and other static files remain static resources.

## Same-Port PWA Verification

Commands run:

```bash
cd frontend && npm install && npm run typecheck && npm run build
mvn test
mvn package -DskipTests
jar tf target/Lqtigee-spark-ai-0.0.1-SNAPSHOT.jar | rg 'BOOT-INF/classes/static/(index.html|manifest.webmanifest|sw.js|assets/)'
LQTIGEE_API_TOKEN=test-token java -jar target/Lqtigee-spark-ai-0.0.1-SNAPSHOT.jar
curl -sS http://127.0.0.1:20261/
curl -sS http://127.0.0.1:20261/sessions
curl -sS http://127.0.0.1:20261/manifest.webmanifest
curl -sS http://127.0.0.1:20261/sw.js
curl -sS http://127.0.0.1:20261/api/health
```

Results:

- Frontend typecheck and build: `PASS`.
- Backend test suite: `PASS`, 68 tests, 0 failures, 0 errors, 0 skipped.
- Backend package with frontend assets: `PASS`.
- Packaged jar contains `BOOT-INF/classes/static/index.html`: `PASS`.
- Packaged jar contains `BOOT-INF/classes/static/manifest.webmanifest`: `PASS`.
- Packaged jar contains `BOOT-INF/classes/static/sw.js`: `PASS`.
- Packaged jar contains `BOOT-INF/classes/static/assets/`: `PASS`.
- Packaged jar started on `*:20261`: `PASS`.
- `GET /`: `PASS`, returned the PWA shell with `id="root"`, `/manifest.webmanifest`, and `/assets/`.
- `GET /sessions`: `PASS`, returned the PWA shell with `id="root"`, `/manifest.webmanifest`, and `/assets/`.
- `GET /manifest.webmanifest`: `PASS`, returned `name=Lqtigee`, `short_name=Lqtigee`, and `display=standalone`.
- `GET /sw.js`: `PASS`, keeps `/api/**` on direct `fetch(event.request)`.
- `GET /api/health`: `PASS`, returned `serviceName=Lqtigee-spark-ai`, `appName=Lqtigee`, and `port=20261`.

## Android Installability Claim

Android Chrome installability is `NOT_CLAIMED` in this audit.

Plain `http://118.24.15.133:20261` may be used for reachability testing after mapping is configured, but it must not be claimed as Android-installable PWA delivery unless a later secure-origin Android Chrome audit passes.
