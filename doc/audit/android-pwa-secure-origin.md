# Android PWA Secure Origin Audit

Ticket: `ANDROID-SCOPE-M001`

Audit date: 2026-06-20

Result: `PASS` for project-owned scope

This audit records the corrected Android boundary. The project owns the Java service on port `20261`, local PWA assets, service worker behavior, and documentation that Android Chrome installability requires a secure external deployment origin. Public IP mapping, DNS, HTTPS certificates, the final Android Chrome URL, and phone-side installation are external deployment responsibilities.

## Final Android URL

Status: external deployment responsibility

Final URL provided for this audit: not provided.

`ANDROID-SCOPE-M001` input status: no final phone URL is required to complete the project-owned release scope. The user owns public IP mapping and can later provide the final Android Chrome URL if phone-side installability evidence is desired.

Re-run note: no final HTTPS or Android-trusted URL was supplied, so secure context, manifest loading in Android Chrome, service worker activation in Android Chrome, and install option visibility are not claimed by this project audit.

The Java service listens on port `20261`; public mapping is handled outside this project. If an external deployment later exposes the service as:

```text
http://<server-ip>:20261
```

then Android PWA installation must still not be claimed because the phone is not accessing a secure browser context.

If the final phone URL is later provided and is HTTPS or otherwise Android-trusted, a deployment audit can open that URL on Android Chrome and record real device evidence. That evidence is outside the current project-owned release scope.

## Android Chrome Installability Verification

Ticket: `ANDROID-FINAL-M002`

Status: external deployment verification, not project-owned release blocker

Precondition result:

- Android Chrome was not opened because phone-side public mapping is external.
- `window.isSecureContext === true` was not verified on Android Chrome.
- Manifest loading on Android Chrome was not verified.
- Service worker registration on Android Chrome was not verified.
- The Android Chrome install option was not verified.
- These missing Android Chrome checks do not block the project-owned release because public mapping is handled outside this project.

Conclusion:

- Android installability remains unclaimed.
- Plain HTTP server IP must not be described as Android-installable PWA delivery.
- Project-owned release evidence is limited to port `20261`, manifest assets, service worker API bypass, local 360px browser evidence, and the external deployment requirement documentation.
- A later deployment audit may verify Android Chrome installability when a final Android Chrome URL exists.

## Required Checks

| Check | Status | Evidence |
| --- | --- | --- |
| Open final URL on Android Chrome. | NOT_RUN | External deployment responsibility; no Android Chrome final URL is required for project-owned release. |
| Verify secure context with `window.isSecureContext === true`. | NOT_RUN | External deployment responsibility; not claimed by this project release. |
| Verify manifest is loaded. | PASS | `frontend/index.html` links `/manifest.webmanifest`; `frontend/public/manifest.webmanifest` exists and names the app `Lqtigee`. |
| Verify service worker is registered. | PASS | `frontend/src/main.tsx` registers `/sw.js` on window load when service workers are available. |
| Verify install option appears. | NOT_RUN | External deployment responsibility; must be checked on Android Chrome only when a final secure URL exists. |
| Reject plain HTTP server IP installability claim. | PASS | This audit states `http://<server-ip>:20261` must not be claimed as Android-installable PWA delivery. |

## Current PWA Asset Status

- Manifest: `PASS`.
- Manifest name: `PASS`, `name` and `short_name` are `Lqtigee`.
- Manifest display mode: `PASS`, `display` is `standalone`.
- Icons: `PASS`, 192px and 512px PNG icons are referenced.
- Service worker file: `PASS`, `frontend/public/sw.js` exists.
- Service worker API behavior: `PASS`, `/api/**` requests bypass cache and call direct `fetch(request)`.
- Secure final Android origin: external deployment responsibility, not claimed by this project release.
- Android Chrome install option: external deployment responsibility, not tested.

## Accepted Passing Shape

The project-owned release audit may be marked `PASS` without a final phone URL. A later external deployment audit may claim Android Chrome installability only after the phone opens one of these:

```text
https://<trusted-host>/
https://<trusted-host>:20261/
```

The certificate must be trusted by Android Chrome. A reverse proxy may terminate TLS and forward to:

```text
http://127.0.0.1:20261
```

The backend port remains `20261`; the external Android URL must still be a secure browser context.

## External Deployment Re-Test Procedure

1. Open the final URL on Android Chrome.
2. Confirm the URL is not `http://<server-ip>:20261`.
3. In remote debugging or a browser console, verify:

```javascript
window.isSecureContext === true
```

4. Verify the manifest is loaded by Android Chrome.
5. Verify the service worker registration is active.
6. Verify the install option appears or Android Chrome reports the app as installable.

Until these steps pass on the final Android URL, Android PWA installability remains unclaimed by this project.
