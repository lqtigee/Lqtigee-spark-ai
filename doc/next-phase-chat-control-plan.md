# Next-Phase Session Chat Control Plan

Ticket: `PLAN-CHAT-CONTROL-M001`

Project: `Lqtigee-spark-ai`  
App: `Lqtigee`  
Backend port: `20261`  
Public browser URL currently used for evidence: `http://118.24.15.133:20261`

## 1. Goal

Turn the current `/sessions` screen into the phone-first control surface:

1. The phone sees real local Codex CLI and opencode sessions.
2. Each session has a real title and opens as a real chat transcript.
3. The selected chat can continue through the existing Java backend run API.
4. The run stream is shown inline in the chat panel.
5. A terminal run refreshes the transcript from the real backend.
6. The public server remains only a port mapping layer to this local machine.

This plan is intentionally decomposed. A future AI agent must complete exactly one ticket at a time and must not combine UI polish, runtime behavior, parser behavior, and deployment evidence in one pass.

## 2. Non-Negotiable Rules

- No mock data.
- No fake sessions.
- No fake models.
- No fake chat messages.
- No generated transcript summaries.
- No fake SSE events.
- No frontend fallback success arrays.
- No backend fallback to empty success on scanner/parser/runtime failure.
- No public-server session discovery.
- No direct frontend file reads.
- No new runtime command syntax.
- No shell string execution.
- No application code change in planning-only tickets.

If a real endpoint fails, show or record the real failure. Do not convert the failure into an empty UI success state.

## 3. Source Of Truth

The source of truth for phone UI data is the Java service listening on backend port `20261` on the current local machine.

The public server is only an access mapping layer:

```text
Phone browser
  -> http://118.24.15.133:20261
  -> public mapping
  -> local Java service on this machine
  -> local Codex JSONL and local opencode SQLite
```

The public server must not scan Codex sessions, opencode sessions, models, transcripts, or run state.

## 4. PostgreSQL Boundary

PostgreSQL is the database for Lqtigee-owned persistent data only:

- future run records
- future audit events
- future settings
- future user preferences

PostgreSQL must not replace live external session discovery in this phase:

- Codex sessions still come from `/home/lqtiger/.codex/sessions/**/*.jsonl`.
- opencode sessions still come from `/home/lqtiger/.local/share/opencode/opencode.db`.
- Transcript messages still come from the selected real Codex JSONL file or selected real opencode SQLite rows.

Do not create a PostgreSQL table that returns sessions as if they were current Codex/opencode sessions unless a later dedicated ticket defines schema, invalidation, and real-source verification.

## 5. Existing Backend Contract To Reuse

No new backend endpoint is required for the first inline chat-control implementation.

Existing endpoints:

```text
GET  /api/sessions
GET  /api/sessions/{source}/{id}/transcript
GET  /api/models
POST /api/runs
GET  /api/runs/{runId}/events
POST /api/runs/{runId}/stop
```

Existing frontend API functions:

```text
frontend/src/api/remoteApi.ts
  listSessions()
  getSessionTranscript(source, id)
  listModels()
  startRun(request)
  openRunEvents(runId, handlers)
  stopRun(runId)
```

Existing backend classes:

```text
src/main/java/com/lqtigee/sparkai/web/SessionController.java
src/main/java/com/lqtigee/sparkai/web/RunController.java
src/main/java/com/lqtigee/sparkai/service/SessionTranscriptService.java
src/main/java/com/lqtigee/sparkai/service/RunService.java
src/main/java/com/lqtigee/sparkai/runtime/RunEventBus.java
```

The chat-control UI must call `startRun`, subscribe through `openRunEvents`, and stop through `stopRun`. It must not create a second frontend runtime protocol.

## 6. Existing Frontend Code Map

Current files to build on:

```text
frontend/src/pages/SessionsPage.tsx
  loads real sessions
  filters real sessions
  selects one session
  loads real transcript through useSessionTranscriptState

frontend/src/components/SessionDetail.tsx
  renders selected session metadata
  renders transcript.messages as chat

frontend/src/state/useSessionsState.ts
  loads /api/sessions
  stores selected session id

frontend/src/state/useSessionTranscriptState.ts
  loads /api/sessions/{source}/{id}/transcript

frontend/src/state/useModelsState.ts
  loads /api/models

frontend/src/components/PromptComposer.tsx
  owns mode, prompt, dangerous confirmation UI through props

frontend/src/components/ModelSelect.tsx
  filters configured backend models by session source

frontend/src/components/RunTimeline.tsx
  renders real RunEventDto entries

frontend/src/pages/RunsPage.tsx
  already proves the run stream can be displayed from openRunEvents
```

