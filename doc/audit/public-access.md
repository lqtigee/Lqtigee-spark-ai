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

## Public Mapping Verification

Ticket: `PUBLIC-ACCESS-M002`

Result: `PASS` for HTTP reachability, `NOT_CLAIMED` for Android Chrome installability.

Mapping shape:

```text
Android/browser -> http://118.24.15.133:20261
118.24.15.133:20261 -> public-server socat -> 127.0.0.1:20262
127.0.0.1:20262 -> SSH remote forward -> local 127.0.0.1:20261
local 127.0.0.1:20261 -> Lqtigee Spring Boot jar
```

Runtime evidence:

- Local Lqtigee jar is managed by user systemd unit `lqtigee-spark-ai-public-test.service`.
- Local Lqtigee jar listens on `*:20261`.
- Public server has `20261/tcp` open in firewalld.
- Public server runs systemd unit `lqtigee-spark-ai-public-socat.service`.
- Public server listens on `0.0.0.0:20261`.
- SSH remote forward listens on public-server `127.0.0.1:20262` and forwards to local `127.0.0.1:20261`.

External verification:

```bash
curl -sS -i --max-time 10 http://118.24.15.133:20261/api/health
curl -sS --max-time 10 http://118.24.15.133:20261/
curl -sS --max-time 10 http://118.24.15.133:20261/sessions
curl -sS --max-time 10 http://118.24.15.133:20261/manifest.webmanifest
curl -sS --max-time 10 -H "Authorization: Bearer <redacted>" http://118.24.15.133:20261/api/models
curl -sS --max-time 20 -H "Authorization: Bearer <redacted>" http://118.24.15.133:20261/api/sessions
curl -sS --max-time 10 -H "Authorization: Bearer wrong" http://118.24.15.133:20261/api/sessions
```

External results:

- `GET /api/health`: `PASS`, HTTP 200, `serviceName=Lqtigee-spark-ai`, `appName=Lqtigee`, `port=20261`.
- `GET /`: `PASS`, returned PWA shell with `id="root"`, `/manifest.webmanifest`, and `/assets/`.
- `GET /sessions`: `PASS`, returned PWA shell with `id="root"`, `/manifest.webmanifest`, and `/assets/`.
- `GET /manifest.webmanifest`: `PASS`, returned `name=Lqtigee`, `short_name=Lqtigee`, and `display=standalone`.
- `GET /api/models` with the real token: `PASS`, returned 2 models.
- `GET /api/sessions` with the real token: `PASS`, returned 1164 real sessions from `CODEX` and `OPENCODE`.
- Empty model values in external sessions response: `PASS`, count `0`.
- Transcript leak check in external sessions response: `PASS`, `lastMessage` non-empty count `0`.
- `GET /api/sessions` with a wrong token: `PASS`, returned `AUTH_TOKEN_INVALID`.

Android Chrome installability:

- Final URL tested here is `http://118.24.15.133:20261`.
- Secure browser context was not verified.
- Android Chrome install option was not verified.
- Android Chrome installability remains `NOT_CLAIMED`.
- A later HTTPS or Android-trusted URL audit is required before claiming installability.

## Public Mobile Console Rebuild Verification

Ticket: `PUBLIC-ACCESS-M003`

Result: `PASS` for rebuilt UI reachability and local live session source.

Boundary:

- Public URL: `http://118.24.15.133:20261`.
- Public server role: access mapping only.
- Runtime data source: local machine Java service on `127.0.0.1:20261`.
- Live session sources: current local Codex and current local opencode.
- API token: used for authenticated checks, not recorded here.

Build evidence:

```bash
cd frontend && npm install && npm run build
mvn package -DskipTests
jar tf target/Lqtigee-spark-ai-0.0.1-SNAPSHOT.jar | rg 'BOOT-INF/classes/static/(index.html|manifest.webmanifest|assets/index-Cb9nzxpz.css|assets/index-CUF9_5A8.js)'
```

Results:

- Frontend production build: `PASS`, 50 modules transformed.
- Backend package: `PASS`.
- Jar contains rebuilt CSS asset `assets/index-Cb9nzxpz.css`: `PASS`.
- Jar contains rebuilt JS asset `assets/index-CUF9_5A8.js`: `PASS`.
- Jar contains `index.html` and `manifest.webmanifest`: `PASS`.

Runtime evidence:

