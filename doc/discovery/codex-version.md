# Codex Version Discovery

Ticket: `P0-M008`

Command:

```bash
codex --version
```

Observed:

```text
codex-cli 0.141.0
```

Additional observation from latest local session metadata:

```text
payload.cli_version = 0.142.0-alpha.1
```

Conclusion:

```text
The installed `codex` executable reports 0.141.0. Some persisted sessions were written by a 0.142.0-alpha.1 Codex runtime.
```

Risk:

- Parsers must not assume all local session files were written by the exact installed CLI version.

