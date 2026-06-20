# Full Mobile Chat Console Microtasks

Ticket: `PLAN-MOBILE-CHAT-M001`

Purpose: define every known microtask required to turn `Lqtigee` into a usable Android phone chat console for real local Codex CLI and opencode sessions.

This file is a task backlog only. It must not change application code by itself.

## 1. Non-Negotiable Rules

- No mock data.
- No smoke-only verification.
- No smoke test as the primary proof.
- No fake sessions.
- No fake models.
- No fake messages.
- No fake run ids.
- No fake SSE events.
- No generated transcript summaries.
- No fallback success arrays.
- No public-server session discovery.
- No frontend local file reads.
- No shell string command execution.
- No token printing.
- No transcript text in audit docs.

Every implementation task must be copied or referenced as the selected ticket in `doc/micro-tickets.md` before code changes, because `AGENTS.md` requires ticket-driven implementation from that file.

## 2. Source Of Truth

Backend port:

```text
20261
```

Phone access currently used for evidence:

```text
http://118.24.15.133:20261
```

Data source rules:

- Codex sessions come from local `/home/lqtiger/.codex/sessions/**/*.jsonl`.
- opencode sessions come from local `/home/lqtiger/.local/share/opencode/opencode.db`.
- Transcript messages come from the selected real session source only.
- Models come from backend configuration or later real CLI/provider evidence tickets only.
- PostgreSQL stores Lqtigee-owned data only, such as run records, settings, audit records, and future preferences.
- PostgreSQL must not replace live Codex/opencode session discovery in this phase.
- The public server is only an access mapping layer; it is not the Codex/opencode data source.

## 3. Verified Local CLI Capability Snapshot

The following capabilities were checked from local CLI help on this machine.

Codex top-level:

```text
codex exec
codex resume
codex archive
codex delete
codex unarchive
codex fork
codex app-server
codex remote-control
codex mcp
codex plugin
codex doctor
```

Codex `exec resume` options that matter for the phone console:

```text
SESSION_ID
PROMPT
--last
--all
--image
--model
--config key=value
--enable
--disable
--dangerously-bypass-approvals-and-sandbox
--dangerously-bypass-hook-trust
--skip-git-repo-check
--ephemeral
--ignore-user-config
--ignore-rules
--output-schema
--json
--output-last-message
```

Codex `resume` interactive options that matter for future remote-control work:

```text
--remote
--remote-auth-token-env
--model
--profile
--sandbox
--ask-for-approval
--search
--image
--add-dir
--no-alt-screen
```

opencode top-level:

```text
opencode run
opencode session
opencode agent
opencode models
opencode serve
opencode web
opencode attach
opencode export
opencode import
opencode db
```

opencode `run` options that matter for the phone console:

```text
message
--command
--continue
--session
--fork
--share
--model
--agent
--format json
--file
--title
--attach
--dir
--variant
--thinking
--replay
--replay-limit
--interactive
--dangerously-skip-permissions
```

opencode session/agent management:

```text
opencode session list
opencode session delete <sessionID>
opencode agent list
opencode agent create
```

If any help output changes, create a new discovery ticket before modifying command builders.

## 4. Required User Experience

The phone must behave like a real chat app, not like a transcript dump:

1. Open `/sessions`.
2. See real sessions with real titles.
3. Tap one session.
4. Open newest 10 chat messages only.
5. Scroll upward to load older messages.
6. Keep the bottom composer visible above the mobile keyboard.
7. Type a message.
8. Select model and runtime options.
9. Send to the selected real Codex/opencode session.
10. Watch inline SSE output stream on the same chat screen.
11. Stop the active real run.
12. Refresh transcript after terminal event.
13. Manage source-specific CLI options without fake controls.
14. Use public `20261` mapping while still controlling local sessions.

## 5. Microtask Format

Every task below uses:

```text
Purpose:
Allowed files:
Implementation:
Stop conditions:
Verification:
```

Allowed files must be copied into `doc/micro-tickets.md` before implementation.

## 6. Contract And API Microtasks

### MOBILE-CONTRACT-M001 Add Paged Transcript Contract

Purpose:

Define the API contract for newest-10 transcript paging.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`

Implementation:

1. Add `GET /api/sessions/{source}/{id}/transcript?limit=10`.
2. Add optional `before=<cursor>` for older messages.
3. Add `TranscriptPageInfoDto` with `oldestCursor`, `newestCursor`, and `hasMoreBefore`.
4. Add `messages` sorted oldest-to-newest within the returned page.
5. State default limit is `10`.
6. State maximum limit is backend-configured and must not be unbounded.
7. State no generated summary and no fake messages.

Stop conditions:

- Stop if cursor cannot be defined without source-specific assumptions.
- Stop if contract would expose raw prompt text in audit docs.

Verification:

```bash
rg "TranscriptPageInfoDto|limit=10|before=|hasMoreBefore|oldestCursor|newestCursor" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
```

### MOBILE-CONTRACT-M002 Add Chat Run Options Contract

Purpose:

Define source-specific runtime options for continuing a selected session from chat.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`
- `doc/security/command-permission-matrix.md`

Implementation:

1. Extend `StartRunRequest` contract with optional `codexOptions`.
2. Extend `StartRunRequest` contract with optional `opencodeOptions`.
3. Keep `sessionId`, `source`, `modelId`, `mode`, `prompt`, and `confirmDangerous` required.
4. Define that Codex options are ignored unless `source=CODEX`.
5. Define that opencode options are ignored unless `source=OPENCODE`.
6. Define validation error if wrong-source options are provided.
7. Keep existing permission matrix as the authority for dangerous modes.

Stop conditions:

- Stop if option mapping requires guessing undocumented CLI flags.
- Stop if one source's options would be accepted for the other source.

Verification:

```bash
rg "codexOptions|opencodeOptions|wrong-source|DANGER_CONFIRM_REQUIRED" doc/contracts/backend-api-contract.md doc/security/command-permission-matrix.md
```

### MOBILE-CONTRACT-M003 Add Attachment Contract

Purpose:

Define how the phone uploads files/images for Codex `--image` and opencode `--file`.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`
- `doc/security/command-permission-matrix.md`

Implementation:

1. Add `POST /api/attachments`.
2. Add `DELETE /api/attachments/{id}`.
3. Add `AttachmentDto` with `id`, `filename`, `contentType`, `sizeBytes`, `createdAt`.
4. Add `attachmentIds` to `StartRunRequest`.
5. State attachment files are stored only under a configured Lqtigee temp directory.
6. State attachment ids must be resolved server-side to safe paths.
7. State Codex image attachments may map to `--image`.
8. State opencode file attachments may map to `--file`.

Stop conditions:

- Stop if file path is accepted from the frontend.
- Stop if uploaded files can escape the configured attachment directory.

Verification:

```bash
rg "AttachmentDto|/api/attachments|attachmentIds|--image|--file|configured Lqtigee temp" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md doc/security/command-permission-matrix.md
```

### MOBILE-CONTRACT-M004 Add Source Capability Contract

Purpose:

Let the frontend know which controls are available for Codex and opencode without hardcoding fake capability switches.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/contracts/backend-response-fixtures.md`