- Local service unit `lqtigee-spark-ai-public-test.service`: `active`.
- Local service pid after restart: `4027599`.
- SSH remote forward from public server `127.0.0.1:20262` to local `127.0.0.1:20261`: `active`.
- Public server `lqtigee-spark-ai-public-socat.service`: `active`.
- Public server listener `0.0.0.0:20261`: `active`.

External verification:

```bash
curl -sS --max-time 10 http://118.24.15.133:20261/api/health
curl -sS --max-time 10 http://118.24.15.133:20261/sessions
curl -sS --max-time 20 -H "Authorization: Bearer <redacted>" http://118.24.15.133:20261/api/sessions
curl -sS --max-time 20 -H "Authorization: Bearer <redacted>" http://127.0.0.1:20261/api/sessions
```

External results:

- `GET /api/health`: `PASS`, returned `serviceName=Lqtigee-spark-ai`, `appName=Lqtigee`, `port=20261`.
- Health `status` field was `STARTING`; this is the current controller value, not a mapping failure.
- `GET /sessions`: `PASS`, returned the rebuilt PWA shell with `id="root"`, `manifest.webmanifest`, `assets/index-Cb9nzxpz.css`, and `assets/index-CUF9_5A8.js`.
- Authenticated public `GET /api/sessions`: `PASS`, returned 1164 sessions.
- Public source counts: `CODEX=684`, `OPENCODE=480`.
- Authenticated local `GET /api/sessions`: `PASS`, returned the same 1164 sessions.
- Local source counts: `CODEX=684`, `OPENCODE=480`.

Conclusion:

`http://118.24.15.133:20261` now serves the rebuilt mobile console UI and the authenticated session API is reading the current local machine's real Codex/opencode sessions through the local Lqtigee service.

## Public Asset Evidence Refresh

Ticket: `PUBLIC-ACCESS-M004`

Result: `PASS`

Reason:

`BUG-MOBILE-CONSOLE-M002` changed the Overview bundle after `PUBLIC-ACCESS-M003`, so the public shell asset evidence was refreshed without changing code or restarting services in this ticket.

External verification:

```bash
curl -sS --max-time 10 http://118.24.15.133:20261/sessions
curl -sS --max-time 20 -H "Authorization: Bearer <redacted>" http://118.24.15.133:20261/api/sessions
```

External results:

- `GET /sessions`: `PASS`, returned `id="root"`.
- Latest public CSS asset: `assets/index-Cb9nzxpz.css`.
- Latest public JS asset: `assets/index-Cdupm8tF.js`.
- Authenticated public `GET /api/sessions`: `PASS`, returned 1164 sessions.
- Public source counts after latest rebuild: `CODEX=684`, `OPENCODE=480`.
- Token was used for verification but not recorded.

## Public Session Chat Verification

Ticket: `PUBLIC-ACCESS-M005`

Result: `PASS` for rebuilt chat UI and transcript endpoint reachability.

Boundary:

- Public URL remains `http://118.24.15.133:20261`.
- Public server remains an access mapping layer.
- Runtime data source remains the local machine's current Codex JSONL and opencode SQLite session storage.
- Token was used for authenticated checks but not recorded.
- Transcript text was not recorded in this audit.

Build and runtime:

- Frontend production build: `PASS`, 51 modules transformed.
- Backend package: `PASS`.
- Local service `lqtigee-spark-ai-public-test.service`: `active` after restart.
- Latest public CSS asset: `assets/index-DClTdR5J.css`.
- Latest public JS asset: `assets/index-DDQBspPq.js`.

External results:

- `GET /sessions`: `PASS`, returned `id="root"` and latest chat UI assets.
- Authenticated `GET /api/sessions`: `PASS`, returned 1169 sessions.
- Source counts: `CODEX=684`, `OPENCODE=485`.
- Authenticated Codex transcript endpoint: `PASS`, selected one real Codex session and returned 2284 visible user/assistant messages.
- Authenticated opencode transcript endpoint: `PASS`, selected one real opencode session and returned 2 visible user/assistant messages.
- Transcript role sets were limited to `assistant` and `user`.

Follow-up:

The latest Codex session title derived from an environment-context user record, which is real data but not a useful display title. A follow-up title-filter ticket should exclude environment-context records from title and last-message derivation without hiding actual chat transcript messages.

Ticket `BUG-SESSION-TITLE-M002` was created to filter environment-context records from list title and preview derivation only.

