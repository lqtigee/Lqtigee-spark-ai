import { useCallback, useState } from "react";
import { listSessions, refreshSessions } from "../api/remoteApi";
import type { AgentSource, RemoteSession, SelectedSessionRef } from "../types/api";

const SELECTED_SESSION_SOURCE_KEY = "lqtigee_selected_session_source";
const SELECTED_SESSION_ID_KEY = "lqtigee_selected_session_id";

interface SessionsState {
  loading: boolean;
  loaded: boolean;
  error: unknown;
  refreshError: unknown;
  sessions: RemoteSession[];
  selectedSessionRef: SelectedSessionRef | null;
  loadSessions(): Promise<void>;
  refreshSessionRefs(refs: SelectedSessionRef[]): Promise<void>;
  selectSession(session: RemoteSession | null): void;
  clearSelectedSession(): void;
}

export function useSessionsState(): SessionsState {
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [refreshError, setRefreshError] = useState<unknown>(null);
  const [sessions, setSessions] = useState<RemoteSession[]>([]);
  const [selectedSessionRef, setSelectedSessionRef] = useState<SelectedSessionRef | null>(() => readPersistedSelectedSessionRef());

  const loadSessions = useCallback(async () => {
    setLoading(true);
    setError(null);
    setRefreshError(null);

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
      setRefreshError(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const refreshSessionRefs = useCallback(async (refs: SelectedSessionRef[]) => {
    const uniqueRefs = uniqueSessionRefs(refs);
    if (uniqueRefs.length === 0) {
      return;
    }

    const requestedKeys = new Set(uniqueRefs.map(sessionRefKey));
    setRefreshError(null);

    try {
      const response = await refreshSessions(uniqueRefs);
      const refreshedByKey = new Map(response.sessions.map((session) => [sessionKey(session), session]));
      const returnedKeys = new Set(refreshedByKey.keys());

      setSessions((currentSessions) =>
        currentSessions.flatMap((session) => {
          const key = sessionKey(session);
          if (!requestedKeys.has(key)) {
            return [session];
          }
          const refreshedSession = refreshedByKey.get(key);
          return refreshedSession ? [refreshedSession] : [];
        })
      );
      setSelectedSessionRef((currentSessionRef) => {
        if (!currentSessionRef || !requestedKeys.has(sessionRefKey(currentSessionRef)) || returnedKeys.has(sessionRefKey(currentSessionRef))) {
          return currentSessionRef;
        }
        clearPersistedSelectedSessionRef();
        return null;
      });
    } catch (caughtError) {
      setRefreshError(caughtError);
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
    refreshError,
    sessions,
    selectedSessionRef,
    loadSessions,
    refreshSessionRefs,
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

function uniqueSessionRefs(refs: SelectedSessionRef[]): SelectedSessionRef[] {
  const refsByKey = new Map<string, SelectedSessionRef>();
  refs.forEach((ref) => {
    const trimmedId = ref.id.trim();
    if (trimmedId) {
      refsByKey.set(`${ref.source}:${trimmedId}`, { source: ref.source, id: trimmedId });
    }
  });
  return Array.from(refsByKey.values());
}

function sessionKey(session: RemoteSession): string {
  return `${session.source}:${session.id}`;
}

function sessionRefKey(ref: SelectedSessionRef): string {
  return `${ref.source}:${ref.id.trim()}`;
}

function isAgentSource(value: string | null): value is AgentSource {
  return value === "CODEX" || value === "OPENCODE";
}
