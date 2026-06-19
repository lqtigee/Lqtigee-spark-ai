# Codex Session Candidate Files

Ticket: `P0-M015`

Command shape:

```bash
find /home/lqtiger/.codex/sessions -type f -printf '%T@ %s %p\n' | sort -nr
```

Most recent candidates:

```text
size=1183042 path=/home/lqtiger/.codex/sessions/2026/06/20/rollout-2026-06-20T00-46-43-019ee090-24e8-7ac1-bd1c-8e4d6788fbf1.jsonl
size=1329753 path=/home/lqtiger/.codex/sessions/2026/06/20/rollout-2026-06-20T00-39-50-019ee089-d81c-75f0-8664-5159e0fee8a6.jsonl
size=2863881 path=/home/lqtiger/.codex/sessions/2026/06/19/rollout-2026-06-19T02-38-36-019edbd0-3874-7702-845f-b21f583ebaa9.jsonl
```

Candidate classification:

```text
Codex current local sessions are JSONL files.
```

Approved first parser target:

```text
/home/lqtiger/.codex/sessions/**/*.jsonl
```

