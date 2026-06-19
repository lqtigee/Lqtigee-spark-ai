# Infinite Loop Risk Prediction

Project: `Lqtigee-spark-ai`  
App name: `Lqtigee`  
Backend port: `20261`

This document predicts whether AI implementation can enter an infinite or self-reinforcing correction loop, identifies the exact risk points, and defines hard stop rules.

## 1. Short Answer

Can this project enter an AI infinite correction loop?

```text
Yes, if parser/runtime/frontend work starts before discovery gates are complete.
```

Will it likely enter an infinite loop if every agent follows `AGENTS.md` and `doc/micro-tickets.md`?

```text
Unlikely.
```

Current predicted risk before completing discovery and adding all gate tickets:

```text
Medium-high
```

Predicted risk after discovery artifacts, CLI syntax evidence, parser samples, and contract tests:

```text
Low to medium-low
```

The project should not be treated as low risk yet.

## 2. What "Infinite Loop" Means Here

An AI loop is not just a failing test. A failing test is good if it points to the correct layer.

An AI implementation loop means:

```text
The AI repeatedly changes code, but each change is based on an earlier wrong assumption, so the project moves farther from the real solution.
```

In this project, the dangerous loop usually has this shape:

```text
unknown local runtime fact
-> guessed code
-> fake success
-> integration failure
-> patch wrong layer
-> still failing
-> patch another wrong layer
-> contract drift
-> hard-to-repair project
```

## 3. Highest Probability Loop

### LOOP-001 Session Parser Loop

Prediction:

```text
This is the most likely infinite-loop source.
```

Trigger:

- Codex or opencode session format is unknown.
- AI writes parser from memory or from file names.
- Parser fills missing required fields using fallback values.

Loop chain:

```text
parser guesses session id/workspace/model
-> /api/sessions returns plausible but wrong session
-> UI enables Control page
-> command starts in wrong workspace or with wrong session id
-> runtime fails
-> AI patches command builder
-> command still fails
-> AI patches UI validation
-> real bug remains parser
```

Probability without guardrails:

```text
High
```

Probability with current micro-ticket gates:

```text
Low-medium
```

Stop rule:

```text
No parser without sanitized real sample.
```

Required correction:

1. Stop parser implementation.
2. Complete discovery sample ticket.
3. Write parser fixture from sanitized real sample.
4. Re-run parser ticket.

## 4. Second Highest Probability Loop

### LOOP-002 Frontend Fake Data Loop

Prediction:

```text
Likely if UI work starts too early.
```

Trigger:

- AI wants to make UI visible before backend endpoint exists.
- AI adds sample sessions or models.

Loop chain:

```text
fake session data
-> UI components depend on fake shape
-> backend returns different shape
-> API client patched
-> UI still expects old fields
-> components patched
-> tests still pass against fake data
-> real API remains broken
```

Probability without guardrails:

```text
High
```

Probability with current micro-ticket gates:

```text
Low
```

Stop rule:

```text
No business UI before real endpoint exists.
```

Required correction:

1. Delete fake data.
2. Show loading/error/empty states only.
3. Resume only after real endpoint ticket passes.

## 5. Third Highest Probability Loop

### LOOP-003 CLI Syntax Loop

Prediction:

```text
Likely if command builders are implemented before CLI syntax discovery.
```

Trigger:

- AI assumes CLI arguments without static CLI/session evidence. For Codex, selected-session control must use `codex exec resume <SESSION_ID>` through the documented argument order; plain `codex exec <prompt>` is the wrong control path.
- CLI returns usage errors.
- Runtime treats stderr as normal output.

Loop chain:

```text
guessed command args
-> process starts but exits non-zero
-> stderr streamed
-> run marked complete
-> UI displays output
-> user says command did not work
-> AI edits SSE/UI
-> command syntax remains wrong
```

Probability without guardrails:

```text
Medium-high
```

Probability with current micro-ticket gates:

```text
Low-medium
```

Stop rule:

```text
No command builder without doc/discovery/cli-command-syntax.md.
```

Required correction:

1. Stop runtime implementation.
2. Capture `--help` output and existing session evidence. Do not execute live CLI commands as gates.
3. Write command builder snapshot test from evidence.
4. Implement command builder.

## 6. Additional Loop Risks

### LOOP-004 Empty Success Loop

Trigger:

- Scanner catches exception and returns `[]`.

Loop:

```text
real scanner failure
-> /api/sessions returns []
-> UI shows empty success
-> user thinks no sessions exist
-> AI debugs frontend empty state
-> scanner failure remains hidden
```

Stop rule:

```text
Scanner/parser failure must return typed error, never successful empty list.
```

Risk:

```text
High if not tested, low if error contract tests exist.
```

### LOOP-005 Partial Adapter Loop

Trigger:

- Codex fails, opencode succeeds.
- Unified endpoint returns only opencode without warning.

Loop:

```text
Codex hidden failure
-> user expects Codex session
-> AI edits UI source filter
-> backend Codex failure remains hidden
```

Stop rule:

```text
Unified /api/sessions fails if any required adapter fails.
Source-specific endpoints may succeed independently.
```

Risk:

```text
Medium.
```

### LOOP-006 SSE Terminal Loop

Trigger:

- Process exits but SSE does not emit terminal event.

Loop:

```text
process finished
-> UI still says running
-> user clicks stop
-> stop fails because process already exited
-> AI edits stop endpoint
-> missing terminal event remains
```

Stop rule:

```text
Every process exit emits exactly one terminal event.
```

Risk:

```text
Medium.
```

### LOOP-007 Broad Refactor Loop

Trigger:

- AI hits a narrow failing test.
- AI rewrites architecture or many unrelated files.

Loop:

```text
small failure
-> broad refactor
-> new failures
-> more broad fixes
-> original failure disappears into noise
```

Stop rule:

```text
One ticket, one behavior, allowed files only.
```

Risk:

```text
Medium-high without AGENTS.md, low-medium with AGENTS.md.
```

### LOOP-008 PWA Cache Loop

Trigger:

- Service worker caches `/api/**`.

Loop:

```text
backend changes/fails
-> PWA serves cached API success
-> UI appears correct
-> real server behavior differs
-> AI debugs backend while phone sees stale data
```

Stop rule:

```text
Service worker must bypass /api/**.
```

Risk:

```text
Medium.
```

## 7. Numeric Prediction Table

| Loop ID | Name | Current risk | Risk after AGENTS + micro tickets + discovery | Main prevention |
| --- | --- | --- | --- | --- |
| LOOP-001 | Session parser loop | High | Low-medium | sanitized real sample gate |
| LOOP-002 | Frontend fake data loop | High | Low | no business UI before API |
| LOOP-003 | CLI syntax loop | Medium-high | Low-medium | CLI syntax evidence |
| LOOP-004 | Empty success loop | High | Low | typed error contract |
| LOOP-005 | Partial adapter loop | Medium | Low-medium | unified endpoint failure rule |
| LOOP-006 | SSE terminal loop | Medium | Low-medium | terminal event tests |
| LOOP-007 | Broad refactor loop | Medium-high | Low-medium | AGENTS allowed-files rule |
| LOOP-008 | PWA cache loop | Medium | Low | API cache exclusion |

## 8. Exact Conditions That Mean "Infinite Loop Is Starting"

If any of the following appears in code or an AI answer, assume loop risk is active:

1. "temporarily mock"
2. "fallback to filename"
3. "return empty list on error"
4. "ignore this adapter failure"
5. "we can infer command syntax"
6. "make UI first and wire later"
7. "catch all and continue"
8. "mark success despite non-zero exit"
9. "cache all fetch requests"
10. "refactor broadly to simplify"
11. "tests use representative fake session"
12. "controller directly scans files"
13. "frontend parses .codex or opencode files"

Required response:

```text
Stop the ticket.
Write BLOCKED or create a new micro ticket.
Do not continue implementation.
```

## 9. Bug Fix Risk Policy

Future bug fixes can also create loops if they are handled as broad repair work.

Every bug fix must start as a micro ticket:

```text
BUG-<area>-<number>
```

Required bug ticket fields:

- Symptom
- Expected behavior
- Actual behavior
- Smallest suspected layer
- Allowed files
- Failing verification
- Fix steps
- Verification command

Bug fix loop prevention:

1. Reproduce first.
2. Add or identify one failing verification.
3. Fix one layer only.
4. Do not rewrite nearby code.
5. Stop after two failed attempts and reassess.

If the second attempt fails:

```text
The AI must stop and write a new risk note before continuing.
```

## 10. Feature Addition Risk Policy

Future features must not be implemented directly.

Required sequence:

1. Add/update requirement.
2. Add architecture impact note.
3. Add risk note if parser/runtime/security/SSE/PWA is touched.
4. Add micro tickets.
5. Implement one ticket at a time.

If a feature request says "just add X quickly", the agent must still create a micro ticket first.

## 11. Final Prediction

Most accurate current prediction:

```text
The project will probably enter a damaging correction loop if an AI starts coding parser, runtime, or business UI before discovery gates are complete.
```

Most accurate prediction under the enforced workflow:

```text
The project is unlikely to enter an infinite loop if agents obey AGENTS.md, complete one micro ticket at a time, and stop on missing real facts.
```

The risk is not zero. The remaining non-zero risk is concentrated in:

- real Codex session format
- real opencode session format
- real command syntax
- SSE process lifecycle

Those are exactly the areas that must remain blocked until evidence exists.
