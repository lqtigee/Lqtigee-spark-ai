import { useCallback, useState } from "react";
import { listSessions } from "../api/remoteApi";
import type { RemoteSession } from "../types/api";

interface SessionsState {
  loading: boolean;
  error: unknown;
  sessions: RemoteSession[];
  selectedSessionId: string;
  loadSessions(): Promise<void>;
  selectSession(sessionId: string): void;
}

export function useSessionsState(): SessionsState {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [sessions, setSessions] = useState<RemoteSession[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");

  const loadSessions = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await listSessions();
      setSessions(response.sessions);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setLoading(false);
    }
  }, []);

  const selectSession = useCallback((sessionId: string) => {
    setSelectedSessionId(sessionId);
  }, []);

  return {
    loading,
    error,
    sessions,
    selectedSessionId,
    loadSessions,
    selectSession
  };
}
