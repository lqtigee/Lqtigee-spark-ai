# Command Permission Matrix

This matrix defines how `StartRunRequest.mode` maps to CLI permissions. It is mandatory for command builder implementation.

## 1. Modes

| Mode | Purpose | Default Permission | Dangerous Flag Allowed |
| --- | --- | --- | --- |
| `ASK` | Ask for analysis or explanation | read-only | no |
| `REVIEW` | Review code or state | read-only | no |
| `EDIT` | Allow repository edits | workspace-write | no |
| `SHELL` | Allow broad command execution | explicit dangerous confirmation required | yes |

## 2. Codex Mapping

| Mode | Required args |
| --- | --- |
| `ASK` | `-s read-only` |
| `REVIEW` | `-s read-only` |
| `EDIT` | `-s workspace-write` |
| `SHELL` | `--dangerously-bypass-approvals-and-sandbox` only when `confirmDangerous=true` |

Codex selected-session base command:

```text
codex -C <workspace> <permission-args> exec resume --json -m <model> --skip-git-repo-check <sessionId> <prompt>
```

Rules:

- Do not use danger mode for `ASK`, `REVIEW`, or `EDIT`.
- Do not omit sandbox mode for `ASK`, `REVIEW`, or `EDIT`.
- Do not enable `SHELL` unless the request has `confirmDangerous=true`.
- If `SHELL` is requested without confirmation, return `DANGER_CONFIRM_REQUIRED`.
- Codex `--image` attachments may use only server-owned attachment ids resolved to safe paths inside the configured Lqtigee temp directory.
- Codex attachment paths must never come directly from frontend request strings.

## 3. opencode Mapping

| Mode | Required args |
| --- | --- |
| `ASK` | no dangerous flag |
| `REVIEW` | no dangerous flag |
| `EDIT` | no dangerous flag |
| `SHELL` | `--dangerously-skip-permissions` only when `confirmDangerous=true` |

opencode selected-session base command:

```text
opencode run --format json --model <model> --dir <workspace> --session <sessionId> <prompt>
```

Rules:

- Do not add `--dangerously-skip-permissions` for `ASK`, `REVIEW`, or `EDIT`.
- If `SHELL` is requested without confirmation, return `DANGER_CONFIRM_REQUIRED`.
- Do not add `--continue`; selected session must use explicit `--session`.
- opencode `--file` attachments may use only server-owned attachment ids resolved to safe paths inside the configured Lqtigee temp directory.
- opencode attachment paths must never come directly from frontend request strings.

## 4. Test Requirements

Command builder tests must cover:

1. `ASK` safe args.
2. `REVIEW` safe args.
3. `EDIT` safe args.
4. `SHELL` without confirmation fails.
5. `SHELL` with confirmation adds only the documented dangerous flag.
6. Prompt remains one argument array item.
7. No command uses `sh -c`.

## 5. Source-Scoped Session Actions

Session actions use a separate source-scoped request contract:

```text
POST /api/sessions/{source}/{id}/actions
```

Rules:

- The URL source is authoritative and must be `CODEX` or `OPENCODE`.
- The URL id is the selected real session id for that source.
- Action builders must build argument arrays only.
- Action builders must not use `sh -c`, `bash -c`, or any shell string.
- Action builders must not accept raw filesystem paths from the frontend.
- Runtime capabilities must expose a session action only after the matching command builder and service tests pass.

## 5.1 Codex Session Action Mapping

| Action | Required args | Destructive | Confirmation |
| --- | --- | --- | --- |
| `archive` | `codex archive <sessionId>` | no | not required |
| `unarchive` | `codex unarchive <sessionId>` | no | not required |
| `delete` | `codex delete <sessionId>` | yes | `confirmDestructive=true` |
| `fork` | `codex fork <sessionId>` | no | not required |

Rules:

- `delete` must return `DANGER_CONFIRM_REQUIRED` unless `confirmDestructive=true`.
- Blank session ids must return `VALIDATION_FAILED`.
- `fork` must not be enabled unless local help evidence and command builder tests prove the CLI shape.

## 5.2 opencode Session Action Mapping

| Action | Required args | Destructive | Confirmation |
| --- | --- | --- | --- |
| `delete` | `opencode session delete <sessionId>` | yes | `confirmDestructive=true` |
| `export` | `opencode export <sessionId>` | no | not required |
| `import` | `opencode import <source>` | yes | `confirmDestructive=true` |
| `fork` | source-specific verified command only | no | not required |

Rules:

- `delete` and `import` must return `DANGER_CONFIRM_REQUIRED` unless `confirmDestructive=true`.
- Blank session ids must return `VALIDATION_FAILED`.
- `export` output must flow only through authenticated endpoints and must not be written into docs.
- `fork` must stay disabled until a verified opencode session fork command exists.
