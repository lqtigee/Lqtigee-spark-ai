# Backend Controller Shortcut Audit

Ticket: AUDIT-M001

Scope:

- `src/main/java/com/lqtigee/sparkai/web`

Search command:

```bash
rg -n "Files\.|Path\.|ProcessBuilder|new File" src/main/java/com/lqtigee/sparkai/web
```

Result:

- PASS
- No controller references `Files.`, `Path.`, `ProcessBuilder`, or `new File`.
- Controllers delegate to service/controller-owned DTO response wrapping only.

Blocked items:

- None.
