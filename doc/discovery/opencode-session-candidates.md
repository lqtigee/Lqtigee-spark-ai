# opencode Session Candidate Files

Ticket: `P0-M016`

Most relevant candidates:

```text
size=201256960 path=/home/lqtiger/.local/share/opencode/opencode.db
size=5437874 path=/home/lqtiger/.local/share/opencode/log/opencode.log
size=3571 path=/home/lqtiger/.local/state/opencode/prompt-history.jsonl
size=282 path=/home/lqtiger/.local/state/opencode/model.json
```

Current session source decision:

```text
Use /home/lqtiger/.local/share/opencode/opencode.db for first opencode session discovery implementation.
```

Reason:

- SQLite database has a real `session` table with required fields.
- Log lines contain session ids and model ids but are not sufficient as the primary session source.
- `model.json` contains recent/favorite model preferences, not sessions.
- `prompt-history.jsonl` contains input history, not complete session metadata.

