# Chat Control Live Evidence

Ticket: `CHAT-RUN-M006`

Audit date: 2026-06-22

Result: `PASS`

Scope:

- Latest inline chat control code was committed and pushed before this audit.
- Evidence used the local Java service at `http://127.0.0.1:20261`.
- A command-scoped token was read locally and is redacted; no token value is recorded.
- no fake events were added, inferred, or substituted.
- no transcript text and no prompt text are recorded.

## Evidence

- Health endpoint: `PASS`; `serviceName=Lqtigee-spark-ai`, `appName=Lqtigee`, `port=20261`.
- Sessions endpoint: `PASS`; session count `1254`; source breakdown `CODEX=684`, `OPENCODE=570`.
- Candidate session: `PASS`; source `CODEX`; id prefix `019ee090-24e`; model `gpt-5.5`; workspace `/home/lqtiger`; updated time `2026-06-21T15:30:32.794Z`.
- Transcript before run: `PASS`; transcript message count `10`; older messages available `true`.
- Models endpoint: `PASS`; enabled model ids and sources: `gpt-5.5` from `CODEX`, `openai/Lqtigee` from `OPENCODE`.
- Run start: `PASS`; runId `c11beced-97d4-4acf-8186-fb3ac097e9fb`; status `RUNNING`; start elapsed time `988 ms`.
- SSE events: `PASS`; event types `done`; terminal type `done`; terminal count `1`; response completed `true`.
- Stop attempt after the first event window: real result `RUN_ALREADY_FINISHED`; no fabricated stop result was used.
- Transcript after terminal: `PASS`; transcript message count before `10`, after `10`; older messages available after terminal `true`.

## Result Criteria

- `PASS` because health, sessions, candidate transcript, models, run start, SSE terminal, and post-terminal transcript calls all succeeded with real data.
- no fake events
- no transcript text
