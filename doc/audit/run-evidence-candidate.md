# Run Evidence Candidate Audit

Ticket: `EVIDENCE-RUNS-M001`

Audit date: 2026-06-20

Result: PASS

Purpose: select one real existing session candidate for a later live run SSE evidence ticket. This ticket did not start a run.

## Commands

- Confirmed port `20261` was free before backend start.
- Started the backend locally with an audit bearer token.
- Requested `GET /api/sessions`.
- Requested `GET /api/models`.
- Stopped the backend before writing this result.

## API Evidence

- `GET /api/sessions` returned HTTP 200.
- Total real sessions returned: 1152.
- Real Codex sessions returned: 684.
- Real opencode sessions returned: 468.
- `GET /api/models` returned HTTP 200.
- Enabled model entries returned: 2.

## Candidate

- source: `CODEX`
- sessionId: `019ee090-24e8-7ac1-bd1c-8e4d6788fbf1`
- model: `gpt-5.5`
- workspace: `/home/lqtiger`

The candidate was selected from the real `/api/sessions` response because `source`, `id`, `model`, and `workspace` were all present and non-empty. The model `gpt-5.5` was present in the real `/api/models` response and is enabled for `CODEX`.

## Safety Notes

- no fake session was created.
- no fake model was created.
- no prompt, transcript, or message content was copied.
- no full raw API response was copied.
- no run started in this ticket.
- no `POST /api/runs` request was made.
