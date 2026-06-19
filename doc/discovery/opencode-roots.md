# opencode Roots Discovery

Ticket: `P0-M014`

Observed roots:

```text
/home/lqtiger/.config/opencode
/home/lqtiger/.local/share/opencode
/home/lqtiger/.local/state/opencode
/home/lqtiger/.opencode
```

Important files:

```text
/home/lqtiger/.config/opencode/opencode.jsonc
/home/lqtiger/.local/share/opencode/opencode.db
/home/lqtiger/.local/share/opencode/log/opencode.log
/home/lqtiger/.local/state/opencode/model.json
/home/lqtiger/.local/state/opencode/prompt-history.jsonl
/home/lqtiger/.opencode/bin/opencode
```

Conclusion:

```text
opencode state exists. Current real session data is stored primarily in SQLite at /home/lqtiger/.local/share/opencode/opencode.db.
```

Parser warning:

- `.config/opencode/opencode.jsonc` is configuration, not session data.
- `prompt-history.jsonl` is prompt input history, not enough for session discovery.
- `opencode.db` is the correct first source for real sessions.