Implementation:

1. Add `GET /api/capabilities`.
2. Add `SourceCapabilityDto` with `source`, `runOptions`, `attachments`, `sessionActions`, and `dangerousOptions`.
3. Populate contract examples only from verified local CLI help.
4. State runtime code must not claim a capability until backend validation and command builder tests exist.

Stop conditions:

- Stop if capabilities would be hardcoded in frontend.
- Stop if a capability has no local CLI evidence.

Verification:

```bash
rg "/api/capabilities|SourceCapabilityDto|runOptions|sessionActions|dangerousOptions" doc/contracts/backend-api-contract.md doc/contracts/backend-response-fixtures.md
```

### MOBILE-CONTRACT-M005 Add Session Management Contract

Purpose:

Define explicit endpoints for archive, delete, fork, export, and import style session management instead of hiding them inside generic run commands.

Allowed files:

- `doc/contracts/backend-api-contract.md`
- `doc/security/command-permission-matrix.md`

Implementation:

1. Add source-scoped session action request contract.
2. Define allowed Codex actions: `archive`, `delete`, `unarchive`, `fork`.
3. Define allowed opencode actions: `delete`, `export`, `import`, `fork`.
4. Require explicit confirmation for destructive actions.
5. State that each action needs a later CLI evidence and command builder ticket before enabling.

Stop conditions:

- Stop if destructive actions can run without explicit confirmation.
- Stop if backend action contract is not source-scoped.

Verification:

```bash
rg "archive|delete|unarchive|fork|export|import|explicit confirmation|source-scoped" doc/contracts/backend-api-contract.md doc/security/command-permission-matrix.md
```

## 7. Backend Transcript Paging Microtasks

### MOBILE-BE-PAGE-M001 Add Transcript Page DTOs

Purpose:

Create backend DTOs for paged transcript responses.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/dto/SessionTranscriptDto.java`
- `src/main/java/com/lqtigee/sparkai/dto/SessionMessageDto.java`
- `src/test/java/com/lqtigee/sparkai/dto/SessionTranscriptDtoTest.java`

Implementation:

1. Add `TranscriptPageInfoDto`.
2. Add page info to `SessionTranscriptDto`.
3. Keep message fields unchanged.
4. Do not add generated summaries.

Stop conditions:

- Stop if frontend contract was not updated first.

Verification:

```bash
mvn test -Dtest=SessionTranscriptDtoTest
rg "TranscriptPageInfoDto|hasMoreBefore|oldestCursor|newestCursor" src/main/java src/test/java
```

### MOBILE-BE-PAGE-M002 Page Codex Transcript Reader

Purpose:

Return newest 10 visible Codex messages and older pages from a real JSONL file.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/codex/CodexTranscriptReader.java`
- `src/test/java/com/lqtigee/sparkai/codex/CodexTranscriptReaderTest.java`
- `src/test/resources/samples/codex-transcript-sample.jsonl`

Implementation:

1. Add method `readPage(Path rawFile, int limit, String beforeCursor)`.
2. Cursor must be derived from real line/message position, not from fake ids.
3. Default to newest 10 messages when `beforeCursor` is absent.
4. Exclude developer, system, tool, reasoning, encrypted, snapshot, and empty text records.
5. Return messages oldest-to-newest within the page.
6. Set `hasMoreBefore` from real available older visible messages.

Stop conditions:

- Stop if Codex cursor cannot be stable from the parsed JSONL structure.
- Stop if a missing field would be filled from filename fallback.

Verification:

```bash
mvn test -Dtest=CodexTranscriptReaderTest
rg "readPage|beforeCursor|hasMoreBefore|limit" src/main/java/com/lqtigee/sparkai/codex src/test/java/com/lqtigee/sparkai/codex
```

### MOBILE-BE-PAGE-M003 Page opencode Transcript Reader

Purpose:

Return newest 10 visible opencode messages and older pages from the real SQLite tables.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeSqliteTranscriptReader.java`
- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeSqliteTranscriptReaderTest.java`

Implementation:

1. Add method `readPage(Path database, String sessionId, int limit, String beforeCursor)`.
2. Cursor must come from real message/part ordering fields.
3. Default to newest 10 visible user/assistant text messages.
4. Exclude non-text parts and empty text.
5. Return messages oldest-to-newest within the page.
6. Set `hasMoreBefore` only from real older visible rows.

Stop conditions:

- Stop if SQLite schema does not expose stable ordering.
- Stop if reader would need fake cursor values.

Verification:

```bash
mvn test -Dtest=OpencodeSqliteTranscriptReaderTest
rg "readPage|beforeCursor|hasMoreBefore|limit" src/main/java/com/lqtigee/sparkai/opencode src/test/java/com/lqtigee/sparkai/opencode
```

### MOBILE-BE-PAGE-M004 Add Paged Transcript Endpoint

Purpose:

Wire query params into the transcript service and controller.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/SessionTranscriptService.java`
- `src/main/java/com/lqtigee/sparkai/web/SessionController.java`
- `src/test/java/com/lqtigee/sparkai/service/SessionTranscriptServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/SessionControllerTest.java`

Implementation:

1. Accept `limit` and `before`.
2. Default `limit` to 10.
3. Reject limit less than 1.
4. Reject limit greater than configured maximum.
5. Validate selected real session before reading transcript.
6. Dispatch to source-specific paged reader.
7. Do not convert reader failures into empty success.

Stop conditions:

- Stop if service needs controller filesystem access.
- Stop if a failed source would return an empty page.

Verification:

```bash
mvn test -Dtest=SessionTranscriptServiceTest,SessionControllerTest
rg "RequestParam|limit|before|TranscriptPageInfoDto" src/main/java/com/lqtigee/sparkai/service src/main/java/com/lqtigee/sparkai/web src/test/java
```

## 8. Frontend Transcript Paging Microtasks

### MOBILE-FE-PAGE-M001 Add Paged Transcript Types And API

Purpose:

Teach the frontend client about page info and cursor query params.

Allowed files:

- `frontend/src/types/api.ts`
- `frontend/src/api/remoteApi.ts`

Implementation:

1. Add `TranscriptPageInfoDto`.
2. Add `pageInfo` to `SessionTranscriptDto`.
3. Change `getSessionTranscript(source, id, options)` to accept `limit` and `before`.
4. Default caller behavior must still request real backend data.

Stop conditions:

- Stop if backend contract is not updated.

Verification:

```bash
cd frontend && npm run build
rg "TranscriptPageInfoDto|pageInfo|before|limit" frontend/src/types/api.ts frontend/src/api/remoteApi.ts
```

### MOBILE-FE-PAGE-M002 Add Transcript Paging State Hook

Purpose:

Load newest 10 messages first and prepend older pages when the user scrolls upward.

Allowed files:

- `frontend/src/state/useSessionTranscriptState.ts`

Implementation:

1. Replace single `loadTranscript` state with page-aware state.
2. Add `loadNewestTranscript(source, id)`.
3. Add `loadOlderMessages()`.
4. Store `messages`, `pageInfo`, `loadingNewest`, `loadingOlder`, `error`.
5. Prevent concurrent older-page requests.
6. Do not create local fake messages.

Stop conditions:

- Stop if API response lacks page info.

Verification:

```bash
cd frontend && npm run build
rg "loadNewestTranscript|loadOlderMessages|loadingOlder|hasMoreBefore|pageInfo" frontend/src/state/useSessionTranscriptState.ts
```

### MOBILE-FE-PAGE-M003 Show Newest 10 And Load Older At Top

Purpose:

Stop dumping the whole transcript and use chat-style history loading.

Allowed files:

- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Render only messages from paged transcript state.
2. Add a top load-older control when `hasMoreBefore` is true.
3. Trigger `loadOlderMessages` when the user scrolls near the top.
4. Keep loading older distinct from loading newest.
5. Preserve selected session title and metadata.
6. Do not hide API errors.

Stop conditions:

- Stop if all messages are still rendered on first load.

Verification:

```bash
cd frontend && npm run build
rg "loadOlderMessages|hasMoreBefore|loadingOlder|chat-history-top" frontend/src/components/SessionDetail.tsx frontend/src/pages/SessionsPage.tsx frontend/src/styles/global.css
```

### MOBILE-FE-PAGE-M004 Preserve Scroll Anchor When Older Messages Prepend

Purpose:

Prevent the chat viewport from jumping when older messages are loaded.

Allowed files:

- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Track scroll container before older load.
2. After prepending messages, restore visual anchor based on scrollHeight delta.
3. Keep newest page initially scrolled to bottom.
4. Do not force bottom scroll while the user is reading older messages.

Stop conditions:

- Stop if anchor restore requires changing backend API.

Verification:

```bash
cd frontend && npm run build
rg "scrollHeight|scrollTop|loadOlder|chat-scroll" frontend/src/components/SessionDetail.tsx frontend/src/styles/global.css
```

## 9. Bottom Composer And Chat Input Microtasks

### MOBILE-COMPOSER-M001 Add Bottom Composer Shell

Purpose:

Add the always-accessible bottom composer in the selected session chat.

Allowed files:

- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Render composer at bottom of chat panel when a real session is selected.
2. Include textarea, send button, stop button slot, and options button slot.
3. Keep composer above mobile safe area.
4. Keep textarea usable with mobile keyboard.
5. Do not call APIs in this shell ticket.

Stop conditions:

- Stop if composer appears when no real session is selected.

Verification:

```bash
cd frontend && npm run build
rg "SessionChatComposer|bottom composer|chat-composer|safe-area-inset-bottom" frontend/src/components frontend/src/styles/global.css
```

### MOBILE-COMPOSER-M002 Add Prompt Draft State

Purpose:

Keep one draft prompt per selected source/id.

Allowed files:

- `frontend/src/state/useChatDraftState.ts`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/SessionDetail.tsx`

Implementation:

1. Create `useChatDraftState`.
2. Key drafts by `source:id`.
3. Store draft in localStorage.
4. Clear draft only after successful `POST /api/runs` returns a real run id.
5. Do not store API tokens with drafts.
6. Pass selected session source/id from the selected-session detail into the composer.

Stop conditions:

- Stop if draft key uses id only.

Verification:

```bash
cd frontend && npm run build
rg "useChatDraftState|source.*id|localStorage|clearDraft" frontend/src/state frontend/src/components/SessionChatComposer.tsx
```

### MOBILE-COMPOSER-M003 Add Model And Mode Controls

Purpose:

Let the user select real backend model and command mode directly in the chat composer.

Allowed files:

- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/ModelSelect.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Use models from `GET /api/models` only.
2. Filter by selected session source.
3. Add mode selector for `ASK`, `REVIEW`, `EDIT`, `SHELL`.
4. Require dangerous confirmation for `SHELL`.
5. Disable send when no real supported model exists.

Stop conditions:

- Stop if any model is hardcoded.

Verification:

```bash
cd frontend && npm run build
rg "ASK|REVIEW|EDIT|SHELL|confirmDangerous|ModelSelect" frontend/src/components/SessionChatComposer.tsx frontend/src/components/ModelSelect.tsx
! rg "sample model|fake model|mock model" frontend/src
```

### MOBILE-COMPOSER-M004 Wire Send To Real Run API

Purpose:

Send the composer prompt to the selected real session using `POST /api/runs`.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Build `StartRunRequest` from selected `RemoteSession`.
2. Include selected real `modelId`.
3. Include selected mode.
4. Include prompt.
5. Include dangerous confirmation.
6. Call `startRun`.
7. Store only returned real `runId`.
8. Do not navigate away from chat.

Stop conditions:

- Stop if prompt would be appended locally as a fake user message.

Verification:

```bash
cd frontend && npm run build
rg "startRun|StartRunRequest|runId|onStart" frontend/src/state/useSessionChatRunState.ts frontend/src/components/SessionChatComposer.tsx frontend/src/pages/SessionsPage.tsx
! rg "fake message|mock message|sample prompt" frontend/src
```

### MOBILE-COMPOSER-M005 Add Keyboard And Safe-Area Behavior

Purpose:

Make the composer usable on Android phone keyboards.

Allowed files:

- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Keep composer sticky at bottom of chat panel.
2. Use `env(safe-area-inset-bottom)`.
3. Ensure message list bottom padding accounts for composer height.
4. Keep send button visible at 320px width.
5. Prevent horizontal overflow.

Stop conditions:

- Stop if layout requires hiding the prompt input.

Verification:

```bash
cd frontend && npm run build
rg "safe-area-inset-bottom|position: sticky|chat-composer|padding-bottom" frontend/src/styles/global.css
```

## 10. Inline Streaming Microtasks

### MOBILE-STREAM-M001 Add Chat Run State Hook

Purpose:

Own active run id, SSE stream, events, terminal state, and errors for one selected chat.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`

Implementation:

1. Call `startRun`.
2. Open `openRunEvents`.
3. Append only real `RunEventDto` events.
4. Detect `done`, `error`, and `stopped`.
5. Close stream on unmount.
6. Keep `starting`, `streaming`, `stopping`, `terminal`, and `error`.

Stop conditions:

- Stop if fake events are needed for UI state.

Verification:

```bash
cd frontend && npm run build
rg "useSessionChatRunState|openRunEvents|RunEventDto|done|error|stopped" frontend/src/state/useSessionChatRunState.ts
```

### MOBILE-STREAM-M002 Render Inline SSE Output

Purpose:

Show streaming output on the same chat screen so the phone can see live progress.

Allowed files:

- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/RunTimeline.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Render run events below or above composer inside chat panel.
2. Show event type and message.
3. Auto-scroll streaming area while user is at bottom.
4. Keep output scrollable on mobile.
5. Do not merge run events into transcript messages.

Stop conditions:

- Stop if stream output is only visible on `/runs`.

Verification:

```bash
cd frontend && npm run build
rg "RunTimeline|run-timeline|streaming|events" frontend/src/components/SessionChatComposer.tsx frontend/src/components/RunTimeline.tsx frontend/src/styles/global.css
```

### MOBILE-STREAM-M003 Add Stop Button For Active Run

Purpose:

Let phone stop the real running process from the chat screen.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/components/SessionChatComposer.tsx`

