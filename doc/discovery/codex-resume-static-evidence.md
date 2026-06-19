# Codex Resume Static Evidence

Ticket: `STATIC-M001`

Status: `PASS`

This document proves selected-session Codex command construction from existing local evidence. It does not execute Codex.

## 1. Evidence Inputs

- `doc/discovery/codex-help.md`
- `doc/discovery/cli-command-syntax.md`
- `doc/discovery/codex-sample-1.md`

## 2. Existing Session Evidence

The existing Codex sample proves these fields are available from session JSONL:

```text
session id: session_meta.payload.id
workspace: session_meta.payload.cwd or turn_context.payload.cwd
model: turn_context.payload.model
raw file: /home/lqtiger/.codex/sessions/**/*.jsonl
```

No transcript or prompt text is needed for command construction.

## 3. CLI Syntax Evidence

The recorded CLI help proves:

```text
codex exec resume [OPTIONS] [SESSION_ID] [PROMPT]
```

Required options are documented:

```text
--json
-m, --model <MODEL>
-C, --cd <DIR>
--skip-git-repo-check
```

Local help verification also recorded that `-C` after `resume` is rejected, so `-C <workspace>` must appear before `exec`.

## 4. Approved Argument Array Shape

```text
codex -C <workspace> -s read-only exec resume --json -m <model> --skip-git-repo-check <sessionId> <prompt>
```

`EDIT` mode replaces `-s read-only` with `-s workspace-write`.

`SHELL` mode is blocked unless `confirmDangerous=true`, then uses the dangerous Codex flag defined in `doc/security/command-permission-matrix.md`.

## 5. Implementation Gate

`CodexCommandBuilderTest` must assert:

1. `-C` appears before `exec`.
2. `exec` is followed by `resume`.
3. `--json` exists.
4. `-m <model>` exists.
5. `<sessionId>` appears before `<prompt>`.
6. Prompt remains one argument array item.
7. No shell string is used.

## 6. Prohibited Action

Do not execute Codex as a live command gate.
