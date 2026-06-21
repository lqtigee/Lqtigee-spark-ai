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

## Public Inline SSE Verification

Ticket: `MOBILE-PUBLIC-M003`

Audit date: 2026-06-22

Result: `PASS` for public-route run start, SSE observation, and terminal event count.

Scope:

- Public URL: `http://118.24.15.133:20261`.
- Public route mapping: public server `20261` to public server `127.0.0.1:20262` to local Java service `127.0.0.1:20261`.
- Runtime data source: current local Codex/opencode sessions.
- Persistence precondition: local PostgreSQL container was started on `127.0.0.1:5432`, project schema was imported, and the Java service was restarted with `lqtigee.database.enabled=true`.
- API token: used for authenticated public API calls, not recorded here.
- Prompt text: not recorded here.
- Transcript text: not recorded here.

Evidence:

- Public health endpoint: `PASS`; `serviceName=Lqtigee-spark-ai`, `appName=Lqtigee`, `port=20261`.
- Public sessions endpoint: `PASS`; session count `1302`; source breakdown `CODEX=684`, `OPENCODE=618`.
- Public models endpoint: `PASS`; enabled model ids and sources included `gpt-5.5` from `CODEX` and `openai/Lqtigee` from `OPENCODE`.
- Candidate session: `PASS`; source `CODEX`; selected from current local sessions.
- Run start through public `POST /api/runs`: `PASS`; runId `5e373f51-e81d-4f92-aeb7-4887000c7fbe`; start status `RUNNING`.
- SSE subscription through public `GET /api/runs/{runId}/events`: `PASS`.
- Stop through public `POST /api/runs/{runId}/stop`: `PASS`; stop response status `STOPPED`.
- SSE event types: `stopped`.
- SSE terminal type: `stopped`.
- SSE terminal count: `1`.
- SSE byte count observed by the verification script: `155`.
- Verification elapsed time: `181452 ms`.
- Real process cleanup: `PASS`; no child Codex process remained after stop.
- No fake events were added, inferred, or substituted.
- no prompt text
- no transcript text

Observed follow-up risk from this older run:

- The SSE client received exactly one terminal event, `stopped`, which satisfies this ticket.
- The PostgreSQL `run_records` row later showed status `EXITED`, and the Java service logged `RUN_ALREADY_FINISHED` from `ProcessOutputPump` after the stop path completed.
- This was the pre-fix behavior that became `BUG-RUN-STOP-RACE-M001`.

## Stopped Run PostgreSQL Reverification

Ticket: `EVIDENCE-RUN-STOP-PG-M001`

Audit date: 2026-06-22

Result: `PASS`

Scope:

- Public URL: `http://118.24.15.133:20261`.
- Public route mapping: public server to local Java service on port `20261`.
- Runtime data source: current local Codex session.
- PostgreSQL container: `lqtigee-spark-ai-postgres` on `127.0.0.1:5432`.
- API token: used for authenticated public API calls, not recorded here.
- Prompt text: not recorded here.
- Transcript text: not recorded here.
- stdout/stderr content: not recorded here.

Evidence:

- Run start through public `POST /api/runs`: `PASS`; runId `20c9622e-873b-4acb-988d-5ce0cd0c50ae`.
- Stop through public `POST /api/runs/{runId}/stop`: `PASS`; stop response status `STOPPED`.
- SSE subscription through public `GET /api/runs/{runId}/events`: `PASS`.
- SSE event types: `stopped`.
- SSE terminal type: `stopped`.
- SSE terminal count: `1`.
- SSE byte count observed by the verification script: `155`.
- PostgreSQL status for `run_records.run_id=20c9622e-873b-4acb-988d-5ce0cd0c50ae`: `STOPPED`.
- Service log check for this run and the previous race signature: `PASS`; no `RUN_ALREADY_FINISHED` entry was found in the recent service log window.
- Real process cleanup: `PASS`; no child process for this run remained after stop.
- No fake events were added, inferred, or substituted.
- no prompt text
- no transcript text

Result criteria:

- `PASS` because the current code produced exactly one real `stopped` SSE terminal event and PostgreSQL preserved `STOPPED` for the same run id.
- The old `MOBILE-PUBLIC-M003` stopped-run persistence race is reverified fixed for the current code.
