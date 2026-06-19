# Lqtigee-spark-ai Project Spec

This file is the immutable project fact source. If any implementation needs to change a fact here, stop and create a specification-change ticket first.

## 1. Constants

- Project name: `Lqtigee-spark-ai`
- App name: `Lqtigee`
- Backend port: `20261`
- Java package root: `com.lqtigee.sparkai`
- Backend runtime: Java 21 + Spring Boot 3
- Frontend runtime: React + TypeScript + Vite
- Mobile delivery: PWA first
- Android installation mode: installable PWA first, optional native wrapper later
- Backend public surface: one HTTP port, `20261`

## 2. Product Goal

Build an Android-installable control console that connects from a phone to the server and controls real local AI coding sessions through one Java backend service.

The product must support:

1. View current Codex CLI sessions.
2. View current opencode sessions.
3. Select one real session.
4. Select one backend-configured model supported by that session source.
5. Send one prompt/command to the selected session.
6. Stream real run events back to the phone.
7. Stop a running process.

## 3. Hard Rules

- No mock data.
- No fake session data.
- No fake model list.
- No silent fallback success.
- No parser fallback to filename for required fields.
- No frontend hardcoded business sessions.
- No frontend hardcoded business models.
- No shell string command execution.
- No service worker caching for `/api/**`.
- No controller direct filesystem scanning.
- No controller direct process launching.

If a real path, command, state format, API shape, or verification result is unknown, the current task must stop with `BLOCKED`.

## 4. Real Local Facts Already Discovered

- Java 21 is available.
- Maven is available.
- Node and npm are available.
- Port `20261` was available at discovery time.
- Codex command: `/home/lqtiger/.npm-global/bin/codex`.
- Codex sessions source: `/home/lqtiger/.codex/sessions/**/*.jsonl`.
- Codex session records are JSONL with top-level `type`, `timestamp`, and `payload`.
- opencode command: `/home/lqtiger/.opencode/bin/opencode`.
- opencode sessions source: `/home/lqtiger/.local/share/opencode/opencode.db`.
- opencode session source table: `session`.
- opencode model field is JSON text such as `{"id":"Lqtigee","providerID":"openai"}`.

## 5. Command Control Facts

Codex selected-session control must use a resume path, not a plain new `codex exec` path.

Approved Codex command family:

```text
codex -C <workspace> exec resume --json -m <model> --skip-git-repo-check <sessionId> <prompt>
```

Approved opencode command family:

```text
opencode run --format json --model <provider/model> --dir <workspace> --session <sessionId> <prompt>
```

Both command families must be verified by static CLI help evidence, existing session evidence, and command builder unit tests before the backend enables `POST /api/runs` for that source.

## 6. Backend API Source Of Truth

The API response shape is defined in:

```text
doc/contracts/backend-api-contract.md
```

The frontend must implement TypeScript types from that contract only. It must not create a competing response shape.

## 7. Implementation Discipline

Every future implementation task must:

1. Select exactly one micro ticket.
2. Touch only allowed files.
3. Run the ticket verification.
4. Stop after two failed attempts.
5. Report changed files and verification result.

No broad repair pass is allowed.
