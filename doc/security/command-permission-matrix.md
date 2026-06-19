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

## 4. Test Requirements

Command builder tests must cover:

1. `ASK` safe args.
2. `REVIEW` safe args.
3. `EDIT` safe args.
4. `SHELL` without confirmation fails.
5. `SHELL` with confirmation adds only the documented dangerous flag.
6. Prompt remains one argument array item.
7. No command uses `sh -c`.
