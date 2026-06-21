import { useCallback, useState } from "react";
import { listSessions } from "../api/remoteApi";
import type { AgentSource, RemoteSession, SelectedSessionRef } from "../types/api";

const SELECTED_SESSION_SOURCE_KEY = "lqtigee_selected_session_source";
const SELECTED_SESSION_ID_KEY = "lqtigee_selected_session_id";

interface SessionsState {
  loading: boolean;
  loaded: boolean;
  error: unknown;
  sessions: RemoteSession[];
  selectedSessionRef: SelectedSessionRef | null;
  loadSessions(): Promise<void>;
  selectSession(session: RemoteSession | null): void;
  clearSelectedSession(): void;
}

export function useSessionsState(): SessionsState {
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [sessions, setSessions] = useState<RemoteSession[]>([]);
  const [selectedSessionRef, setSelectedSessionRef] = useState<SelectedSessionRef | null>(() => readPersistedSelectedSessionRef());

  const loadSessions = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await listSessions();
      setSessions(response.sessions);
      setSelectedSessionRef((currentSessionRef) => {
        if (!currentSessionRef) {
          return null;
        }
        const stillExists = response.sessions.some((session) => isSelectedSession(session, currentSessionRef));
        if (!stillExists) {
          clearPersistedSelectedSessionRef();
          return null;
        }
        return currentSessionRef;
      });
      setLoaded(true);
    } catch (caughtError) {
      setSessions([]);
      setLoaded(false);
      setError(caughtError);
    } finally {
      setLoading(false);
    }
  }, []);

  const selectSession = useCallback((session: RemoteSession | null) => {
    if (!session) {
      clearPersistedSelectedSessionRef();
      setSelectedSessionRef(null);
      return;
    }

    const nextSessionRef = { source: session.source, id: session.id };
    persistSelectedSessionRef(nextSessionRef);
    setSelectedSessionRef(nextSessionRef);
  }, []);

  const clearSelectedSession = useCallback(() => {
    clearPersistedSelectedSessionRef();
    setSelectedSessionRef(null);
  }, []);

  return {
    loading,
    loaded,
    error,
    sessions,
    selectedSessionRef,
    loadSessions,
    selectSession,
    clearSelectedSession
  };
}

function readPersistedSelectedSessionRef(): SelectedSessionRef | null {
  const persistedSource = localStorage.getItem(SELECTED_SESSION_SOURCE_KEY);
  const persistedId = localStorage.getItem(SELECTED_SESSION_ID_KEY);
  if (!isAgentSource(persistedSource) || !persistedId) {
    return null;
  }
  return { source: persistedSource, id: persistedId };
}

function persistSelectedSessionRef(sessionRef: SelectedSessionRef) {
  localStorage.setItem(SELECTED_SESSION_SOURCE_KEY, sessionRef.source);
  localStorage.setItem(SELECTED_SESSION_ID_KEY, sessionRef.id);
}

function clearPersistedSelectedSessionRef() {
  localStorage.removeItem(SELECTED_SESSION_SOURCE_KEY);
  localStorage.removeItem(SELECTED_SESSION_ID_KEY);
}

function isSelectedSession(session: RemoteSession, selectedSessionRef: SelectedSessionRef): boolean {
  return session.source === selectedSessionRef.source && session.id === selectedSessionRef.id;
}

function isAgentSource(value: string | null): value is AgentSource {
  return value === "CODEX" || value === "OPENCODE";
}
