# Runs SSE Live Evidence

Ticket: `EVIDENCE-RUNS-M002`

Audit date: 2026-06-20

Result: FAIL

Follow-up: `BUG-RUN-SSE-M001` was created to fix the observed blocking cause. This document is not yet PASS; `EVIDENCE-RUNS-M002` must be re-run with real events after the fix.

Re-run: `EVIDENCE-RUNS-M003` was executed after `BUG-RUN-SSE-M001`. Result remains FAIL because start now returns quickly, but the SSE subscription received no events.

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
