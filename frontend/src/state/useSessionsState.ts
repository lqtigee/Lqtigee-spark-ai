import { useCallback, useState } from "react";
import { listSessions } from "../api/remoteApi";
import type { RemoteSession } from "../types/api";

const SELECTED_SESSION_KEY = "lqtigee_selected_session_id";

interface SessionsState {
  loading: boolean;
  loaded: boolean;
  error: unknown;
  sessions: RemoteSession[];
  selectedSessionId: string;
  loadSessions(): Promise<void>;
  selectSession(sessionId: string): void;
}

export function useSessionsState(): SessionsState {
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [sessions, setSessions] = useState<RemoteSession[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState(() => localStorage.getItem(SELECTED_SESSION_KEY) ?? "");

  const loadSessions = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await listSessions();
      setSessions(response.sessions);
      setSelectedSessionId((currentSessionId) => {
        if (!currentSessionId) {
          return "";
        }
        const stillExists = response.sessions.some((session) => session.id === currentSessionId);
        if (!stillExists) {
          localStorage.removeItem(SELECTED_SESSION_KEY);
          return "";
        }
        return currentSessionId;
      });
      setLoaded(true);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setLoading(false);
    }
  }, []);

  const selectSession = useCallback((sessionId: string) => {
    if (sessionId) {
      localStorage.setItem(SELECTED_SESSION_KEY, sessionId);
    } else {
      localStorage.removeItem(SELECTED_SESSION_KEY);
    }
    setSelectedSessionId(sessionId);
  }, []);

  return {
    loading,
    loaded,
    error,
    sessions,
    selectedSessionId,
    loadSessions,
    selectSession
  };
}