## Public Codex Title Filter Verification

Ticket: `BUG-SESSION-TITLE-M002`

Result: `PASS`

External verification:

- Local service restarted after rebuild and remained `active`.
- Authenticated public `GET /api/sessions`: `PASS`, returned 1169 sessions.
- First Codex session title after filtering: real user chat title, not environment context.
- First Codex session `hasEnvTitle=false`.
- Authenticated Codex transcript endpoint still returned visible user/assistant messages.
- Transcript count for the selected Codex session: 998 messages.
- Transcript role set remained limited to `assistant` and `user`.
- Token and transcript text were not recorded.

## Mobile Public Rebuild Verification

Ticket: `MOBILE-PUBLIC-M001`

Audit date: 2026-06-22

Result: `PASS` for public 20261 rebuild and local live session API mapping.

Boundary:

- Public URL: `http://118.24.15.133:20261`.
- Public server role: access mapping only.
- Runtime data source: local machine Java service on `127.0.0.1:20261`.
- Live session sources: current local `CODEX` JSONL sessions and current local `OPENCODE` session store.
- API token: used for authenticated checks, not recorded here.
- Transcript text: not recorded here.

Build evidence:

```bash
cd frontend && npm install && npm run build
mvn package -DskipTests
jar tf target/Lqtigee-spark-ai-0.0.1-SNAPSHOT.jar | rg 'BOOT-INF/classes/static/(index.html|manifest.webmanifest|sw.js|assets/index-DNeAlTH7.js|assets/index-CCiJRPPr.css)'
```

Results:

- Frontend dependency install: `PASS`; npm reported one low severity advisory, not changed in this ticket.
- Frontend production build: `PASS`, 62 modules transformed.
- Backend package: `PASS`; jar packaging copied the rebuilt frontend resources.
- Latest CSS asset: `assets/index-CCiJRPPr.css`.
- Latest JS asset: `assets/index-DNeAlTH7.js`.
- Packaged jar contains `index.html`, `manifest.webmanifest`, `sw.js`, latest CSS asset, and latest JS asset: `PASS`.

Runtime evidence:

- Local Java service unit `lqtigee-spark-ai-public-test.service`: `active`.
- Local Java service pid after restart: `1465703`.
- Local Java listener: `127.0.0.1:20261`.
- Local SSH remote-forward unit `lqtigee-spark-ai-ssh-forward.service`: `active`.
- SSH remote forward: public server `127.0.0.1:20262` to local `127.0.0.1:20261`.
- Public server service `lqtigee-spark-ai-public-socat.service`: `active`.
- Public server listener: `0.0.0.0:20261`.
- Public server forwarding: `0.0.0.0:20261` to public server `127.0.0.1:20262`.

Public verification:

```bash
curl -sS --max-time 10 http://118.24.15.133:20261/api/health
curl -sS --max-time 10 http://118.24.15.133:20261/sessions
curl -sS --max-time 30 -H "Authorization: Bearer <redacted>" http://118.24.15.133:20261/api/sessions
curl -sS --max-time 30 -H "Authorization: Bearer <redacted>" http://127.0.0.1:20261/api/sessions
```

External results:

- Public `GET /api/health`: `PASS`, returned `serviceName=Lqtigee-spark-ai`, `appName=Lqtigee`, and `port=20261`.
- Public `GET /sessions`: `PASS`, returned the rebuilt PWA shell with `id="root"`, `manifest.webmanifest`, `assets/index-CCiJRPPr.css`, and `assets/index-DNeAlTH7.js`.
- Authenticated public `GET /api/sessions`: `PASS`, returned 1299 real sessions.
- Public source counts: `CODEX=684`, `OPENCODE=615`.
- Public sessions with non-empty title: `1299`.
- Authenticated local `GET /api/sessions`: `PASS`, returned 1299 real sessions.
- Local source counts: `CODEX=684`, `OPENCODE=615`.
- Local sessions with non-empty title: `1299`.
- Public and local session response byte counts matched exactly: `557285`.

Android Chrome installability:

- Final URL tested here is plain `http://118.24.15.133:20261`.
- Secure browser context was not verified.
- Android Chrome install option was not verified.
- Android Chrome installability is `NOT_ANDROID_INSTALLABILITY` for this audit.
- A later HTTPS or Android-trusted URL audit is required before claiming Android installability.