Do not duplicate these concepts with alternate names unless a ticket explicitly says to extract a shared component or hook.

## 7. Target Phone UI

Primary screen:

```text
/sessions
```

Mobile layout:

1. Header with page title and reload action.
2. Source filter segmented control: All, Codex, opencode.
3. Search field.
4. Session list with source, title, model, workspace, updated time, and last visible message preview.
5. Selecting a session opens a chat view for that session.
6. Chat header shows title, source, model, workspace, updated time, and back action.
7. Chat message list shows only real transcript messages returned by the transcript endpoint.
8. Composer area belongs to the selected chat, not to a separate fake control page.
9. Composer includes model select, mode selector, prompt textarea, dangerous confirmation when mode is `SHELL`, run button, stop button when a run is active, and inline run event timeline.
10. When a terminal event arrives, the UI reloads the transcript from the real transcript endpoint.

Desktop layout:

1. Session list remains on the left.
2. Chat panel remains on the right.
3. Composer remains inside the selected chat panel.
4. No nested cards inside cards.

Visual constraints:

- Must fit 320px width without horizontal overflow.
- Keep repeated cards at 8px border radius or less.
- Do not use oversized marketing hero layout.
- Do not add visible explanatory text about how the app works.
- Do not use one-hue decoration.
- Do not let long titles, paths, model names, or run event text overflow their containers.

## 8. Exact Runtime Flow

### 8.1 Open Sessions

Function-level flow:

1. `SessionsPage` checks `localStorage.getItem("lqtigee_token")`.
2. If token is missing, it does not call protected endpoints.
3. If token exists, `useSessionsState.loadSessions()` calls `listSessions()`.
4. `listSessions()` calls `GET /api/sessions`.
5. The backend returns real sessions or a typed error.
6. `SessionsPage` displays loading, error, empty, or success states distinctly.

### 8.2 Select Session

Planned change:

Selection must be stored as source plus id, not id only.

Reason:

`CODEX` and `OPENCODE` can theoretically have the same textual session id. The UI must not accidentally open or run the wrong source.

Required shape:

```ts
interface SelectedSessionRef {
  source: AgentSource;
  id: string;
}
```

Storage keys:

```text
lqtigee_selected_session_source
lqtigee_selected_session_id
```

URL query:

```text
/sessions?source=CODEX&sessionId=<encoded-id>
/sessions?source=OPENCODE&sessionId=<encoded-id>
```

### 8.3 Load Chat Transcript

Function-level flow:

1. `SessionsPage` finds selected session by `{ source, id }`.
2. `useSessionTranscriptState.loadTranscript(selected.source, selected.id)` runs.
3. `getSessionTranscript(source, id)` calls `GET /api/sessions/{source}/{id}/transcript`.
4. `SessionDetail` renders `transcript.messages`.
5. Empty transcript is shown only when the endpoint succeeds and returns an empty `messages` array.
6. API failure renders `ErrorPanel`.

No frontend code may append the submitted prompt as a fake chat message.

### 8.4 Load Models For Chat

Function-level flow:

1. `SessionsPage` loads models through `useModelsState.loadModels()` when token exists.
2. `SessionChatComposer` filters models by `model.enabled && model.sources.includes(session.source)`.
3. The selected model id is cleared if it no longer belongs to the selected session source.
4. Missing or failed models render a real error or disabled command form.

Do not hardcode models in frontend.

### 8.5 Start Run From Chat

Function-level flow:

1. `SessionChatComposer` validates selected session, selected model, non-empty prompt, source support, and dangerous confirmation.
2. It builds a `StartRunRequest` using the selected real session:

```ts
{
  sessionId: session.id,
  source: session.source,
  modelId,
  mode,
  prompt,
  confirmDangerous
}
```

