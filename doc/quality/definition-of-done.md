# Definition of Done

This document defines when work is done. It prevents "almost working" states that cause rework.

## 1. Ticket-Level Done

A micro ticket is done only when:

1. It modifies only allowed files.
2. It implements exactly the ticket scope.
3. It adds no mock data.
4. It adds no fallback success.
5. It runs the ticket verification command.
6. Verification passes.
7. The response lists changed files.
8. The response states whether the next ticket is safe.

## 2. Backend Method Done

A backend method is done only when:

1. Inputs are validated.
2. Expected failures use `ApiException`.
3. It does not catch and hide errors.
4. It has a focused unit test if logic is non-trivial.
5. It does not access layers it should not access.

## 3. Backend Endpoint Done

An endpoint is done only when:

1. Auth behavior is tested.
2. Success JSON matches `doc/contracts/backend-api-contract.md`.
3. Error JSON matches `ApiErrorDto`.
4. Controller calls only service layer.
5. No filesystem or process logic is inside controller.

## 4. Parser Done

A parser/reader is done only when:

1. It uses sanitized real sample structure from `doc/discovery`.
2. It rejects missing required fields.
3. It does not fallback to filename for required fields.
4. It has a success test using sanitized real structure.
5. It has a missing-field failure test.
6. It does not expose prompt content in v1.

## 5. Runtime Done

Runtime execution is done only when:

1. Command is built as argument array.
2. No `sh -c` is used.
3. Workspace passes `PathGuard`.
4. Non-zero exit marks run `FAILED`.
5. Each run emits exactly one terminal event.
6. Stop behavior is tested.

## 6. Frontend Component Done

A frontend component is done only when:

1. It accepts data via props or hook.
2. It contains no business mock data.
3. It renders loading/error/empty/success if page-level.
4. It does not parse backend internals.
5. It typechecks.
6. It fits 320px width if visible on mobile.

## 7. PWA Done

PWA is done only when:

1. Manifest name is `Lqtigee`.
2. App can build.
3. Service worker excludes `/api/**`.
4. API failures are not cached.
5. Android installability is manually verified.

## 8. Feature Done

A feature is done only when:

1. Requirements updated.
2. Architecture updated if needed.
3. Risk updated if it touches parser/runtime/security/SSE/PWA.
4. Micro tickets added.
5. All micro tickets pass.
6. E2E checklist passes.

## 9. Bug Fix Done

A bug fix is done only when:

1. Bug ticket exists.
2. Failing behavior is reproduced.
3. One smallest responsible layer is fixed.
4. Verification passes.
5. No broad refactor was performed.

