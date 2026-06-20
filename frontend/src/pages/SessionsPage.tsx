import { useEffect, useMemo, useRef, useState } from "react";
import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { SessionCard } from "../components/SessionCard";
import { SessionDetail } from "../components/SessionDetail";
import { useSessionChatRunState } from "../state/useSessionChatRunState";
import { useSessionTranscriptState } from "../state/useSessionTranscriptState";
import { useSessionsState } from "../state/useSessionsState";
import type { AgentSource, RemoteSession, RunEventDto, StartRunRequest } from "../types/api";

const TOKEN_KEY = "lqtigee_token";

type SourceFilter = "ALL" | AgentSource;

interface SelectedSessionRef {
  source: AgentSource;
  id: string;
}

export function SessionsPage() {
  const sessionsState = useSessionsState();
  const transcriptState = useSessionTranscriptState();
  const chatRunState = useSessionChatRunState();
  const selectedSessionRef = useRef<SelectedSessionRef | null>(null);
  const hasToken = Boolean((localStorage.getItem(TOKEN_KEY) ?? "").trim());
  const [query, setQuery] = useState("");
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>("ALL");
  const filteredSessions = useMemo(
    () => filterSessions(sessionsState.sessions, sourceFilter, query),
    [query, sessionsState.sessions, sourceFilter]
  );
  const selectedSession = sessionsState.sessions.find((session) => session.id === sessionsState.selectedSessionId);
  const chatRunBelongsToSelectedSession = Boolean(
    selectedSession &&
      chatRunState.activeSessionRef &&
      chatRunState.activeSessionRef.source === selectedSession.source &&
      chatRunState.activeSessionRef.id === selectedSession.id
  );
  const counts = useMemo(() => countSessions(sessionsState.sessions), [sessionsState.sessions]);

  useEffect(() => {
    if (hasToken) {
      void sessionsState.loadSessions();
    }
  }, [hasToken, sessionsState.loadSessions]);

  useEffect(() => {
    if (selectedSession) {
      selectedSessionRef.current = { source: selectedSession.source, id: selectedSession.id };
      void transcriptState.loadNewestTranscript(selectedSession.source, selectedSession.id);
    } else {
      selectedSessionRef.current = null;
      transcriptState.clearTranscript();
    }
  }, [selectedSession?.id, selectedSession?.source, transcriptState.loadNewestTranscript, transcriptState.clearTranscript]);

  function handleSelectSession(sessionId: string) {
    sessionsState.selectSession(sessionId);
  }

  function handleBack() {
    sessionsState.selectSession("");
    transcriptState.clearTranscript();
  }

  async function handleStartChatRun(request: StartRunRequest): Promise<string | null> {
    const startedSessionRef = selectedSession ? { source: selectedSession.source, id: selectedSession.id } : null;
    if (!startedSessionRef) {
      return null;
    }

    return chatRunState.startSessionRun(
      request,
      startedSessionRef,
      (event: RunEventDto) => {
        void handleTerminalChatRun(event, startedSessionRef);
      }
    );
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
      transcriptState.loadNewestTranscript(startedSessionRef.source, startedSessionRef.id),
      sessionsState.loadSessions()
    ]);
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Codex / opencode</p>
          <h2>会话</h2>
        </div>
        <button className="button button--secondary" disabled={!hasToken || sessionsState.loading} onClick={() => void sessionsState.loadSessions()} type="button">
          刷新
        </button>
      </div>

      {!hasToken ? (
        <section className="action-panel action-panel--warning">
          <div>
            <h3>需要令牌</h3>
            <p>保存 API 令牌后才能加载实时会话。</p>
          </div>
          <a className="button button--primary" href="/settings">
            设置
          </a>
        </section>
      ) : null}

      {hasToken ? (
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
      {sessionsState.loaded && !sessionsState.error && sessionsState.sessions.length === 0 ? <p className="empty-state">未找到会话</p> : null}
      {sessionsState.loaded && !sessionsState.error && sessionsState.sessions.length > 0 && filteredSessions.length === 0 ? (
        <p className="empty-state">没有匹配当前筛选条件的会话</p>
      ) : null}
      {filteredSessions.length > 0 ? (
        <div className={selectedSession ? "sessions-layout sessions-layout--chat-open" : "sessions-layout"}>
          <div className="session-grid">
            {filteredSessions.map((session) => (
              <SessionCard
                key={session.id}
                onSelect={handleSelectSession}
                selected={session.id === sessionsState.selectedSessionId}
                session={session}
              />
            ))}
          </div>
          <SessionDetail
            chatRunError={chatRunBelongsToSelectedSession ? chatRunState.error : null}
            chatRunEvents={chatRunBelongsToSelectedSession ? chatRunState.events : []}
            chatRunId={chatRunBelongsToSelectedSession ? chatRunState.runId : ""}
            chatRunNonTerminal={chatRunBelongsToSelectedSession && chatRunState.nonTerminal}
            chatRunStarting={chatRunState.starting}
            chatRunStopping={chatRunState.stopping}
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
