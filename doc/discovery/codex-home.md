# Codex Home Discovery

Ticket: `P0-M010`

Root:

```text
/home/lqtiger/.codex
```

Observed important paths:

```text
/home/lqtiger/.codex/sessions
/home/lqtiger/.codex/sessions/2026
/home/lqtiger/.codex/session_index.jsonl
/home/lqtiger/.codex/history.jsonl
/home/lqtiger/.codex/config.toml
/home/lqtiger/.codex/model-catalog.local.json
/home/lqtiger/.codex/logs_2.sqlite
/home/lqtiger/.codex/state_5.sqlite
/home/lqtiger/.codex/goals_1.sqlite
```

Conclusion:

```text
Codex home exists and contains JSONL session files under /home/lqtiger/.codex/sessions.
```

Parser warning:

- `config.toml`, `env.local`, SQLite files, and shell snapshots are not session files for first parser implementation.
- First parser should target session JSONL files under `/home/lqtiger/.codex/sessions/**`.