Implementation:

1. Enable stop only when real run id exists and no terminal event exists.
2. Call `stopRun(runId)`.
3. Show stopping state.
4. Do not synthesize stopped events.

Stop conditions:

- Stop if no real run id exists.

Verification:

```bash
cd frontend && npm run build
rg "stopRun|stopActiveRun|stopping|terminal" frontend/src/state/useSessionChatRunState.ts frontend/src/components/SessionChatComposer.tsx
```

### MOBILE-STREAM-M004 Refresh Transcript After Terminal Event

Purpose:

Reload real transcript after the selected session run completes or stops.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Capture source/id at send time.
2. On terminal event, compare captured source/id to current selected source/id.
3. Reload transcript if still selected.
4. Reload session list for updated title/preview/time.
5. Do not fake assistant messages.

Stop conditions:

- Stop if selected session changed and code would refresh the wrong chat.

Verification:

```bash
cd frontend && npm run build
rg "onTerminal|loadNewestTranscript|loadSessions|selectedSessionRef" frontend/src/state/useSessionChatRunState.ts frontend/src/pages/SessionsPage.tsx
! rg "setTranscript.*event|fake assistant|mock transcript" frontend/src
```

### MOBILE-STREAM-M005 Guard Double Start And Stale Stream

Purpose:

Prevent double sends and keep active stream tied to the session that started it.

Allowed files:

- `frontend/src/state/useSessionChatRunState.ts`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Store active session ref with the run.
2. Disable send while a non-terminal run is active.
3. Keep stop bound to active run.
4. Do not auto-stop when switching sessions.
5. Do not show old stream as current session output.

Stop conditions:

- Stop if run ownership would require an invented backend endpoint.

Verification:

```bash
cd frontend && npm run build
rg "activeSessionRef|nonTerminal|terminal|disabled" frontend/src/state/useSessionChatRunState.ts frontend/src/components/SessionChatComposer.tsx frontend/src/pages/SessionsPage.tsx
```

## 11. Codex Controls Microtasks

### MOBILE-CODEX-M001 Record Codex CLI Option Evidence

Purpose:

Record current local Codex help evidence before adding UI controls.

Allowed files:

- `doc/discovery/codex-chat-controls.md`

Implementation:

1. Record command used: `codex exec resume --help`.
2. Record command used: `codex resume --help`.
3. Summarize option names only.
4. Do not record auth files or secrets.

Stop conditions:

- Stop if Codex command is unavailable.

Verification:

```bash
test -f doc/discovery/codex-chat-controls.md
rg "exec resume|--image|--model|--config|--json|--sandbox|--ask-for-approval|--search|--add-dir" doc/discovery/codex-chat-controls.md
```

### MOBILE-CODEX-M002 Add Codex Options DTO

Purpose:

Create typed backend request fields for Codex-only options.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/dto/StartRunRequest.java`
- `src/main/java/com/lqtigee/sparkai/dto/CodexRunOptionsDto.java`
- `src/test/java/com/lqtigee/sparkai/dto/StartRunRequestTest.java`

Implementation:

1. Add `CodexRunOptionsDto`.
2. Include image attachment ids.
3. Include profile.
4. Include sandbox.
5. Include approval policy.
6. Include search enabled.
7. Include add-dir list.
8. Include config overrides as structured key/value list.
9. Include output schema attachment id only after attachment contract exists.
10. Validate wrong-source options elsewhere.

Stop conditions:

- Stop if contract was not updated.

Verification:

```bash
mvn test -Dtest=StartRunRequestTest
rg "CodexRunOptionsDto|profile|sandbox|approval|search|addDir|config" src/main/java src/test/java
```

### MOBILE-CODEX-M003 Validate Codex Options

Purpose:

Reject unsafe or unsupported Codex options before command building.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`
- `src/test/java/com/lqtigee/sparkai/service/RunServiceTest.java`

Implementation:

1. Reject `codexOptions` when source is not `CODEX`.
2. Reject `opencodeOptions` when source is `CODEX`.
3. Validate sandbox values against Codex help.
4. Validate approval policy values against Codex help.
5. Require confirmation for dangerous bypass flags.
6. Reject direct filesystem paths from frontend.

Stop conditions:

- Stop if frontend-supplied paths would be trusted directly.

Verification:

```bash
mvn test -Dtest=RunServiceTest
rg "codexOptions|approval|sandbox|DANGER_CONFIRM_REQUIRED|wrong source" src/main/java/com/lqtigee/sparkai/service src/test/java/com/lqtigee/sparkai/service
```

### MOBILE-CODEX-M004 Map Codex Safe Runtime Options

Purpose:

Map safe Codex options to `codex exec resume` argument arrays.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java`

Implementation:

1. Map model to `-m`.
2. Map profile to `-p` only if supported by selected command path.
3. Map sandbox to `-s` only when using a command path that supports it.
4. Map `--json`.
5. Preserve selected session id.
6. Preserve prompt as one argument.
7. Do not use `sh -c`.

Stop conditions:

- Stop if local help shows selected command path does not support an option.

Verification:

```bash
mvn test -Dtest=CodexCommandBuilderTest
rg "exec|resume|--json|-m|sessionId|prompt" src/main/java/com/lqtigee/sparkai/runtime src/test/java/com/lqtigee/sparkai/runtime
```

### MOBILE-CODEX-M005 Map Codex Image Attachments

Purpose:

Support Codex `--image` from uploaded phone attachments.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexCommandBuilder.java`
- `src/main/java/com/lqtigee/sparkai/service/AttachmentService.java`
- `src/test/java/com/lqtigee/sparkai/runtime/CodexCommandBuilderTest.java`

Implementation:

1. Resolve attachment ids server-side.
2. Require image content types for Codex `--image`.
3. Add one `--image` argument per resolved safe image path.
4. Reject missing attachment ids.
5. Do not accept raw paths from frontend.

Stop conditions:

- Stop if attachment service is not implemented.

Verification:

```bash
mvn test -Dtest=CodexCommandBuilderTest
rg "--image|attachmentIds|AttachmentService" src/main/java src/test/java
```

### MOBILE-CODEX-M006 Add Codex Options Sheet UI

Purpose:

Expose Codex-specific controls only when the selected session source is `CODEX`.

Allowed files:

- `frontend/src/components/CodexOptionsSheet.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Add Codex options sheet component.
2. Show image attachment picker entry.
3. Show profile input/select only when capabilities say it is enabled.
4. Show sandbox selector.
5. Show approval policy selector.
6. Show search toggle.
7. Show add-dir input controlled by backend-validated roots.
8. Hide this sheet for opencode sessions.

Stop conditions:

- Stop if capabilities endpoint is missing and UI would hardcode availability.

Verification:

```bash
cd frontend && npm run build
rg "CodexOptionsSheet|sandbox|approval|search|addDir|CODEX" frontend/src/components frontend/src/styles/global.css
```

## 12. opencode Controls Microtasks

### MOBILE-OPENCODE-M001 Record opencode CLI Option Evidence

Purpose:

Record current local opencode help evidence before adding UI controls.

Allowed files:

- `doc/discovery/opencode-chat-controls.md`

Implementation:

1. Record command used: `opencode run --help`.
2. Record command used: `opencode session --help`.
3. Record command used: `opencode agent --help`.
4. Summarize option names only.
5. Do not record secrets.

Stop conditions:

- Stop if opencode command is unavailable.

Verification:

```bash
test -f doc/discovery/opencode-chat-controls.md
rg "opencode run|--session|--fork|--file|--agent|--variant|--thinking|--replay-limit|session delete|agent list" doc/discovery/opencode-chat-controls.md
```

### MOBILE-OPENCODE-M002 Add opencode Options DTO

Purpose:

Create typed backend request fields for opencode-only options.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/dto/StartRunRequest.java`
- `src/main/java/com/lqtigee/sparkai/dto/OpencodeRunOptionsDto.java`
- `src/test/java/com/lqtigee/sparkai/dto/StartRunRequestTest.java`

Implementation:

1. Add `OpencodeRunOptionsDto`.
2. Include agent id/name.
3. Include fork boolean.
4. Include share boolean.
5. Include variant string.
6. Include thinking boolean.
7. Include replay boolean.
8. Include replay limit number.
9. Include file attachment ids.
10. Include title only for new-session future tasks, not selected-session continue.

Stop conditions:

- Stop if selected-session continue requires a title.

Verification:

```bash
mvn test -Dtest=StartRunRequestTest
rg "OpencodeRunOptionsDto|agent|fork|share|variant|thinking|replayLimit|attachmentIds" src/main/java src/test/java
```

### MOBILE-OPENCODE-M003 Validate opencode Options

Purpose:

Reject unsafe or unsupported opencode options before command building.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/RunService.java`
- `src/test/java/com/lqtigee/sparkai/service/RunServiceTest.java`

Implementation:

1. Reject `opencodeOptions` when source is not `OPENCODE`.
2. Reject `codexOptions` when source is `OPENCODE`.
3. Require confirmation for `dangerously-skip-permissions`.
4. Validate replay limit bounds.
5. Validate agent exists only after real agent list endpoint exists.
6. Reject direct frontend file paths.

Stop conditions:

- Stop if agent validation would use fake agent list.

Verification:

```bash
mvn test -Dtest=RunServiceTest
rg "opencodeOptions|replayLimit|dangerously|wrong source|agent" src/main/java/com/lqtigee/sparkai/service src/test/java/com/lqtigee/sparkai/service
```

### MOBILE-OPENCODE-M004 Add Real opencode Agent List Endpoint

Purpose:

Expose real opencode agents for the phone options sheet.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/opencode/OpencodeAgentService.java`
- `src/main/java/com/lqtigee/sparkai/web/OpencodeController.java`
- `src/main/java/com/lqtigee/sparkai/dto/OpencodeAgentDto.java`
- `src/test/java/com/lqtigee/sparkai/opencode/OpencodeAgentServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/OpencodeControllerTest.java`

Implementation:

1. Use real opencode source/config or CLI evidence.
2. Return typed error if agents cannot be listed.
3. Do not return fake default agents.
4. Protect endpoint with bearer token.

Stop conditions:

- Stop if no real agent source is discovered.

Verification:

```bash
mvn test -Dtest=OpencodeAgentServiceTest,OpencodeControllerTest
rg "OpencodeAgentDto|/api/opencode/agents|agent list" src/main/java src/test/java
```

### MOBILE-OPENCODE-M005 Map opencode Runtime Options

Purpose:

Map opencode options to `opencode run --format json --session <id>`.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilderTest.java`

Implementation:

1. Keep `--format json`.
2. Keep `--session`.
3. Map model to `--model`.
4. Map workspace to `--dir`.
5. Map agent to `--agent`.
6. Map fork to `--fork`.
7. Map share to `--share`.
8. Map variant to `--variant`.
9. Map thinking to `--thinking`.
10. Map replay false as `--no-replay` if local help supports it.
11. Map replay limit to `--replay-limit`.
12. Preserve prompt as argument array item.
13. Do not use `sh -c`.

Stop conditions:

- Stop if a flag is not confirmed by local help.

Verification:

```bash
mvn test -Dtest=OpencodeCommandBuilderTest
rg "--format|--session|--model|--dir|--agent|--fork|--variant|--replay-limit" src/main/java/com/lqtigee/sparkai/runtime src/test/java/com/lqtigee/sparkai/runtime
```

### MOBILE-OPENCODE-M006 Map opencode File Attachments

Purpose:

Support opencode `--file` from uploaded phone attachments.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilder.java`
- `src/main/java/com/lqtigee/sparkai/service/AttachmentService.java`
- `src/test/java/com/lqtigee/sparkai/runtime/OpencodeCommandBuilderTest.java`

Implementation:

1. Resolve attachment ids server-side.
2. Add one `--file` per resolved safe attachment path.
3. Reject missing attachment ids.
4. Do not accept raw frontend paths.

Stop conditions:

- Stop if attachment service is not implemented.

Verification:

```bash
mvn test -Dtest=OpencodeCommandBuilderTest
rg "--file|attachmentIds|AttachmentService" src/main/java src/test/java
```

### MOBILE-OPENCODE-M007 Add opencode Options Sheet UI

Purpose:

Expose opencode-specific controls only when the selected session source is `OPENCODE`.

Allowed files:

- `frontend/src/components/OpencodeOptionsSheet.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/state/useOpencodeAgentsState.ts`
- `frontend/src/styles/global.css`

Implementation:

1. Add opencode options sheet component.
2. Load real agents from backend endpoint.
3. Show agent selector.
4. Show fork toggle.
5. Show share toggle only if backend capability enables it.
6. Show variant input/select.
7. Show thinking toggle.
8. Show replay and replay-limit controls.
9. Show file attachment picker entry.
10. Hide this sheet for Codex sessions.

Stop conditions:

- Stop if agent list endpoint returns fake data.

Verification:

```bash
cd frontend && npm run build
rg "OpencodeOptionsSheet|useOpencodeAgentsState|agent|fork|variant|thinking|replay|OPENCODE" frontend/src
```

## 13. Attachment Microtasks

### MOBILE-ATTACH-M001 Add Attachment Storage Properties

Purpose:

Configure where uploaded phone files are stored before CLI execution.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/config/RemoteProperties.java`
- `src/main/resources/application.yml`
- `src/test/java/com/lqtigee/sparkai/config/RemotePropertiesTest.java`

Implementation:

1. Add attachment root property.
2. Add max file size property.
3. Add allowed content type config.
4. Validate root is configured and inside allowed service-owned path.

Stop conditions:

- Stop if attachment root would be arbitrary user input.

Verification:

```bash
mvn test -Dtest=RemotePropertiesTest
rg "attachment|upload|max" src/main/java/com/lqtigee/sparkai/config src/main/resources/application.yml src/test/java
```

### MOBILE-ATTACH-M002 Add Attachment Upload Endpoint

Purpose:

Let the phone upload images/files securely.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/AttachmentService.java`
- `src/main/java/com/lqtigee/sparkai/web/AttachmentController.java`
- `src/main/java/com/lqtigee/sparkai/dto/AttachmentDto.java`
- `src/test/java/com/lqtigee/sparkai/service/AttachmentServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/AttachmentControllerTest.java`

