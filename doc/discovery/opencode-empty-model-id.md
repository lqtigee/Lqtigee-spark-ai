# Opencode Empty Model Id Discovery

Ticket: `BUG-OPENCODE-SESSION-MODEL-001`

Audit date: 2026-06-20

Result: `BLOCKED`

This discovery explains why live `/api/opencode/sessions` currently returns `OPENCODE_SESSION_FIELD_MISSING` with `detail=session.model.id`.

## Scope

- Database: `/home/lqtiger/.local/share/opencode/opencode.db`
- Tables inspected: `session`, `message`, `event`, `session_message`
- Data copied here: schema-level counts and model metadata only
- Data intentionally not copied: prompts, message text, part text, transcript contents, secrets, tokens, credentials

## Session Row Summary

Command shape:

```bash
sqlite3 ~/.local/share/opencode/opencode.db \
  "SELECT COUNT(*) AS total_rows, ... FROM session;"
```

Observed counts:

| Total session rows | Rows with non-empty `session.model.id` | Rows with empty `session.model.id` |
| --- | --- | --- |
| 468 | 464 | 4 |

The four empty-id rows are non-archived.

## Empty Model Patterns

Observed `session.model` patterns:

| `session.model` JSON | Count |
| --- | --- |
| `{"id":"","providerID":"Lqtigee","variant":"default"}` | 3 |
| `{"id":"","providerID":"gpt-5.5","variant":"high"}` | 1 |

All four rows were created by opencode `1.17.7` with `agent=build`.

## Metadata Cross-Check

The same sessions were checked in metadata-bearing tables without copying prompt or transcript text.

| Table | Rows checked | Non-empty `model.id` | Non-empty `model.modelID` | Non-empty `model.providerID` |
| --- | --- | --- | --- | --- |
| `message` | 5 | 0 | 0 | 5 |
| `event` | 29 | 0 | 0 | 4 |
| `session_message` | 8 | 0 | 0 | 4 |

Conclusion:

- No inspected metadata table contains a recoverable non-empty `model.id` or `model.modelID` for these four sessions.
- `providerID` alone is present in some metadata, but providerID alone is not a model id under the current backend contract.
- Using `providerID` alone would be a fallback guess and is not allowed.

## Current Contract Impact

- `RemoteSessionDto.model` is required.
- `/api/sessions` must not return partial success when one source fails.
- Parser failure must not become empty success.
- Therefore the current typed failure is correct until a follow-up ticket changes the contract or defines a non-fallback handling rule.

## Follow-Up Required

A follow-up implementation ticket must choose exactly one behavior before any code changes:

- Keep typed failure for opencode rows whose model id cannot be recovered.
- Or update the backend API contract to allow excluding non-runnable opencode sessions from list results, with explicit user-facing evidence that those rows are not commandable.

Any follow-up must not fallback to provider name, title, workspace, filename, or configured default model.
