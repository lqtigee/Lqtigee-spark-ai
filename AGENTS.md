# Lqtigee-spark-ai Agent Instructions

These instructions apply to every AI agent working in this repository.

## Project Constants

- Project name: `Lqtigee-spark-ai`
- App name: `Lqtigee`
- Backend port: `20261`
- Java package root: `com.lqtigee.sparkai`
- Backend: Java 21 + Spring Boot 3
- Mobile delivery: Android-installable PWA first

## Mandatory Work Mode

All work must be ticket-driven.

Use this order:

1. Read `README.md`.
2. Read `PROJECT_SPEC.md`.
3. Read `doc/requirements.md`.
4. Read `doc/architecture.md`.
5. Read `doc/ai-implementation-risk.md`.
6. Read `doc/infinite-loop-risk.md`.
7. Read `doc/contracts/backend-api-contract.md`.
8. Read `doc/contracts/backend-response-fixtures.md`.
9. Read `doc/security/command-permission-matrix.md`.
10. Read `doc/deployment/pwa-installability.md`.
11. Read `doc/quality/definition-of-done.md`.
12. Read `doc/quality/e2e-matrix.md`.
13. Read `doc/quality/readiness-95.md`.
14. Read `doc/quality/rework-prevention.md`.
15. Read `doc/micro-tickets.md`.
16. Select exactly one micro ticket.
17. Implement only that ticket.
18. Run that ticket's verification command.
19. Report changed files and verification result.

If a user asks for a new feature, bug fix, refactor, or UI change that is not already represented as a micro ticket, do not implement it directly. First create or update a micro ticket in `doc/micro-tickets.md`, then implement the ticket only after the user agrees or explicitly asks to proceed.

## Hard Prohibitions

Do not:

- Implement multiple micro tickets in one pass.
- Modify files outside the selected ticket's allowed files.
- Add mock data.
- Add fake sessions.
- Add fake model lists.
- Convert failures into empty successful arrays.
- Guess Codex session format.
- Guess opencode session format.
- Guess CLI command syntax.
- Build frontend business UI before the real API exists.
- Use shell string command execution for runtime commands.
- Put filesystem scanning in controllers.
- Put process launching in controllers.
- Cache `/api/**` responses in service workers.
- Broad-refactor to fix a narrow bug.
- Change API response shape without updating `doc/contracts/backend-api-contract.md` first.
- Enable Codex runtime before selected-session command builder tests pass.
- Enable opencode runtime before selected-session command builder tests pass.
- Use a command permission mapping not defined in `doc/security/command-permission-matrix.md`.
- Claim Android installability over plain HTTP server IP.

## Required Stop Conditions

Stop and output `BLOCKED` if:

- A required discovery document is missing.
- A real sample file is missing for parser work.
- CLI syntax evidence is missing for command builder work.
- A ticket requires files not listed in its allowed files.
- Verification fails twice for the same ticket.
- The fix would require changing unrelated architecture.
- A test would require invented business data.
- A source adapter failure is being hidden as success.
- The change would violate `doc/quality/definition-of-done.md`.
- The change would make an E2E gate in `doc/quality/e2e-matrix.md` impossible to pass.
- The change violates `doc/quality/rework-prevention.md`.
- A selected-session command builder lacks static CLI/session evidence.
- Runtime mode permissions would violate `doc/security/command-permission-matrix.md`.
- Android/PWA delivery would violate `doc/deployment/pwa-installability.md`.

Use this format:

```text
BLOCKED

Ticket:
Reason:
Missing information:
Required user input or discovery artifact:
Files changed:
Files not changed:
```

## Infinite Loop Prevention

The main predicted infinite loop is:

```text
unknown session format
-> guessed parser
-> fake/plausible API session
-> UI accepts fake session
-> command execution fails
-> AI patches runtime/UI
-> parser remains wrong
-> repeated patches compound the wrong assumption
```

Prevent this by enforcing:

- No parser without sanitized real sample.
- No command builder without CLI syntax evidence.
- No business UI without real endpoint.
- No success response from failed scanner/parser.
- No retry after two failed attempts without reassessing the ticket.

## Bug Fix Protocol

For bugs:

1. Write a bug micro ticket first.
2. The ticket must state the failing behavior.
3. Add or identify one failing verification.
4. Fix the smallest responsible layer.
5. Do not refactor nearby code unless the ticket says so.
6. Re-run the failing verification.
7. Report changed files.

Bug tickets must use this format:

```text
### BUG-<area>-<number> <title>

Symptom:
Expected:
Actual:
Allowed files:
Failing verification:
Implementation:
Verification:
```

## Feature Addition Protocol

For new features:

1. Add requirements text.
2. Add architecture impact.
3. Add risk assessment if it touches parser/runtime/security/SSE/PWA caching.
4. Split into micro tickets.
5. Implement one ticket at a time.

Never implement a new feature directly from a paragraph request.

## Final Response Requirements

Every implementation response must include:

- Selected ticket ID.
- What changed.
- Changed files.
- Verification command.
- Verification result.
- Whether the next ticket is safe to start.
