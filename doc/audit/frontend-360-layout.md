# Frontend 360px Layout Audit

Ticket: APP-WIRE-M004

Evidence ticket: EVIDENCE-FRONTEND-360-M001

Scope:

- `frontend/src/styles/global.css`
- `frontend/src/components`
- `frontend/src/pages`
- Built frontend output from `npm run build`

Build command:

```bash
cd frontend && npm install && npm run typecheck && npm run build
rm -rf node_modules package-lock.json dist
```

Build result:

- PASS
- `npm run typecheck` completed successfully.
- `npm run build` completed successfully.
- Generated frontend files were removed after verification.

CSS inspection command:

```bash
rg -n "min-width|width:|grid-template-columns|white-space|overflow-x|position: fixed|left: 0|right: 0" frontend/src/styles/global.css frontend/src/components frontend/src/pages
```

360px inspection result:

- PASS
- `html`, `body`, and `.app-shell` use `min-width: 320px`; this is below the required 360px audit width.
- `.app-shell__main` uses `min-width: 0`, preventing grid/flex child overflow from the shell itself.
- Mobile bottom navigation is fixed from `left: 0` to `right: 0`, so it is viewport-width bound.
- Mobile bottom navigation uses `grid-template-columns: repeat(5, minmax(0, 1fr))`, so each item can shrink inside a 360px viewport.
- Mobile bottom navigation links use `min-width: 0`, `overflow: hidden`, `text-overflow: ellipsis`, and `white-space: nowrap`; long labels should truncate instead of forcing horizontal overflow.
- Desktop sidebar width `220px` only applies inside `@media (min-width: 900px)`, so it does not affect 360px layout.

Browser viewport check:

- PASS
- Firefox was available at `/usr/bin/firefox`.
- `geckodriver 0.36.0` was available at `/snap/bin/geckodriver`.
- The built frontend was served from `http://127.0.0.1:4173` by `npx vite preview --host 127.0.0.1 --port 4173`.
- Firefox direct screenshot mode captured real browser screenshots at `360x800`:
  - `doc/audit/screenshots/frontend-360-home.png`
  - `doc/audit/screenshots/frontend-360-sessions.png`
  - `doc/audit/screenshots/frontend-360-control.png`
  - `doc/audit/screenshots/frontend-360-runs.png`
  - `doc/audit/screenshots/frontend-360-settings.png`
- Each screenshot file was verified as `PNG image data, 360 x 800`.
- GeckoDriver top-level headless Firefox could not reduce the browser `innerWidth` below `500`, so the DOM overflow measurement loaded the built frontend inside a real `360x800` browser iframe and switched WebDriver context into that frame before measuring the application document.
- The iframe measurement keeps the application CSS viewport at `window.innerWidth=360` and reads the actual rendered app document, not source CSS.

Browser viewport commands:

```bash
cd frontend && npm install && npm run typecheck && npm run build
npx vite preview --host 127.0.0.1 --port 4173
firefox --headless --screenshot=/home/lqtiger/GIT_HUB/Lqtigee-spark-ai/doc/audit/screenshots/frontend-360-home.png --window-size=360,800 http://127.0.0.1:4173/
firefox --headless --screenshot=/home/lqtiger/GIT_HUB/Lqtigee-spark-ai/doc/audit/screenshots/frontend-360-sessions.png --window-size=360,800 http://127.0.0.1:4173/sessions
firefox --headless --screenshot=/home/lqtiger/GIT_HUB/Lqtigee-spark-ai/doc/audit/screenshots/frontend-360-control.png --window-size=360,800 http://127.0.0.1:4173/control
firefox --headless --screenshot=/home/lqtiger/GIT_HUB/Lqtigee-spark-ai/doc/audit/screenshots/frontend-360-runs.png --window-size=360,800 http://127.0.0.1:4173/runs
firefox --headless --screenshot=/home/lqtiger/GIT_HUB/Lqtigee-spark-ai/doc/audit/screenshots/frontend-360-settings.png --window-size=360,800 http://127.0.0.1:4173/settings
```

GeckoDriver 360px DOM measurement:

```text
{"path":"/","innerWidth":360,"innerHeight":800,"documentClientWidth":360,"documentScrollWidth":360,"bodyClientWidth":360,"bodyScrollWidth":360,"maxScrollWidth":360,"horizontalOverflow":false}
{"path":"/sessions","innerWidth":360,"innerHeight":800,"documentClientWidth":360,"documentScrollWidth":360,"bodyClientWidth":360,"bodyScrollWidth":360,"maxScrollWidth":360,"horizontalOverflow":false}
{"path":"/control","innerWidth":360,"innerHeight":800,"documentClientWidth":348,"documentScrollWidth":348,"bodyClientWidth":348,"bodyScrollWidth":348,"maxScrollWidth":348,"horizontalOverflow":false}
{"path":"/runs","innerWidth":360,"innerHeight":800,"documentClientWidth":360,"documentScrollWidth":360,"bodyClientWidth":360,"bodyScrollWidth":360,"maxScrollWidth":360,"horizontalOverflow":false}
{"path":"/settings","innerWidth":360,"innerHeight":800,"documentClientWidth":360,"documentScrollWidth":360,"bodyClientWidth":360,"bodyScrollWidth":360,"maxScrollWidth":360,"horizontalOverflow":false}
```

Known horizontal overflow:

- PASS
- No known horizontal overflow was found in the inspected CSS.
- Real browser DOM measurement for `/`, `/sessions`, `/control`, `/runs`, and `/settings` reported `horizontalOverflow=false` at `window.innerWidth=360`.
- The largest measured `maxScrollWidth` was `360`, which does not exceed the 360px viewport.

Observed 360px UI quality notes:

- `frontend-360-control.png` shows real API authentication errors and no fake fallback data.
- `frontend-360-settings.png` shows the Settings form is cramped at 360px. This is not horizontal overflow, but it should be handled by a separate UI refinement ticket if mobile polish becomes a release requirement.

Blocked items:

- None.