Implementation:

1. Accept multipart upload.
2. Require bearer token.
3. Store file under configured attachment root.
4. Generate server-owned attachment id.
5. Return `AttachmentDto`.
6. Reject oversized files.
7. Reject forbidden content types.
8. Do not expose raw filesystem path.

Stop conditions:

- Stop if upload can overwrite existing files.

Verification:

```bash
mvn test -Dtest=AttachmentServiceTest,AttachmentControllerTest
rg "/api/attachments|MultipartFile|AttachmentDto|sizeBytes|contentType" src/main/java src/test/java
```

### MOBILE-ATTACH-M003 Add Attachment Delete Endpoint

Purpose:

Allow removing uploaded files before sending a prompt.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/AttachmentService.java`
- `src/main/java/com/lqtigee/sparkai/web/AttachmentController.java`
- `src/test/java/com/lqtigee/sparkai/service/AttachmentServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/AttachmentControllerTest.java`

Implementation:

1. Add `DELETE /api/attachments/{id}`.
2. Require bearer token.
3. Delete only files owned by attachment service.
4. Return typed success or typed not found error.

Stop conditions:

- Stop if path deletion can escape attachment root.

Verification:

```bash
mvn test -Dtest=AttachmentServiceTest,AttachmentControllerTest
rg "DELETE|attachments|PathGuard|attachment root" src/main/java src/test/java
```

### MOBILE-ATTACH-M004 Add Frontend Attachment Picker

Purpose:

Let phone users attach images/files to the next prompt.

Allowed files:

- `frontend/src/api/remoteApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/state/useAttachmentsState.ts`
- `frontend/src/components/AttachmentPicker.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Add attachment API types.
2. Add upload function.
3. Add delete function.
4. Add state hook.
5. Add file input UI.
6. Show uploaded attachment chips.
7. Pass attachment ids to `StartRunRequest`.
8. Do not upload until user selects real local phone file.

Stop conditions:

- Stop if backend attachment endpoint is missing.

Verification:

```bash
cd frontend && npm run build
rg "AttachmentPicker|useAttachmentsState|uploadAttachment|deleteAttachment|attachmentIds" frontend/src
```

## 14. Session Identity And Navigation Microtasks

### MOBILE-SESSION-M001 Persist Selection As Source/ID Tuple

Purpose:

Prevent Codex/opencode id collisions and wrong-session runs.

Allowed files:

- `frontend/src/types/api.ts`
- `frontend/src/state/useSessionsState.ts`
- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/pages/ControlPage.tsx`

Implementation:

1. Add `SelectedSessionRef`.
2. Persist source and id together.
3. Clear stale selection after real session list loads.
4. Find selected session by source/id only.

Stop conditions:

- Stop if code keeps id-only selected session state.

Verification:

```bash
cd frontend && npm run build
rg "SelectedSessionRef|selectedSessionRef|lqtigee_selected_session_source" frontend/src
```

### MOBILE-SESSION-M002 Sync Selected Chat With URL

Purpose:

Allow reload and direct open of the selected chat.

Allowed files:

- `frontend/src/pages/SessionsPage.tsx`

Implementation:

1. Read `source` and `sessionId` query params.
2. Validate source.
3. Select only if loaded real sessions contain source/id.
4. Update URL on selection.
5. Clear URL on back.

Stop conditions:

- Stop if invalid URL creates a fake selected session.

Verification:

```bash
cd frontend && npm run build
rg "URLSearchParams|sessionId|replaceState|source" frontend/src/pages/SessionsPage.tsx
```

### MOBILE-SESSION-M003 Add Session Action Menu Contract UI

Purpose:

Provide a phone menu for source-specific actions without executing them yet.

Allowed files:

- `frontend/src/components/SessionActionMenu.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Show actions from backend capability data only.
2. Separate destructive actions visually.
3. Require confirmation UI state for destructive actions.
4. Do not call backend action endpoints in this ticket.

Stop conditions:

- Stop if actions are hardcoded without capabilities.

Verification:

```bash
cd frontend && npm run build
rg "SessionActionMenu|confirmation|destructive|capabilities" frontend/src
```

## 15. Session Management Backend Microtasks

### MOBILE-MGMT-CODEX-M001 Add Codex Archive Command Builder

Purpose:

Implement Codex archive/unarchive command specs with tests before exposing UI.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexSessionActionCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/CodexSessionActionCommandBuilderTest.java`

Implementation:

1. Build argument arrays for `codex archive <id>`.
2. Build argument arrays for `codex unarchive <id>`.
3. Reject blank session id.
4. Do not use shell strings.

Stop conditions:

- Stop if local help evidence for action is missing.

Verification:

```bash
mvn test -Dtest=CodexSessionActionCommandBuilderTest
rg "archive|unarchive|List<String>|sh -c" src/main/java src/test/java
```

### MOBILE-MGMT-CODEX-M002 Add Codex Delete Command Builder

Purpose:

Implement Codex delete command spec with explicit confirmation.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexSessionActionCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/CodexSessionActionCommandBuilderTest.java`

Implementation:

1. Build argument array for `codex delete <id>`.
2. Require confirmation flag before building command.
3. Reject blank session id.
4. Do not use shell strings.

Stop conditions:

- Stop if destructive confirmation is not available in contract.

Verification:

```bash
mvn test -Dtest=CodexSessionActionCommandBuilderTest
rg "delete|confirm|CodexSessionActionCommandBuilder" src/main/java src/test/java
```

### MOBILE-MGMT-CODEX-M003 Add Codex Fork Action

Purpose:

Fork a Codex session using verified CLI action when requested.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/CodexSessionActionCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/CodexSessionActionCommandBuilderTest.java`

Implementation:

1. Build argument array for `codex fork <id>`.
2. Require selected real session.
3. Return process output through typed action result endpoint in later service ticket.

Stop conditions:

- Stop if fork command help is missing.

Verification:

```bash
mvn test -Dtest=CodexSessionActionCommandBuilderTest
rg "fork" src/main/java/com/lqtigee/sparkai/runtime src/test/java/com/lqtigee/sparkai/runtime
```

### MOBILE-MGMT-OPENCODE-M001 Add opencode Delete Command Builder

Purpose:

Implement opencode session deletion with explicit confirmation.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/OpencodeSessionActionCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/OpencodeSessionActionCommandBuilderTest.java`

Implementation:

1. Build argument array for `opencode session delete <sessionID>`.
2. Require confirmation flag.
3. Reject blank session id.
4. Do not use shell strings.

Stop conditions:

- Stop if `opencode session delete` help evidence is missing.

Verification:

```bash
mvn test -Dtest=OpencodeSessionActionCommandBuilderTest
rg "session|delete|confirm" src/main/java/com/lqtigee/sparkai/runtime src/test/java/com/lqtigee/sparkai/runtime
```

