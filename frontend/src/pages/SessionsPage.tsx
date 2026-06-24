import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { SessionCard } from "../components/SessionCard";
import { SessionDetail } from "../components/SessionDetail";
import { startSessionAction } from "../api/remoteApi";
import { useSessionChatRunState } from "../state/useSessionChatRunState";
import { useSessionTranscriptState } from "../state/useSessionTranscriptState";
import { useSessionsState } from "../state/useSessionsState";
import type { StartSessionRunResult } from "../state/useSessionChatRunState";
import type { AgentSource, RemoteSession, RunEventDto, SelectedSessionRef, SessionActionResponse, StartRunRequest } from "../types/api";

const TOKEN_KEY = "lqtigee_token";
const RUNNING_SESSION_REFRESH_MS = 2500;
const RUNNING_SESSION_AUTO_REFRESH_WINDOW_MS = 30 * 60 * 1000;

type SourceFilter = "ALL" | AgentSource;

export function SessionsPage() {
  const sessionsState = useSessionsState();
  const transcriptState = useSessionTranscriptState();
  const chatRunState = useSessionChatRunState();
  const selectedSessionRef = useRef<SelectedSessionRef | null>(null);
  const hasToken = Boolean((localStorage.getItem(TOKEN_KEY) ?? "").trim());
  const [query, setQuery] = useState("");
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>("ALL");
  const [actionInFlightSessionRef, setActionInFlightSessionRef] = useState<SelectedSessionRef | null>(null);
  const [actionResult, setActionResult] = useState<SessionActionResponse | null>(null);
  const [actionError, setActionError] = useState<unknown>(null);
  const canRenderSessions = hasToken && sessionsState.loaded && !sessionsState.error;
  const filteredSessions = useMemo(
    () => filterSessions(sessionsState.sessions, sourceFilter, query),
    [query, sessionsState.sessions, sourceFilter]
  );
  const selectedSession = sessionsState.sessions.find((session) => isSelectedSession(session, sessionsState.selectedSessionRef));
  const chatOpen = Boolean(selectedSession);
  const showSessionList = !chatOpen;
  const runningSessionRefs = useMemo(
    () =>
      sessionsState.sessions
        .filter((session) => session.status === "RUNNING" && isRecentlyUpdated(session.updatedAt))
        .map((session) => ({ source: session.source, id: session.id })),
    [sessionsState.sessions]
  );
  const hasRunningSession = runningSessionRefs.length > 0;
  const selectedSessionIsRunning = selectedSession?.status === "RUNNING";
  const canRenderSessionLayout = canRenderSessions && (filteredSessions.length > 0 || chatOpen);
  const visibleSessionCards = showSessionList ? filteredSessions : [];
  const chatRunBusy = chatRunState.starting || chatRunState.nonTerminal;
  const chatRunBelongsToSelectedSession = Boolean(
    selectedSession &&
      chatRunState.activeSessionRef &&
      chatRunState.activeSessionRef.source === selectedSession.source &&
      chatRunState.activeSessionRef.id === selectedSession.id
  );
  const chatRunOtherSessionNonTerminal = Boolean(
    selectedSession &&
      chatRunBusy &&
      chatRunState.activeSessionRef &&
      !chatRunBelongsToSelectedSession
  );
  const selectedChatRunError = chatRunBelongsToSelectedSession && chatRunState.error ? chatRunState.error : null;
  const selectedActionInFlight = Boolean(
    selectedSession &&
      actionInFlightSessionRef &&
      isSameSessionRef(actionInFlightSessionRef, { source: selectedSession.source, id: selectedSession.id })
  );
  const counts = useMemo(() => countSessions(sessionsState.sessions), [sessionsState.sessions]);
  const sessionsPollInFlightRef = useRef(false);
  const transcriptPollInFlightRef = useRef(false);

  useEffect(() => {
    if (hasToken) {
      void sessionsState.loadSessions();
    }
  }, [hasToken, sessionsState.loadSessions]);

  useEffect(() => {
    if (!hasToken || chatOpen || !hasRunningSession) {
      return;
    }

    const intervalId = window.setInterval(() => {
      if (sessionsPollInFlightRef.current) {
        return;
      }
      sessionsPollInFlightRef.current = true;
      void sessionsState.refreshSessionRefs(runningSessionRefs).finally(() => {
        sessionsPollInFlightRef.current = false;
      });
    }, RUNNING_SESSION_REFRESH_MS);

    return () => window.clearInterval(intervalId);
  }, [chatOpen, hasToken, hasRunningSession, runningSessionRefs, sessionsState.refreshSessionRefs]);

  useEffect(() => {
    if (!hasToken || !selectedSession || !selectedSessionIsRunning) {
      return;
    }

    const intervalId = window.setInterval(() => {
      if (transcriptPollInFlightRef.current) {
        return;
      }
      transcriptPollInFlightRef.current = true;
      void Promise.all([
        sessionsState.refreshSessionRefs([{ source: selectedSession.source, id: selectedSession.id }]),
        transcriptState.refreshNewestTranscript(selectedSession.source, selectedSession.id)
      ]).finally(() => {
          transcriptPollInFlightRef.current = false;
        });
    }, RUNNING_SESSION_REFRESH_MS);

    return () => window.clearInterval(intervalId);
  }, [
    hasToken,
    selectedSession?.id,
    selectedSession?.source,
    selectedSessionIsRunning,
    sessionsState.refreshSessionRefs,
    transcriptState.refreshNewestTranscript
  ]);

  useEffect(() => {
    if (!sessionsState.loaded || sessionsState.error) {
      return;
    }

    const hasSelectedQuery = hasSelectedSessionQuery(window.location.search);
    const selectedQueryRef = readSelectedSessionQuery(window.location.search);
    if (!selectedQueryRef) {
      if (hasSelectedQuery) {
        sessionsState.clearSelectedSession();
        writeSelectedSessionQuery(null);
      }
      return;
    }

    const realSession = sessionsState.sessions.find((session) => isSelectedSession(session, selectedQueryRef));
    if (!realSession) {
      sessionsState.clearSelectedSession();
      writeSelectedSessionQuery(null);
      return;
    }
    if (!isSelectedSession(realSession, sessionsState.selectedSessionRef)) {
      sessionsState.selectSession(realSession);
    }
  }, [
    sessionsState.loaded,
    sessionsState.error,
    sessionsState.sessions,
    sessionsState.selectedSessionRef,
    sessionsState.selectSession,
    sessionsState.clearSelectedSession
  ]);

  useEffect(() => {
    if (selectedSession) {
      selectedSessionRef.current = { source: selectedSession.source, id: selectedSession.id };
      void transcriptState.loadNewestTranscript(selectedSession.source, selectedSession.id);
    } else {
      selectedSessionRef.current = null;
      transcriptState.clearTranscript();
    }
  }, [selectedSession?.id, selectedSession?.source, transcriptState.loadNewestTranscript, transcriptState.clearTranscript]);

  useEffect(() => {
    setActionResult(null);
    setActionError(null);
  }, [selectedSession?.id, selectedSession?.source]);

  const handleSelectSession = useCallback((session: RemoteSession) => {
    sessionsState.selectSession(session);
    writeSelectedSessionQuery({ source: session.source, id: session.id });
  }, [sessionsState.selectSession]);

  function handleBack() {
    sessionsState.clearSelectedSession();
    transcriptState.clearTranscript();
    writeSelectedSessionQuery(null);
  }

  async function handleStartChatRun(request: StartRunRequest): Promise<StartSessionRunResult> {
    const startedSessionRef = selectedSession ? { source: selectedSession.source, id: selectedSession.id } : null;
    if (!startedSessionRef) {
      return { status: "REJECTED" };
    }

    const startResult = await chatRunState.startSessionRun(
      request,
      startedSessionRef,
      (event: RunEventDto) => {
        void handleTerminalChatRun(event, startedSessionRef);
      }
    );
    if (startResult.status === "STARTED") {
      void sessionsState.refreshSessionRefs([startedSessionRef]);
    }
    return startResult;
  }

  async function handleStartSessionAction(action: string, confirmDestructive: boolean): Promise<void> {
    const targetSessionRef = selectedSession ? { source: selectedSession.source, id: selectedSession.id } : null;
    if (!targetSessionRef || isSameSessionRef(actionInFlightSessionRef, targetSessionRef)) {
      return;
    }

    setActionInFlightSessionRef(targetSessionRef);
    setActionResult(null);
    setActionError(null);

    try {
      const response = await startSessionAction(targetSessionRef.source, targetSessionRef.id, {
        action,
        confirmDestructive
      });
      if (isCurrentSelectedSession(targetSessionRef)) {
        setActionResult(response);
      }
      await sessionsState.loadSessions();
    } catch (caughtError) {
      if (isCurrentSelectedSession(targetSessionRef)) {
        setActionError(caughtError);
      }
    } finally {
      setActionInFlightSessionRef((currentActionSessionRef) =>
        isSameSessionRef(currentActionSessionRef, targetSessionRef) ? null : currentActionSessionRef
      );
    }
  }

  async function handleTerminalChatRun(_event: RunEventDto, startedSessionRef: SelectedSessionRef): Promise<void> {
    const currentSessionRef = selectedSessionRef.current;
    if (
      !currentSessionRef ||
      currentSessionRef.source !== startedSessionRef.source ||
      currentSessionRef.id !== startedSessionRef.id
    ) {
      return;
    }

    await Promise.all([
      transcriptState.loadTranscript(startedSessionRef.source, startedSessionRef.id),
      sessionsState.refreshSessionRefs([startedSessionRef])
    ]);
  }

  function isCurrentSelectedSession(sessionRef: SelectedSessionRef): boolean {
    const currentSessionRef = selectedSessionRef.current;
    return Boolean(
      currentSessionRef &&
        currentSessionRef.source === sessionRef.source &&
        currentSessionRef.id === sessionRef.id
    );
  }

  return (
    <section className={chatOpen ? "page-stack page-stack--chat-open" : "page-stack"}>
      {!chatOpen ? (
      <div className="page-heading">
        <div>
          <p className="eyebrow">Codex / opencode</p>
          <h2>会话</h2>
        </div>
        <button className="button button--secondary" disabled={!hasToken || sessionsState.loading} onClick={() => void sessionsState.loadSessions()} type="button">
          刷新
        </button>
      </div>
      ) : null}

      {!hasToken ? (
        <section className="action-panel action-panel--warning">
          <div>
            <h3>需要令牌</h3>
            <p>保存访问令牌后才能加载实时会话。</p>
          </div>
          <a className="button button--primary" href="/settings">
            设置
          </a>
        </section>
      ) : null}

      {canRenderSessions && !chatOpen ? (
        <div className="filter-bar">
          <label className="field field--compact">
            <span>搜索</span>
            <input
              className="input-control"
              onChange={(event) => setQuery(event.target.value)}
              value={query}
              type="search"
            />
          </label>
          <div className="segmented-control" role="group" aria-label="会话来源筛选">
            <button className={sourceFilter === "ALL" ? "is-active" : ""} onClick={() => setSourceFilter("ALL")} type="button">
              全部 {counts.ALL}
            </button>
            <button className={sourceFilter === "CODEX" ? "is-active" : ""} onClick={() => setSourceFilter("CODEX")} type="button">
              Codex {counts.CODEX}
            </button>
            <button className={sourceFilter === "OPENCODE" ? "is-active" : ""} onClick={() => setSourceFilter("OPENCODE")} type="button">
              opencode {counts.OPENCODE}
            </button>
          </div>
        </div>
      ) : null}

      {sessionsState.loading ? <LoadingBlock label="正在加载会话" /> : null}
      {sessionsState.error ? <ErrorPanel title="会话加载失败" error={sessionsState.error} /> : null}
      {sessionsState.refreshError ? <ErrorPanel title="运行状态刷新失败" error={sessionsState.refreshError} /> : null}
      {canRenderSessions && !chatOpen && sessionsState.sessions.length === 0 ? <p className="empty-state">未找到会话</p> : null}
      {canRenderSessions && !chatOpen && sessionsState.sessions.length > 0 && filteredSessions.length === 0 ? (
        <p className="empty-state">没有匹配当前筛选条件的会话</p>
      ) : null}
      {canRenderSessionLayout ? (
        <div className={selectedSession ? "sessions-layout sessions-layout--chat-open" : "sessions-layout"}>
          {showSessionList ? (
            <div className="session-grid">
              {visibleSessionCards.map((session) => (
                <SessionCard
                  key={`${session.source}:${session.id}`}
                  onSelect={handleSelectSession}
                  selected={isSelectedSession(session, sessionsState.selectedSessionRef)}
                  session={session}
                />
              ))}
            </div>
          ) : null}
          <SessionDetail
            actionError={actionError}
            actionInFlight={selectedActionInFlight}
            actionResult={actionResult}
            chatRunError={selectedChatRunError}
            chatRunEvents={chatRunBelongsToSelectedSession ? chatRunState.events : []}
            chatRunId={chatRunBelongsToSelectedSession ? chatRunState.runId : ""}
            chatRunNonTerminal={chatRunBelongsToSelectedSession && chatRunState.nonTerminal}
            chatRunOtherSessionNonTerminal={chatRunOtherSessionNonTerminal}
            chatRunQueuedRuns={chatRunState.queuedRuns}
            chatRunStarting={chatRunBelongsToSelectedSession && chatRunState.starting}
            chatRunStopping={chatRunBelongsToSelectedSession && chatRunState.stopping}
            chatRunStreaming={chatRunBelongsToSelectedSession && chatRunState.streaming}
            chatRunTerminal={chatRunBelongsToSelectedSession ? chatRunState.terminal : null}
            error={transcriptState.error}
            loaded={transcriptState.loaded}
            loading={transcriptState.loading}
            loadingNewest={transcriptState.loadingNewest}
            loadingOlder={transcriptState.loadingOlder}
            messages={transcriptState.messages}
            onBack={handleBack}
            onLoadOlder={transcriptState.loadOlderMessages}
            onStartAction={handleStartSessionAction}
            pageInfo={transcriptState.pageInfo}
            session={selectedSession}
            onStartChatRun={handleStartChatRun}
            onStopChatRun={chatRunState.stopActiveRun}
            transcript={transcriptState.transcript}
          />
        </div>
      ) : null}
    </section>
  );
}

