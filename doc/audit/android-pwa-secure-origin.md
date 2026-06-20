# Android PWA Secure Origin Audit

Ticket: `ANDROID-FINAL-M001`

Audit date: 2026-06-20

Result: `BLOCKED`

This audit re-checks whether the final phone URL can support Android Chrome PWA installation. It does not claim installability unless the final Android URL is opened on Android Chrome and all required checks pass.

## Final Android URL

Status: `BLOCKED`

Final URL provided for this audit: not provided.

`ANDROID-FINAL-M001` input status: no final phone URL was provided in the active implementation thread. The exact missing input is the final Android Chrome URL that the phone will open, for example an HTTPS reverse-proxy origin that maps to the Java service on port `20261`.

Re-run note: no final HTTPS or Android-trusted URL was supplied for `ANDROID-FINAL-M001`, so secure context, manifest loading in Android Chrome, service worker activation in Android Chrome, and install option visibility remain `BLOCKED`.

The Java service listens on port `20261`, but Android PWA installability cannot be proven from the local service port alone. If the final phone URL is:

```text
http://<server-ip>:20261
```

then Android PWA installation is `BLOCKED` because the phone is not accessing a secure browser context.

If the final phone URL is later provided and is HTTPS or otherwise Android-trusted, this audit should change only to `ready for Android Chrome verification`; installability still must remain unclaimed until `ANDROID-FINAL-M002` opens that URL on Android Chrome and records real device evidence.

## Android Chrome Installability Verification

Ticket: `ANDROID-FINAL-M002`

Status: `BLOCKED`

Precondition result:

- `ANDROID-FINAL-M001` did not record an HTTPS or Android-trusted final URL.
- Android Chrome was not opened because there is no final phone URL to open.
- `window.isSecureContext === true` was not verified on Android Chrome.
- Manifest loading on Android Chrome was not verified.
- Service worker registration on Android Chrome was not verified.
- The Android Chrome install option was not verified.

Conclusion:

- Android installability remains unclaimed.
- The secure context row must remain `NOT_RUN` in the release checklist.
- The app installability row must remain `NOT_RUN` in the release checklist.
- The next valid implementation step requires the user or deployment environment to provide the final Android Chrome URL.

## Required Checks

| Check | Status | Evidence |
| --- | --- | --- |
| Open final URL on Android Chrome. | BLOCKED | No final Android URL was provided. |
| Verify secure context with `window.isSecureContext === true`. | BLOCKED | Cannot run on Android Chrome without final URL. |
| Verify manifest is loaded. | PASS | `frontend/index.html` links `/manifest.webmanifest`; `frontend/public/manifest.webmanifest` exists and names the app `Lqtigee`. |
| Verify service worker is registered. | PASS | `frontend/src/main.tsx` registers `/sw.js` on window load when service workers are available. |
| Verify install option appears. | BLOCKED | Must be checked on Android Chrome after opening the final secure URL. |
| Reject plain HTTP server IP installability claim. | PASS | This audit marks `http://<server-ip>:20261` as `BLOCKED`. |

## Current PWA Asset Status

- Manifest: `PASS`.
- Manifest name: `PASS`, `name` and `short_name` are `Lqtigee`.
- Manifest display mode: `PASS`, `display` is `standalone`.
- Icons: `PASS`, 192px and 512px PNG icons are referenced.
- Service worker file: `PASS`, `frontend/public/sw.js` exists.
- Service worker API behavior: `PASS`, `/api/**` requests bypass cache and call direct `fetch(request)`.
- Secure final Android origin: `BLOCKED`, no HTTPS or Android-trusted origin was provided.
- Android Chrome install option: `BLOCKED`, not tested.

## Accepted Passing Shape

The final release audit may be marked `PASS` only after the phone opens one of these:

```text
https://<trusted-host>/
https://<trusted-host>:20261/
```

The certificate must be trusted by Android Chrome. A reverse proxy may terminate TLS and forward to:

```text
http://127.0.0.1:20261
```

The backend port remains `20261`; the external Android URL must still be a secure browser context.

## Required Re-Test Procedure

1. Open the final URL on Android Chrome.
2. Confirm the URL is not `http://<server-ip>:20261`.
3. In remote debugging or a browser console, verify:

```javascript
window.isSecureContext === true
```

4. Verify the manifest is loaded by Android Chrome.
5. Verify the service worker registration is active.
6. Verify the install option appears or Android Chrome reports the app as installable.

Until these steps pass on the final Android URL, Android PWA installability is `BLOCKED`.
