# opencode Session Static Evidence

Ticket: `STATIC-M002`

Status: `PASS`

This document proves selected-session opencode command construction from existing local evidence. It does not execute opencode.

## 1. Evidence Inputs

- `doc/discovery/opencode-help.md`
- `doc/discovery/cli-command-syntax.md`
- `doc/discovery/opencode-sample-1.md`

## 2. Existing Session Evidence

The existing opencode SQLite sample proves these fields are available from the `session` table:

```text
session id: session.id
workspace: session.directory
model JSON: session.model
raw file: /home/lqtiger/.local/share/opencode/opencode.db
```

No transcript or prompt text is needed for command construction.

## 3. CLI Syntax Evidence

The recorded CLI help proves:

```text
opencode run [message..]
--format default|json
--model <provider/model>
--dir <directory>
--session <session id>
```

## 4. Approved Argument Array Shape

```text
opencode run --format json --model <provider/model> --dir <workspace> --session <sessionId> <prompt>
```

`SHELL` mode is blocked unless `confirmDangerous=true`, then uses the dangerous opencode flag defined in `doc/security/command-permission-matrix.md`.

## 5. Implementation Gate

`OpencodeCommandBuilderTest` must assert:

1. `run` appears after executable.
2. `--format json` exists.
3. `--model <provider/model>` exists.
4. `--dir <workspace>` exists.
5. `--session <sessionId>` exists.
6. Prompt remains one argument array item.
7. No shell string is used.
8. `--continue` is not used for explicit selected sessions.

## 6. Prohibited Action

Do not execute opencode as a live command gate.
