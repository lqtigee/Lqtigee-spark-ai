# Runs SSE Live Evidence

Ticket: `EVIDENCE-RUNS-M004`

Audit date: 2026-06-20

Result: PASS

`EVIDENCE-RUNS-M004` passed after `BUG-RUN-SSE-M001` and `BUG-RUN-SSE-M002`. Historical failed attempts remain below as evidence of the defects that were fixed.

Follow-up: `BUG-RUN-SSE-M001` was created to fix the observed blocking cause.

Re-run: `EVIDENCE-RUNS-M003` was executed after `BUG-RUN-SSE-M001`. Result remains FAIL because start now returns quickly, but the SSE subscription received no events.

Follow-up: `BUG-RUN-SSE-M002` was created to fix late-subscriber terminal event delivery.

Purpose: prove whether a real run started through the Java API can emit real SSE events and exactly one terminal event.

## Candidate

The candidate came from `doc/audit/run-evidence-candidate.md`, which is marked `PASS`.

- source: `CODEX`
- sessionId: `019ee090-24e8-7ac1-bd1c-8e4d6788fbf1`
- modelId: `gpt-5.5`
- mode: `ASK`
- effective permission: `READ_ONLY` via Codex `-s read-only`

## Prompt

```text
Lqtigee SSE audit: reply with exactly LQTIGEE_SSE_AUDIT_OK and do not modify files.
```

## Request

`POST /api/runs` was called with the real candidate fields and the prompt above.

## Observed Result

- `POST /api/runs` started a real Codex process.
- The process command contained `-s read-only`.
- The HTTP request did not return a usable `runId` within the live audit window.
- Because no `runId` was returned to the client in time, `GET /api/runs/{runId}/events` could not be opened for this run.
- No SSE event stream was captured.
- terminal event: not observed
- terminal count: 0

## Failure Evidence

The Java thread stack showed the request thread waiting inside:

```text
ProcessOutputPump.publishTerminalEvent -> Process.waitFor
RunService.start -> ProcessOutputPump.attach
```

That means the start request is blocked by process waiting instead of returning `StartRunResponse` immediately. The phone UI cannot subscribe to SSE until it receives a `runId`.

After the audit timeout, the started Codex child processes were terminated and the backend was stopped. The backend then attempted to write a `StartRunResponse`, but it was too late for the client-side SSE workflow.

## Safety Notes

- no fake events were created.
- no fake terminal event was recorded.
- no prompt transcript content was copied.
- no full raw API response was copied.
- no dangerous mode was used.
- all audit-started backend and Codex processes were stopped before finishing.

## Post-Fix Re-run Evidence

Ticket: `EVIDENCE-RUNS-M003`

Result: FAIL

Request:

- source: `CODEX`
- sessionId: `019ee090-24e8-7ac1-bd1c-8e4d6788fbf1`
- modelId: `gpt-5.5`
- mode: `ASK`
- effective permission: `READ_ONLY` via Codex `-s read-only`

Prompt:

```text
Lqtigee SSE audit after async pump fix: reply with exactly LQTIGEE_SSE_AUDIT_OK and do not modify files.
```

Observed start result:

- `POST /api/runs` returned HTTP 200.
- `StartRunResponse.runId` returned in 2118 ms.
- runId: `ca3ef55e-d431-4465-bbe8-1c745a7438f6`
- start status: `RUNNING`
- startedAt: `2026-06-20T07:21:39.193053256Z`

Observed SSE result:

- `GET /api/runs/ca3ef55e-d431-4465-bbe8-1c745a7438f6/events` opened an async SSE response.
- The SSE client waited 90 seconds.
- SSE bytes received: 0.
- terminal event: not observed
- terminal count: 0

Cleanup result:

- `POST /api/runs/ca3ef55e-d431-4465-bbe8-1c745a7438f6/stop` returned HTTP 409 with `code=RUN_ALREADY_FINISHED`.
- Backend shutdown reported one active SSE request during graceful shutdown.
- No audit-started backend or Codex processes remained after cleanup.

Current blocker:

The async start fix worked, but real SSE evidence still fails because the terminal event was not delivered to the subscribed client. The next fix must address event delivery for a run that can finish before or around the time the phone subscribes.

## Terminal Replay Re-run Evidence

Ticket: `EVIDENCE-RUNS-M004`

Result: PASS

Request:

- source: `CODEX`
- sessionId: `019ee090-24e8-7ac1-bd1c-8e4d6788fbf1`
- modelId: `gpt-5.5`
- mode: `ASK`
- effective permission: `READ_ONLY` via Codex `-s read-only`

Prompt:

```text
Lqtigee SSE audit after terminal replay fix: reply with exactly LQTIGEE_SSE_AUDIT_OK and do not modify files.
```

Observed start result:

- `POST /api/runs` returned HTTP 200.
- `StartRunResponse.runId` returned in 1582 ms.
- runId: `5140c361-4273-4455-882a-e02429d64820`
- start status: `RUNNING`
- startedAt: `2026-06-20T07:32:25.394310816Z`

Observed SSE result:

- `GET /api/runs/5140c361-4273-4455-882a-e02429d64820/events` opened an async SSE response.
- SSE bytes received: 176.
- real event types: `done`.
- terminal event: `done`
- terminal count: 1
- SSE response completed: yes, curl exited with status 0.
- terminal data: `exitCode=0`

Cleanup result:

- No stop request was required because the run exited successfully.
- Backend shutdown completed cleanly.
- No audit-started backend or Codex processes remained after cleanup.

Safety:

- no fake events were created.
- no fake terminal event was recorded.
- no prompt transcript content was copied.
- no full raw API response was copied.
- no dangerous mode was used.

## Stdout/Stderr Streaming Re-run Evidence

Ticket: `EVIDENCE-RUNS-STDIO-M001`

Audit date: 2026-06-22

Result: PASS

Purpose: prove that a real public run emits process stdout/stderr line events over SSE before the terminal event after `BUG-RUN-SSE-STDIO-M001`.

Public URL:

- `http://118.24.15.133:20261`

Request summary:

- source: `CODEX`
- sessionId: `019eec24-6faf-7060-8ad0-dbf4bf1a55fd`
- modelId: `gpt-5.5`
- mode: `ASK`
- effective permission: `READ_ONLY` via Codex `-s read-only`

Observed start result:

- `POST /api/runs` returned HTTP 200.
- runId: `28126da7-6427-4706-bae6-51acf47da14f`
- start status: `RUNNING`

Observed SSE result:

- `GET /api/runs/28126da7-6427-4706-bae6-51acf47da14f/events` opened an authenticated public SSE response.
- event count: 5
- real event type counts: `stdout=4`, `done=1`
- terminal event: `done`
- terminal count: 1
- terminal data keys included `exitCode`.
- SSE response completed after the terminal event.

Safety:

- no fake events were created.
- no fake terminal event was recorded.
- stdout/stderr message bodies were not copied.
- no prompt text was copied.
- no transcript text was copied.
- no API token was copied.
- no full raw API response was copied.
- no dangerous mode was used.