### MOBILE-MGMT-OPENCODE-M002 Add opencode Export Action

Purpose:

Support exporting a real opencode session without leaking transcript text into audit docs.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/runtime/OpencodeSessionActionCommandBuilder.java`
- `src/test/java/com/lqtigee/sparkai/runtime/OpencodeSessionActionCommandBuilderTest.java`

Implementation:

1. Build argument array for `opencode export <sessionID>`.
2. Stream output only through authenticated endpoint.
3. Do not write exported transcript to docs.

Stop conditions:

- Stop if export output handling is undefined.

Verification:

```bash
mvn test -Dtest=OpencodeSessionActionCommandBuilderTest
rg "export" src/main/java/com/lqtigee/sparkai/runtime src/test/java/com/lqtigee/sparkai/runtime
```

## 16. Capabilities And Settings Microtasks

### MOBILE-CAP-M001 Backend Capabilities Service

Purpose:

Provide real enabled UI controls based on implemented backend support.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/service/CapabilityService.java`
- `src/main/java/com/lqtigee/sparkai/web/CapabilityController.java`
- `src/main/java/com/lqtigee/sparkai/dto/SourceCapabilityDto.java`
- `src/test/java/com/lqtigee/sparkai/service/CapabilityServiceTest.java`
- `src/test/java/com/lqtigee/sparkai/web/CapabilityControllerTest.java`

Implementation:

1. Return `CODEX` capabilities only for implemented Codex options.
2. Return `OPENCODE` capabilities only for implemented opencode options.
3. Do not claim unimplemented controls.
4. Require bearer token.

Stop conditions:

- Stop if capability is inferred from frontend hardcoding.

Verification:

```bash
mvn test -Dtest=CapabilityServiceTest,CapabilityControllerTest
rg "/api/capabilities|SourceCapabilityDto|CODEX|OPENCODE" src/main/java src/test/java
```

### MOBILE-CAP-M002 Frontend Capabilities State

Purpose:

Load capability data and hide unavailable controls.

Allowed files:

- `frontend/src/types/api.ts`
- `frontend/src/api/remoteApi.ts`
- `frontend/src/state/useCapabilitiesState.ts`
- `frontend/src/components/SessionChatComposer.tsx`

Implementation:

1. Add capability types.
2. Add `getCapabilities()`.
3. Add state hook.
4. Load only when token exists.
5. Hide controls not enabled by backend.
6. Do not hardcode controls as enabled.

Stop conditions:

- Stop if backend endpoint is missing.

Verification:

```bash
cd frontend && npm run build
rg "useCapabilitiesState|getCapabilities|SourceCapabilityDto|capabilities" frontend/src
```

## 17. Mobile UI Completion Microtasks

### MOBILE-UI-M001 Phone Chat Layout Polish

Purpose:

Make `/sessions` feel like a real phone chat app.

Allowed files:

- `frontend/src/components/SessionCard.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/styles/global.css`
- `doc/audit/mobile-console-ui.md`

Implementation:

1. Compact session list.
2. Full-height chat panel.
3. Sticky header.
4. Scrollable message list.
5. Sticky bottom composer.
6. No horizontal overflow at 320px.
7. No nested cards inside cards.
8. No mock explanatory panels.

Stop conditions:

- Stop if layout hides the input.

Verification:

```bash
cd frontend && npm run build
rg "MOBILE-UI-M001|320px|bottom composer|no mock" doc/audit/mobile-console-ui.md
```

### MOBILE-UI-M002 Source-Specific Option Drawer

Purpose:

Give phone users access to CLI features without cluttering the composer.

Allowed files:

- `frontend/src/components/ChatOptionsDrawer.tsx`
- `frontend/src/components/CodexOptionsSheet.tsx`
- `frontend/src/components/OpencodeOptionsSheet.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/styles/global.css`

Implementation:

1. Add options drawer trigger.
2. Render Codex sheet for Codex sessions.
3. Render opencode sheet for opencode sessions.
4. Keep drawer accessible on mobile.
5. Do not show unavailable capabilities.

Stop conditions:

- Stop if drawer controls are not backed by capability data.

Verification:

```bash
cd frontend && npm run build
rg "ChatOptionsDrawer|CodexOptionsSheet|OpencodeOptionsSheet|capabilities" frontend/src
```

### MOBILE-UI-M003 Real Empty/Error States

Purpose:

Keep phone UI honest when data cannot load.

Allowed files:

- `frontend/src/pages/SessionsPage.tsx`
- `frontend/src/components/SessionDetail.tsx`
- `frontend/src/components/SessionChatComposer.tsx`
- `frontend/src/components/ErrorPanel.tsx`

Implementation:

1. Show token missing state before protected calls.
2. Show sessions error when `/api/sessions` fails.
3. Show transcript error when transcript fails.
4. Show models error when `/api/models` fails.
5. Show run error when start/SSE/stop fails.
6. Empty success only after real endpoint succeeds with empty result.

Stop conditions:

- Stop if failed API call renders empty success.

Verification:

```bash
cd frontend && npm run build
! rg "catch.*\\[\\]|fallback.*\\[\\]|fake|mock" frontend/src
```

## 18. PostgreSQL Persistence Microtasks

### MOBILE-PG-RUN-M001 Add Run Record Schema

Purpose:

Persist Lqtigee-owned run metadata in PostgreSQL without replacing live session discovery.

Allowed files:

- `src/main/resources/db/migration/V*_run_records.sql`
- `src/test/resources/db/run-record-schema.sql`
- `doc/architecture.md`

Implementation:

1. Add run record table.
2. Store run id, source, session id, model id, status, started_at, ended_at.
3. Do not store prompt text by default.
4. Do not store transcript text.
5. State PostgreSQL is not the session source.

Stop conditions:

- Stop if migration would store secrets or full prompts without a dedicated security ticket.

Verification:

```bash
rg "run_records|source|session_id|model_id|PostgreSQL is not the session source" src/main/resources src/test/resources doc/architecture.md
```

### MOBILE-PG-RUN-M002 Persist Run State Transitions

Purpose:

Save real run lifecycle states for history/debugging.

Allowed files:

- `src/main/java/com/lqtigee/sparkai/persistence/RunRecordRepository.java`
- `src/main/java/com/lqtigee/sparkai/service/RunService.java`
- `src/test/java/com/lqtigee/sparkai/persistence/RunRecordRepositoryTest.java`
- `src/test/java/com/lqtigee/sparkai/service/RunServiceTest.java`

Implementation:

1. Insert run record after real run id is created.
2. Mark running after process starts.
3. Mark terminal on done/error/stopped.
4. Do not hide process failures.

Stop conditions:

- Stop if database failure would be hidden as run success.

Verification:

```bash
mvn test -Dtest=RunRecordRepositoryTest,RunServiceTest
rg "RunRecordRepository|markRunning|markStopped|markFailed" src/main/java src/test/java
```

## 19. Public Mapping And Evidence Microtasks

### MOBILE-PUBLIC-M001 Rebuild Public App After Chat Console