3. It calls `useSessionChatRunState.startSessionRun(request)`.
4. The hook calls `startRun(request)`.
5. `startRun()` calls `POST /api/runs`.
6. Backend `RunService.start()` validates the real session and real model.
7. Backend command builders produce existing approved selected-session commands.
8. The hook stores the returned real `runId`.
9. The hook opens `openRunEvents(runId, handlers)`.
10. The UI renders only real `RunEventDto` entries from the SSE stream.

Do not redirect to `/runs` for chat-started runs.

### 8.6 Stop Run From Chat

Function-level flow:

1. Stop button is enabled only when a real `runId` exists and no terminal event has arrived.
2. `useSessionChatRunState.stopActiveRun()` calls `stopRun(runId)`.
3. Backend `RunService.stop(runId)` stops the real process and emits a real `stopped` event.
4. UI renders the real stopped event through `RunTimeline`.

Do not synthesize stopped events on the frontend.

### 8.7 Refresh Transcript After Terminal Event

Function-level flow:

1. `openRunEvents` receives a real event.
2. `useSessionChatRunState` checks event type.
3. Terminal types are exactly:

```text
done
error
stopped
```

4. When a terminal event arrives, `SessionsPage` compares the run's captured `{ source, id }` with the currently selected session.
5. If it still matches, `useSessionTranscriptState.loadTranscript(source, id)` reloads the real transcript.
6. `SessionsPage` may also call `useSessionsState.loadSessions()` to refresh title/preview/updated time.

Do not insert run output into `transcript.messages`.

## 9. Micro Ticket Queue

Implementation must follow this order:

1. `CHAT-UX-M001 Persist Selected Session As Source/ID Tuple`
2. `CHAT-UX-M002 Sync Selected Chat With URL Query`
3. `CHAT-UX-M003 Make Session Chat Panel Mobile-First Control Surface`
4. `CHAT-RUN-M001 Add Session Chat Run State Hook`
5. `CHAT-RUN-M002 Add Session Chat Composer Component`
6. `CHAT-RUN-M003 Mount Real Chat Composer In Session Detail`
7. `CHAT-RUN-M004 Refresh Real Transcript After Terminal Chat Run`
8. `CHAT-RUN-M005 Guard Inline Chat Run Against Double Start And Stale Session`
9. `CHAT-RUN-M006 Capture Real Inline Chat Control Evidence`
10. `PUBLIC-ACCESS-M006 Rebuild Public Entry With Inline Chat Control`

The safe next ticket after this plan is:

```text
CHAT-UX-M001
```

## 10. Function-Level Implementation Index

This index is binding guidance for future agents. If a function name listed here conflicts with real code at implementation time, the agent must update the relevant micro ticket first instead of improvising a wider refactor.

### 10.1 `CHAT-UX-M001`

Files:

```text
frontend/src/types/api.ts
frontend/src/state/useSessionsState.ts
frontend/src/pages/SessionsPage.tsx
frontend/src/pages/ControlPage.tsx
```

Required function/type changes:

```text
types/api.ts
  export interface SelectedSessionRef
    Purpose: represent a selected session by source plus id.
    Inputs: none.
    Output fields: source, id.
    Forbidden: rawFile, title, workspace, prompt, transcript data.

useSessionsState.ts
  readPersistedSelectedSessionRef(): SelectedSessionRef | null
    Purpose: read localStorage source/id.
    Return null when either key is missing, blank, or invalid.
    Must not create a default source.

  persistSelectedSessionRef(ref: SelectedSessionRef | null): void
    Purpose: write or clear both localStorage keys.
    Must update source and id together.

  sessionMatchesRef(session: RemoteSession, ref: SelectedSessionRef): boolean
    Purpose: compare both source and id.
    Must not compare id only.

  useSessionsState(): SessionsState
    Purpose: expose selectedSessionRef, loadSessions, selectSession, clearSelectedSession.
    loadSessions must clear stale source/id only after real listSessions succeeds.
    selectSession must accept RemoteSession | null.

SessionsPage.tsx
  findSelectedSession(sessions: RemoteSession[], ref: SelectedSessionRef | null): RemoteSession | undefined
    Purpose: find selected real session by source/id.

ControlPage.tsx
  findSelectedSession(sessions: RemoteSession[], ref: SelectedSessionRef | null): RemoteSession | undefined
    Purpose: keep Control compatible with the same selection identity.
```

