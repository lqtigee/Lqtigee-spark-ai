# Codex Chat Control CLI Evidence

Date: 2026-06-21

Codex CLI version:

- `codex-cli 0.141.0`

Commands checked:

- `codex exec resume --help`
- `codex resume --help`

## `codex exec resume` option names

- `--config`
- `--last`
- `--all`
- `--enable`
- `--disable`
- `--image`
- `--strict-config`
- `--model`
- `--dangerously-bypass-approvals-and-sandbox`
- `--dangerously-bypass-hook-trust`
- `--skip-git-repo-check`
- `--ephemeral`
- `--ignore-user-config`
- `--ignore-rules`
- `--output-schema`
- `--json`
- `--output-last-message`
- `--help`

## `codex resume` option names

- `--config`
- `--last`
- `--all`
- `--enable`
- `--disable`
- `--include-non-interactive`
- `--remote`
- `--remote-auth-token-env`
- `--strict-config`
- `--image`
- `--model`
- `--oss`
- `--local-provider`
- `--profile`
- `--sandbox`
- `--dangerously-bypass-approvals-and-sandbox`
- `--dangerously-bypass-hook-trust`
- `--cd`
- `--add-dir`
- `--ask-for-approval`
- `--search`
- `--no-alt-screen`
- `--help`
- `--version`

## UI control implications

- Image attachments must map to repeatable `--image`.
- Model selection must map to `--model`.
- Config overrides must map to repeatable `--config`.
- Sandbox selection must map to `--sandbox` for interactive resume.
- Approval policy must map to `--ask-for-approval` for interactive resume.
- Web search toggle must map to `--search` for interactive resume.
- Additional writable directories must map to repeatable `--add-dir` for interactive resume.
- JSON event output is available through `--json` on `codex exec resume`.
- Dangerous bypass flags must not be exposed as normal default controls.
