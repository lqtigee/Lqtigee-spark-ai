import { useEffect, useMemo, useState } from "react";
import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { SessionCard } from "../components/SessionCard";
import { SessionDetail } from "../components/SessionDetail";
import { useSessionChatRunState } from "../state/useSessionChatRunState";
import { useSessionTranscriptState } from "../state/useSessionTranscriptState";
import { useSessionsState } from "../state/useSessionsState";
import type { AgentSource, RemoteSession } from "../types/api";

const TOKEN_KEY = "lqtigee_token";

type SourceFilter = "ALL" | AgentSource;

export function SessionsPage() {
  const sessionsState = useSessionsState();
  const transcriptState = useSessionTranscriptState();
  const chatRunState = useSessionChatRunState();
  const hasToken = Boolean((localStorage.getItem(TOKEN_KEY) ?? "").trim());
  const [query, setQuery] = useState("");
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>("ALL");
  const filteredSessions = useMemo(
    () => filterSessions(sessionsState.sessions, sourceFilter, query),
    [query, sessionsState.sessions, sourceFilter]
  );
  const selectedSession = sessionsState.sessions.find((session) => session.id === sessionsState.selectedSessionId);
  const counts = useMemo(() => countSessions(sessionsState.sessions), [sessionsState.sessions]);

  useEffect(() => {
    if (hasToken) {
      void sessionsState.loadSessions();
    }
  }, [hasToken, sessionsState.loadSessions]);

  useEffect(() => {
    if (selectedSession) {
      void transcriptState.loadNewestTranscript(selectedSession.source, selectedSession.id);
    } else {
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

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Codex / opencode</p>
          <h2>Sessions</h2>
        </div>
        <button className="button button--secondary" disabled={!hasToken || sessionsState.loading} onClick={() => void sessionsState.loadSessions()} type="button">
          Reload
        </button>
      </div>

      {!hasToken ? (
        <section className="action-panel action-panel--warning">
          <div>
            <h3>Token required</h3>
            <p>Live sessions cannot load until the API token is saved.</p>
          </div>
          <a className="button button--primary" href="/settings">
            Settings
          </a>
        </section>
      ) : null}

      {hasToken ? (
        <div className="filter-bar">
          <label className="field field--compact">
            <span>Search</span>
            <input
              className="input-control"
              onChange={(event) => setQuery(event.target.value)}
              value={query}
              type="search"
            />
          </label>
          <div className="segmented-control" role="group" aria-label="Session source filter">
            <button className={sourceFilter === "ALL" ? "is-active" : ""} onClick={() => setSourceFilter("ALL")} type="button">
              All {counts.ALL}
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

      {sessionsState.loading ? <LoadingBlock label="Loading sessions" /> : null}
      {sessionsState.error ? <ErrorPanel title="Sessions error" error={sessionsState.error} /> : null}
      {sessionsState.loaded && !sessionsState.error && sessionsState.sessions.length === 0 ? <p className="empty-state">No sessions found</p> : null}
      {sessionsState.loaded && !sessionsState.error && sessionsState.sessions.length > 0 && filteredSessions.length === 0 ? (
        <p className="empty-state">No sessions match the current filter</p>
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
            chatRunError={chatRunState.error}
            chatRunId={chatRunState.runId}
            chatRunStarting={chatRunState.starting}
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
            onStartChatRun={chatRunState.startSessionRun}
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
