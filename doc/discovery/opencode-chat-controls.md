# opencode Chat Control CLI Evidence

Date: 2026-06-21

opencode version:

- `1.17.8`

Commands checked:

- `opencode run --help`
- `opencode session --help`
- `opencode agent --help`

## `opencode run` option names

- `--help`
- `--version`
- `--print-logs`
- `--log-level`
- `--pure`
- `--command`
- `--continue`
- `--session`
- `--fork`
- `--share`
- `--model`
- `--agent`
- `--format`
- `--file`
- `--title`
- `--attach`
- `--password`
- `--username`
- `--dir`
- `--port`
- `--variant`
- `--thinking`
- `--replay`
- `--replay-limit`
- `--interactive`
- `--dangerously-skip-permissions`
- `--demo`

## `opencode session` commands and option names

- `session list`
- `session delete`
- `--help`
- `--version`
- `--print-logs`
- `--log-level`
- `--pure`

## `opencode agent` commands and option names

- `agent create`
- `agent list`
- `--help`
- `--version`
- `--print-logs`
- `--log-level`
- `--pure`

## UI control implications

- Resume must use `--session` for a selected known session id.
- Fork control can map to `--fork` only with a continued session.
- File attachments must map to repeatable `--file` after server-side attachment id resolution.
- Agent selection can map to `--agent`.
- Model selection maps to `--model`.
- JSON streaming maps to `--format json`.
- Variant can map to `--variant`.
- Thinking display can map to `--thinking`.
- Replay limit can map to `--replay-limit`.
- Share can map to `--share`.
- Dangerous permission skip must not be exposed as a normal default control.
