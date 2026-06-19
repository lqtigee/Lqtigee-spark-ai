# CLI Command Syntax Evidence

Ticket: `P0-M020`

## Codex

Evidence:

```text
codex exec [OPTIONS] [PROMPT]
codex exec resume [OPTIONS] [SESSION_ID] [PROMPT]
```

Relevant options:

```text
-m, --model <MODEL>
-C, --cd <DIR>
--skip-git-repo-check
--json
--dangerously-bypass-approvals-and-sandbox
```

Approved selected-session command shape:

```text
codex -C <workspace> exec resume --json -m <model> --skip-git-repo-check <sessionId> <prompt>
```

Dangerous command extension:

```text
codex -C <workspace> exec resume --json -m <model> --skip-git-repo-check --dangerously-bypass-approvals-and-sandbox <sessionId> <prompt>
```

Dangerous extension must require `confirmDangerous=true`.

Notes:

- Plain `codex exec <prompt>` starts a non-interactive run and is not the selected-session control path.
- `codex exec resume --help` rejects `-C` after `resume`; `-C` must be supplied before `exec`.
- Official manual confirms non-interactive resume with `codex exec resume <SESSION_ID>`.
- Command builder unit tests must verify this exact argument order before enabling backend runtime for Codex.

## opencode

Evidence:

```text
opencode run [message..]
```

Relevant options:

```text
--format default|json
--model <provider/model>
--dir <directory>
--session <session id>
--agent <agent>
--dangerously-skip-permissions
```

Approved initial command shape:

```text
opencode run --format json --model <provider/model> --dir <workspace> --session <sessionId> <message>
```

Dangerous command extension:

```text
opencode run --format json --model <provider/model> --dir <workspace> --session <sessionId> --dangerously-skip-permissions <message>
```

Dangerous extension must require `confirmDangerous=true`.

## Implementation Rule

- All commands must be built as argument arrays.
- Prompt/message must be one argument array item.
- Do not execute through `sh -c`.