function isSelectedSession(session: RemoteSession, selectedSessionRef: SelectedSessionRef | null): boolean {
  return Boolean(selectedSessionRef && session.source === selectedSessionRef.source && session.id === selectedSessionRef.id);
}

function isSameSessionRef(left: SelectedSessionRef | null, right: SelectedSessionRef | null): boolean {
  return Boolean(left && right && left.source === right.source && left.id === right.id);
}

function readSelectedSessionQuery(search: string): SelectedSessionRef | null {
  const params = new URLSearchParams(search);
  const source = params.get("source");
  const sessionId = params.get("sessionId");
  if ((source !== "CODEX" && source !== "OPENCODE") || !sessionId?.trim()) {
    return null;
  }
  return {
    source,
    id: sessionId.trim()
  };
}

function hasSelectedSessionQuery(search: string): boolean {
  const params = new URLSearchParams(search);
  return params.has("source") || params.has("sessionId");
}

function writeSelectedSessionQuery(ref: SelectedSessionRef | null): void {
  const nextUrl = new URL(window.location.href);
  if (ref) {
    nextUrl.searchParams.set("source", ref.source);
    nextUrl.searchParams.set("sessionId", ref.id);
  } else {
    nextUrl.searchParams.delete("source");
    nextUrl.searchParams.delete("sessionId");
  }
  window.history.replaceState(null, "", `${nextUrl.pathname}${nextUrl.search}${nextUrl.hash}`);
}

function filterSessions(sessions: RemoteSession[], sourceFilter: SourceFilter, query: string): RemoteSession[] {
  const normalizedQuery = query.trim().toLowerCase();
  return sessions.filter((session) => {
    const sourceMatches = sourceFilter === "ALL" || session.source === sourceFilter;
    const queryMatches =
      !normalizedQuery ||
      [session.title, session.workspace, session.model, session.lastMessage, session.rawFile]
        .join(" ")
        .toLowerCase()
        .includes(normalizedQuery);
    return sourceMatches && queryMatches;
  });
}

function countSessions(sessions: RemoteSession[]): Record<SourceFilter, number> {
  return sessions.reduce<Record<SourceFilter, number>>(
    (counts, session) => {
      counts.ALL += 1;
      counts[session.source] += 1;
      return counts;
    },
    { ALL: 0, CODEX: 0, OPENCODE: 0 }
  );
}

function isRecentlyUpdated(value: string): boolean {
  const updatedAt = new Date(value).getTime();
  if (Number.isNaN(updatedAt)) {
    return false;
  }
  return Date.now() - updatedAt <= RUNNING_SESSION_AUTO_REFRESH_WINDOW_MS;
}