### 10.2 `CHAT-UX-M002`

Files:

```text
frontend/src/pages/SessionsPage.tsx
```

Required functions:

```text
readSelectedSessionQuery(search: string): SelectedSessionRef | null
  Purpose: parse source/sessionId from current URL query.
  Must accept only CODEX or OPENCODE.
  Must return null for invalid or blank values.

writeSelectedSessionQuery(ref: SelectedSessionRef | null): void
  Purpose: update browser URL without page reload.
  Must use window.history.replaceState.
  Must remove source/sessionId when ref is null.

selectSessionFromQueryIfLoaded(queryRef: SelectedSessionRef | null, sessions: RemoteSession[]): RemoteSession | null
  Purpose: convert URL query into a real loaded session.
  Must return null unless the loaded real session list contains the exact source/id.
```

### 10.3 `CHAT-RUN-M001`

Files:

```text
frontend/src/state/useSessionChatRunState.ts
```

Required functions/types:

```text
interface SessionChatRunState
  Purpose: expose runId, events, error, starting, stopping, terminal, startSessionRun, stopActiveRun, clearRun.

useSessionChatRunState(): SessionChatRunState
  Purpose: own exactly one active inline chat run stream.
  Must close existing stream on unmount.

startSessionRun(request: StartRunRequest, onTerminal?: (event: RunEventDto) => void): Promise<void>
  Purpose: call real POST /api/runs and subscribe to real SSE.
  Must store only the returned runId.
  Must append only events received from openRunEvents.

stopActiveRun(): Promise<void>
  Purpose: call real POST /api/runs/{runId}/stop.
  Must no-op or set error when no real runId exists; must not synthesize stopped events.

clearRun(): void
  Purpose: close stream and clear local run state.
```

### 10.4 `CHAT-RUN-M002`

Files:

```text
frontend/src/components/SessionChatComposer.tsx
```

Required functions:

```text
SessionChatComposer(props: SessionChatComposerProps): JSX.Element
  Purpose: render model select, PromptComposer, stop button, errors, and RunTimeline for one selected real session.
  Must not call API functions directly.

validateSessionChatForm(input: ValidationInput): string[]
  Purpose: return user-visible validation errors.
  Must check selected model, source support, prompt, and SHELL confirmation.

buildStartRunRequest(session: RemoteSession, modelId: string, mode: CommandMode, prompt: string, confirmDangerous: boolean): StartRunRequest
  Purpose: convert real selected session and form state into StartRunRequest.
  Must use session.source and session.id.
```

### 10.5 `CHAT-RUN-M003`

Files:

```text
frontend/src/pages/SessionsPage.tsx
frontend/src/components/SessionDetail.tsx
frontend/src/styles/global.css
```

Required changes:

```text
SessionsPage.tsx
  loadSessionsAndModels(): Promise<void>
    Purpose: reload sessions and models for the chat control page.
    Must keep session errors and model errors visible separately.

  handleStartChatRun(request: StartRunRequest): Promise<void>
    Purpose: call chatRunState.startSessionRun from the selected session context.
    Must not navigate to /runs.

  handleStopChatRun(): Promise<void>
    Purpose: call chatRunState.stopActiveRun.

SessionDetail.tsx
  SessionDetail(props): JSX.Element
    Purpose: render transcript plus SessionChatComposer when a real session exists.
    Must not mutate transcript messages.
```

### 10.6 `CHAT-RUN-M004`

Files:

```text
frontend/src/state/useSessionChatRunState.ts
frontend/src/pages/SessionsPage.tsx
```

Required functions:

```text
isTerminalRunEvent(event: RunEventDto): boolean
  Purpose: centralize terminal event test for done/error/stopped.

handleChatRunTerminal(event: RunEventDto, capturedRef: SelectedSessionRef): Promise<void>
  Purpose: reload transcript and sessions only when captured source/id still matches current selected source/id.
  Must not append event text into transcript.
```

### 10.7 `CHAT-RUN-M005`

Files:

```text
frontend/src/state/useSessionChatRunState.ts
frontend/src/components/SessionChatComposer.tsx
frontend/src/pages/SessionsPage.tsx
```

Required functions:

```text
hasActiveNonTerminalRun(state: SessionChatRunState): boolean
  Purpose: disable duplicate starts and expose clear UI state.

assertCanStartRun(): void
  Purpose: prevent double start before calling startRun.
  Must not call backend when a non-terminal run is active.

runBelongsToSelectedSession(activeRef: SelectedSessionRef | null, selectedRef: SelectedSessionRef | null): boolean
  Purpose: avoid applying an active run stream to a newly selected chat.
```

### 10.8 `CHAT-RUN-M006`

Files:

```text
doc/audit/chat-control-live-evidence.md
doc/audit/release-checklist-status.md
```

Required evidence helpers may be shell commands, but the document must record only:

```text
health status
session count
source breakdown
selected source
selected id prefix
model id/source support
transcript message count before run
runId
event type list
terminal type
terminal count
transcript message count after run
PASS or FAIL
```

Forbidden evidence:

```text
token
full session id when avoidable
prompt text
full transcript text
secret paths outside already-authenticated rawFile evidence
environment variables
```

### 10.9 `PUBLIC-ACCESS-M006`

Files:

```text
doc/audit/public-access.md
```

Required checks:

```text
public /api/health status
public /sessions shell asset names
public authenticated /api/sessions source counts
public authenticated transcript response has messages array
asset evidence for inline chat composer
NOT_ANDROID_INSTALLABILITY note for plain HTTP fixed IP
```

Forbidden checks:

```text
live Codex/opencode command execution
token printing
transcript text recording
claiming Android installability for http://118.24.15.133:20261
```

## 11. Future Bug And Feature Protocol

Any future bug or feature related to this phase must follow this protocol:

1. Add a micro ticket before code changes.
2. Name the smallest failing layer first: selection, URL state, transcript loading, model loading, run start, SSE stream, stop, terminal refresh, public rebuild, or evidence.
3. Allowed files must be narrow enough that one agent can finish the ticket without touching unrelated layers.
4. Verification must be executable without mock business data.
5. If the same verification fails twice, stop and add a new discovery or bug ticket.
6. Parser changes are forbidden unless the selected ticket is explicitly a parser ticket.
7. Runtime command builder changes are forbidden unless the selected ticket is explicitly a runtime command ticket.
8. Public deployment changes are forbidden unless the selected ticket is explicitly a public access ticket.
9. UI must not add workaround state for a backend failure.
10. Backend must not hide adapter failures to satisfy UI.

## 12. Risk Prediction After This Plan

If future agents follow this plan and `AGENTS.md`, the risk of entering an unrecoverable AI correction loop is low.

The remaining loop risks are known:

| Risk | Why It Can Loop | Required Stop |
| --- | --- | --- |
| Selected session identity uses id only | UI can open one source and run another | Use source/id tuple before URL sync |
| Composer appends fake prompt | Fake chat appears successful even if backend fails | Never mutate `transcript.messages` locally |
| SSE stream fails but UI shows success | Runtime failure hidden in UI | Render real error, do not synthesize terminal event |
| Public URL points to wrong backend | Phone sees server state instead of local machine | Public evidence must show local session counts |
| Model list hardcoded | UI can start invalid source/model pair | Models only from `GET /api/models` |
| Terminal refresh races selected session change | Wrong transcript reloads after terminal event | Compare captured source/id before refresh |
| PostgreSQL treated as session source | Stale records replace live Codex/opencode | PostgreSQL is not a session source in this phase |

Prediction:

```text
Unrecoverable loop risk: Low if each ticket is executed exactly as written.
Unrecoverable loop risk: Medium-high if an agent combines chat UI, runtime, parser, and public deployment in one pass.
```

## 13. Completion Gate For This Phase

This phase is complete only when all are true:

1. `/sessions` opens real Codex and opencode sessions.
2. Selection is source/id safe.
3. A selected session URL can be reloaded and still opens the same real chat.
4. Chat composer uses only real backend models.
5. Starting a run from chat uses `POST /api/runs`.
6. Inline stream uses only `GET /api/runs/{runId}/events`.
7. Stop uses only `POST /api/runs/{runId}/stop`.
8. Terminal events refresh transcript from the real transcript endpoint.
9. Public `http://118.24.15.133:20261` still maps to this local Java service.
10. Evidence documents do not contain token, prompt text, full transcript text, or secrets.
