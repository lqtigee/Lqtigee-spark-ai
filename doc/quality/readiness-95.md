# 95 Percent Readiness Gate

This document defines when the project can be considered approximately 95% ready for AI implementation without predictable rework.

It does not mean the software is done. It means the remaining work is mostly mechanical micro-ticket execution rather than uncertain product discovery.

## 1. Scoring Model

Each gate is pass/fail. Do not average partial credit inside a gate.

| Gate | Weight | Pass Condition |
| --- | ---: | --- |
| Project constants | 5 | `PROJECT_SPEC.md` exists and matches README/AGENTS |
| Runtime discovery | 10 | Java, Maven, Node, port, Codex command, opencode command documented |
| Session format discovery | 15 | Codex JSONL and opencode SQLite sources documented from local facts |
| Parser plan | 10 | Parser tickets require real sanitized fixtures and missing-field failures |
| API contract | 10 | API contract and response fixtures exist |
| Runtime command design | 15 | Codex resume and opencode session command shapes documented, static-evidence-gated, and unit-test-gated |
| Security plan | 10 | Token, path guard, no shell string, and secret-leak rules are ticketed |
| Frontend plan | 10 | UI states, API-only data flow, and no-mock audit are ticketed |
| PWA plan | 5 | Manifest, service worker, and `/api/**` cache exclusion are ticketed |
| Agent governance | 10 | AGENTS requires one micro ticket, stop conditions, DoD, and E2E gate |
| Deployment installability | 5 | Secure-origin PWA deployment requirement is documented and audited |

Total: 105. Normalize score to 100 after all gates are evaluated.

## 2. Current Score After This Pass

| Gate | Weight | Status | Notes |
| --- | ---: | --- | --- |
| Project constants | 5 | PASS | `PROJECT_SPEC.md` exists |
| Runtime discovery | 10 | PASS | discovery docs exist |
| Session format discovery | 15 | PASS | Codex JSONL and opencode SQLite confirmed |
| Parser plan | 10 | PASS | parser tickets include fixture and missing-field tests |
| API contract | 10 | PASS | contract and fixture docs exist |
| Runtime command design | 15 | PASS-DOC | command shapes documented; static evidence documents exist; command builder tests still gate implementation |
| Security plan | 10 | PASS | token/path/shell-string rules exist |
| Frontend plan | 10 | PASS | API-only UI and audit tickets exist |
| PWA plan | 5 | PASS | cache exclusion ticket exists |
| Agent governance | 10 | PASS | AGENTS and DoD enforce ticket mode |
| Deployment installability | 5 | PASS-DOC | secure-origin requirement documented; final Android URL not yet verified |

Current estimated readiness: 94-95%.

## 3. What Blocks A 95% Claim

Do not claim 95% until all are true:

1. Codex command builder unit test passes.
2. opencode command builder unit test passes.
3. Backend contract tests are implemented from `backend-response-fixtures.md`.
4. opencode SQLite schema guard test is implemented.
5. Frontend no-mock audit runs after frontend exists.
6. Service worker API cache audit runs after service worker exists.
7. Final Android URL is verified as a secure context.

## 4. Infinite Loop Prediction After This Pass

Most likely remaining loop:

```text
static command evidence conflicts with command builder test
-> AI changes command builder and UI together
-> parser/session selection is blamed
-> frontend adds workaround state
-> backend contract drifts
```

Required prevention:

- Static command evidence failure may modify only discovery/design/runtime command builder tickets.
- UI must not change because command builder evidence is incomplete.
- Parser must not change unless the selected session id is proven invalid.
- Backend API contract must not change unless the response shape is truly wrong.

## 5. Delivery Confidence

With this documentation set, the project is feasible and unlikely to enter an unrecoverable loop if future agents follow `AGENTS.md`.

The remaining non-mechanical uncertainty is no longer live CLI execution behavior. Live CLI gates are forbidden. The remaining gates are implementation/test gates:

- Codex selected-session command builder test.
- opencode selected-session command builder test.
- Final Android installability over the user's chosen external mapping.

After static evidence tickets, command builder tests, and final Android secure-context verification pass, readiness can be treated as about 95%.
