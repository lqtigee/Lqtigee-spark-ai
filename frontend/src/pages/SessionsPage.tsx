import { useEffect } from "react";
import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { SessionCard } from "../components/SessionCard";
import { SessionDetail } from "../components/SessionDetail";
import { useSessionsState } from "../state/useSessionsState";

export function SessionsPage() {
  const sessionsState = useSessionsState();
  const selectedSession = sessionsState.sessions.find((session) => session.id === sessionsState.selectedSessionId);

  useEffect(() => {
    void sessionsState.loadSessions();
  }, [sessionsState.loadSessions]);

  return (
    <section>
      <h2>Sessions</h2>
      {sessionsState.loading ? <LoadingBlock label="Loading sessions" /> : null}
      {sessionsState.error ? <ErrorPanel title="Sessions error" error={sessionsState.error} /> : null}
      {sessionsState.loaded && !sessionsState.error && sessionsState.sessions.length === 0 ? <p>No sessions found</p> : null}
      {sessionsState.sessions.length > 0 ? (
        <div>
          <div>
            {sessionsState.sessions.map((session) => (
              <SessionCard
                key={session.id}
                onSelect={sessionsState.selectSession}
                selected={session.id === sessionsState.selectedSessionId}
                session={session}
              />
            ))}
          </div>
          <SessionDetail session={selectedSession} />
        </div>
      ) : null}
    </section>
  );
}
