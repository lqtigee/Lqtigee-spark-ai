# Codex Help Discovery

Ticket: `P0-M009`

Commands:

```bash
codex --help
codex exec --help
codex exec resume --help
```

Observed command support:

```text
codex exec [OPTIONS] [PROMPT]
codex exec [OPTIONS] <COMMAND> [ARGS]
codex exec resume [OPTIONS] [SESSION_ID] [PROMPT]
```

Relevant options:

```text
-m, --model <MODEL>
-C, --cd <DIR>
-s, --sandbox <SANDBOX_MODE>
--dangerously-bypass-approvals-and-sandbox
--skip-git-repo-check
--json
-o, --output-last-message <FILE>
```

Noninteractive command evidence:

```text
`codex exec` is explicitly documented as "Run Codex non-interactively".
```

Confirmed selected-session command-builder shape for future ticket:

```text
codex -C <workspace> exec resume --json -m <model> --skip-git-repo-check <sessionId> <prompt>
```

Important ordering:

- `codex exec resume -C <workspace>` is rejected by the local CLI.
- `codex exec resume --cd <workspace>` is rejected by the local CLI.
- Therefore `-C <workspace>` must appear before `exec`.
- Plain `codex exec <prompt>` starts a non-interactive run and must not be used for selected-session control.

Dangerous mode note:

- `--dangerously-bypass-approvals-and-sandbox` exists but must be gated by `confirmDangerous`.
- Safer default should use `--sandbox workspace-write` or configured policy, not danger mode.

Risk:

- Command builder must use argument arrays. Do not shell-concatenate the prompt.
- Runtime remains blocked until static CLI/session evidence exists and `CodexCommandBuilderTest` proves the selected-session resume argument array.
