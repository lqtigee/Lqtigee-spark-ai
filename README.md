# Lqtigee-spark-ai

Android-installable remote control console for local Codex CLI and opencode sessions.

- Project name: `Lqtigee-spark-ai`
- App name: `Lqtigee`
- Backend port: `20261`
- Backend runtime: Java 21 + Spring Boot 3
- Mobile delivery: PWA plus Android WebView APK
- UI source of truth: backend API only

## Purpose

`Lqtigee` lets a phone connect to one Java service port on the server and operate real local AI coding sessions:

1. Inspect real Codex CLI sessions.
2. Inspect real opencode sessions.
3. Select a real session.
4. Select a real backend-configured model.
5. Send a command to that session.
6. Stream real stdout, stderr, status, and completion events.
7. Stop a real running process.

## Non-Negotiable Rules

- No mock data.
- No fake sessions.
- No fake model list.
- No silent fallback to success.
- No `catch` returning empty arrays.
- No guessed parsing of Codex or opencode state.
- No shell string command execution.
- No frontend parsing local files.
- No controller reading files or starting processes directly.
- No hidden UI success state when the API failed.

If a required format, path, command, or API behavior is unknown, the task must stop with `BLOCKED`.

## Documentation

Start here:

- [PROJECT_SPEC.md](PROJECT_SPEC.md): immutable project constants and no-rework rules.
- [doc/requirements.md](doc/requirements.md): detailed requirements and implementation rules.
- [doc/architecture.md](doc/architecture.md): decoupled frontend/backend architecture.
- [doc/implementation-design.md](doc/implementation-design.md): runtime-informed implementation design based on this machine's real Codex/opencode data.
- [doc/contracts/backend-api-contract.md](doc/contracts/backend-api-contract.md): exact backend/frontend API response contract.
- [doc/contracts/backend-response-fixtures.md](doc/contracts/backend-response-fixtures.md): test-only JSON response fixtures for contract checks.
- [doc/security/command-permission-matrix.md](doc/security/command-permission-matrix.md): required mode-to-CLI permission mapping.
- [doc/deployment/pwa-installability.md](doc/deployment/pwa-installability.md): Android PWA secure-origin deployment requirement.
- [doc/plan.md](doc/plan.md): AI-executable micro-task plan.
- [doc/micro-tickets.md](doc/micro-tickets.md): smallest executable function-point tickets.
- [doc/ai-implementation-risk.md](doc/ai-implementation-risk.md): AI implementation feasibility and dead-loop risk assessment.
- [doc/infinite-loop-risk.md](doc/infinite-loop-risk.md): explicit prediction of AI infinite-loop risk points and stop rules.
- [doc/quality/definition-of-done.md](doc/quality/definition-of-done.md): done criteria for tickets, backend, frontend, runtime, PWA, bugs, and features.
- [doc/quality/e2e-matrix.md](doc/quality/e2e-matrix.md): end-to-end verification matrix and release gate.
- [doc/quality/weak-points.md](doc/quality/weak-points.md): weak point elimination plan toward 95% implementation confidence.
- [doc/quality/readiness-95.md](doc/quality/readiness-95.md): explicit 95% readiness gate and remaining blockers.
- [doc/quality/rework-prevention.md](doc/quality/rework-prevention.md): layer ownership and stop rules to avoid repeated repairs.
- [doc/quality/release-checklist.md](doc/quality/release-checklist.md): final release checklist.
- [AGENTS.md](AGENTS.md): mandatory project instructions for future AI agents.

## Required First Step

Before writing application code, complete the discovery tickets in [doc/plan.md](doc/plan.md):

1. Verify Java, Maven, Node, and Android/PWA assumptions.
2. Verify port `20261` is available.
3. Verify Codex CLI command and state paths.
4. Verify opencode command and state paths.
5. Capture real sample session files for parser design.

No UI business page or command execution is allowed before those discovery tickets pass.

Android APK:

```text
android-app/release/Lqtigee-debug.apk
```

The APK is a native WebView shell for the mapped `20261` service. PWA installation still requires a secure browser context; the APK can load the HTTP mapped service directly.