Purpose:

Serve the latest phone chat console bundle from public `20261`.

Allowed files:

- `doc/audit/public-access.md`

Implementation:

1. Build frontend.
2. Package backend.
3. Restart local user service.
4. Verify public `/api/health`.
5. Verify public `/sessions`.
6. Verify authenticated public `/api/sessions`.
7. Record asset names and source counts only.
8. Do not print token.
9. Do not claim Android installability for plain HTTP fixed IP.

Stop conditions:

- Stop if public endpoint is not mapped to local Java service.

Verification:

```bash
rg "MOBILE-PUBLIC-M001|public 20261|CODEX|OPENCODE|NOT_ANDROID_INSTALLABILITY" doc/audit/public-access.md
```

### MOBILE-PUBLIC-M002 Verify Public Transcript Paging

Purpose:

Prove public URL returns newest-10 transcript page and older page cursor without transcript text in docs.

Allowed files:

- `doc/audit/public-access.md`

Implementation:

1. Call public authenticated `/api/sessions`.
2. Pick one real session id.
3. Call public transcript endpoint with `limit=10`.
4. Record message count only.
5. If `hasMoreBefore`, call older page.
6. Record page info fields only.
7. Do not record message text.

Stop conditions:

- Stop if transcript endpoint dumps all messages on first page.

Verification:

```bash
rg "MOBILE-PUBLIC-M002|limit=10|hasMoreBefore|message count|no transcript text" doc/audit/public-access.md
```

### MOBILE-PUBLIC-M003 Verify Public Inline SSE Run

Purpose:

Prove phone-facing public route can start a real selected-session run and see SSE events.

Allowed files:

- `doc/audit/chat-control-live-evidence.md`
- `doc/audit/release-checklist-status.md`

Implementation:

1. Use authenticated public API.
2. Select real session and supported real model.
3. Start a run with a safe prompt that contains no secret.
4. Subscribe to SSE.
5. Record event types only.
6. Record terminal count and terminal type.
7. Stop if needed.
8. Do not record prompt text or transcript text.

Stop conditions:

- Stop if SSE cannot be observed from public route.

Verification:

```bash
rg "MOBILE-PUBLIC-M003|SSE|runId|terminal count|event types|no transcript text" doc/audit/chat-control-live-evidence.md doc/audit/release-checklist-status.md
```

## 20. End-To-End No-Regression Microtasks

### MOBILE-E2E-M001 Local Current Sessions Evidence

Purpose:

Use current local sessions as the primary evidence source, not smoke fixtures.

Allowed files:

- `doc/audit/mobile-chat-e2e.md`

Implementation:

1. Verify local `/api/health`.
2. Verify authenticated `/api/sessions`.
3. Record source counts.
4. Verify one transcript page for each source if available.
5. Record message counts only.
6. No smoke-only verification.

Stop conditions:

- Stop if current sessions cannot be read.

Verification:

```bash
test -f doc/audit/mobile-chat-e2e.md
rg "MOBILE-E2E-M001|No smoke|session counts|CODEX|OPENCODE|message counts" doc/audit/mobile-chat-e2e.md
```

### MOBILE-E2E-M002 Frontend No Mock Audit

Purpose:

Ensure the phone UI contains no fake sessions, models, messages, or events.

Allowed files:

- `doc/audit/mobile-chat-e2e.md`

Implementation:

1. Run frontend build.
2. Search frontend source for forbidden mock terms.
3. Record results.
4. Any business-data fake is FAIL.

Stop conditions:

- Stop if fake business data is found.

Verification:

```bash
cd frontend && npm run build
! rg "fake session|mock session|sample session|fake model|mock model|sample message|fake event|mock event" frontend/src
rg "MOBILE-E2E-M002|No mock|PASS|FAIL" doc/audit/mobile-chat-e2e.md
```

### MOBILE-E2E-M003 Mobile Viewport Evidence

Purpose:

Verify chat input, history, and stream UI are usable at phone width.

Allowed files:

- `doc/audit/mobile-chat-e2e.md`

Implementation:

1. Use a real built frontend.
2. Inspect 320px and 390px widths.
3. Confirm bottom composer visible.
4. Confirm history is scrollable.
5. Confirm inline stream area visible after run starts.
6. Confirm no horizontal overflow.

Stop conditions:

- Stop if input is hidden or stream output is only on `/runs`.

Verification:

```bash
rg "MOBILE-E2E-M003|320px|390px|bottom composer|inline stream|no horizontal overflow" doc/audit/mobile-chat-e2e.md
```

## 21. Safe Execution Order

Use this order unless the user explicitly changes priority:

1. `MOBILE-CONTRACT-M001`
2. `MOBILE-BE-PAGE-M001`
3. `MOBILE-BE-PAGE-M002`
4. `MOBILE-BE-PAGE-M003`
5. `MOBILE-BE-PAGE-M004`
6. `MOBILE-FE-PAGE-M001`
7. `MOBILE-FE-PAGE-M002`
8. `MOBILE-FE-PAGE-M003`
9. `MOBILE-FE-PAGE-M004`
10. `MOBILE-SESSION-M001`
11. `MOBILE-SESSION-M002`
12. `MOBILE-COMPOSER-M001`
13. `MOBILE-COMPOSER-M002`
14. `MOBILE-COMPOSER-M003`
15. `MOBILE-STREAM-M001`
16. `MOBILE-COMPOSER-M004`
17. `MOBILE-STREAM-M002`
18. `MOBILE-STREAM-M003`
19. `MOBILE-STREAM-M004`
20. `MOBILE-STREAM-M005`
21. `MOBILE-CONTRACT-M002`
22. `MOBILE-CAP-M001`
23. `MOBILE-CAP-M002`
24. `MOBILE-CODEX-M001`
25. `MOBILE-OPENCODE-M001`
26. Source-specific controls and attachments
27. Session management
28. Public evidence
29. E2E evidence

The first safe implementation task after this document is:

```text
MOBILE-CONTRACT-M001 Add Paged Transcript Contract
```

## 22. Known Risk Prediction

High-risk loops and prevention:

| Risk | Loop | Stop rule |
| --- | --- | --- |
| Transcript dumps all messages | UI becomes unusable, AI patches CSS repeatedly | Add backend paging first |
| Composer fakes prompt message | UI looks successful while backend failed | Never mutate transcript locally |
| SSE only works on `/runs` | Phone chat still cannot show progress | Inline stream task must render in `SessionChatComposer` |
| CLI option not supported | Command exits with usage error, UI is blamed | Record local CLI help before mapping |
| opencode agents faked | UI selects invalid agent | Agent list must be real or typed error |
| Attachment raw path accepted | Remote path injection risk | Upload endpoint returns server-owned attachment ids |
| Public server reads its own sessions | Phone controls wrong machine | Public evidence must show local source counts |
| PostgreSQL used for sessions | Stale DB replaces live sessions | PostgreSQL is Lqtigee-owned persistence only |

Predicted unrecoverable loop risk:

```text
Low if tasks are executed in order and one at a time.
High if composer, streaming, CLI options, attachments, and session management are built in one broad pass.
```
