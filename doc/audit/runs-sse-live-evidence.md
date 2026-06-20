# Runs SSE Live Evidence

Ticket: `EVIDENCE-RUNS-M002`

Audit date: 2026-06-20

Result: FAIL

Follow-up: `BUG-RUN-SSE-M001` was created to fix the observed blocking cause. This document is not yet PASS; `EVIDENCE-RUNS-M002` must be re-run with real events after the fix.

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
