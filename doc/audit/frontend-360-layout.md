# Frontend 360px Layout Audit

Ticket: APP-WIRE-M004

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

- NOT_RUN
- Local executable check did not find `chromium`, `chromium-browser`, `google-chrome`, `google-chrome-stable`, or `playwright`.
- Because no local browser tooling was available, no 360px screenshot claim is made.

Known horizontal overflow:

- PASS
- No known horizontal overflow was found in the inspected CSS.

Blocked items:

- None.
