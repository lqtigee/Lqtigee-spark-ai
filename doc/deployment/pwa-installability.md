# PWA Installability Deployment Requirement

This project uses PWA-first delivery for Android installation. That choice has a deployment requirement: the phone must access the app from a secure browser context.

## 1. External Requirement

The Java backend still exposes one service port:

```text
20261
```

But Android installation through Chrome requires the browser origin to satisfy PWA installability rules.

Required external access shape:

```text
https://<trusted-host>/...
```

or a browser-recognized local development secure origin.

Plain HTTP to a LAN or fixed IP address is not enough for installable PWA behavior on Android Chrome because service workers require secure contexts. `http://localhost` is treated specially for local development, but a phone accessing `http://server-ip:20261` is not localhost from the phone.

## 2. Accepted Deployment Shapes

### Option A: Reverse Proxy TLS Termination

```text
Android Chrome
  -> https://<domain-or-trusted-host>
  -> reverse proxy
  -> http://127.0.0.1:20261
```

The Java service still listens on port `20261`. The proxy handles HTTPS externally.

### Option B: Java Service HTTPS Directly

```text
Android Chrome
  -> https://<domain-or-trusted-host>:20261
  -> Java service
```

This requires a certificate trusted by Android Chrome.

### Option C: Native Android Wrapper Later

If HTTPS is not possible, PWA installation is blocked. A native Android wrapper can be designed later, but it is not v1.

## 3. Blocked Deployment Shape

```text
Android Chrome
  -> http://<server-ip>:20261
```

This can work as a browser page, but must not be claimed as Android-installable PWA delivery.

## 4. Required Checks

Before claiming Android installability:

1. Open the app on Android Chrome through the final external URL.
2. Confirm the page is a secure context.
3. Confirm manifest is loaded.
4. Confirm service worker is registered.
5. Confirm `/api/**` is not cached.
6. Confirm browser install option appears or `beforeinstallprompt` can fire.

## 5. Source Basis

- Service workers require secure contexts, with localhost treated specially for development.
- Chromium PWA installability depends on browser quality criteria such as manifest and service worker support.
- Installability must be verified on the final Android URL, not assumed from local desktop development.
