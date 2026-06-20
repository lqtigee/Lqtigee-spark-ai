import { useCallback, useState } from "react";
import { getSessionTranscript } from "../api/remoteApi";
import type { AgentSource, SessionTranscriptDto } from "../types/api";

interface SessionTranscriptState {
  loading: boolean;
  loaded: boolean;
  error: unknown;
  transcript: SessionTranscriptDto | null;
  loadTranscript(source: AgentSource, id: string): Promise<void>;
  clearTranscript(): void;
}

export function useSessionTranscriptState(): SessionTranscriptState {
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [transcript, setTranscript] = useState<SessionTranscriptDto | null>(null);

  const loadTranscript = useCallback(async (source: AgentSource, id: string) => {
    setLoading(true);
    setLoaded(false);
    setError(null);

    try {
      setTranscript(await getSessionTranscript(source, id));
      setLoaded(true);
    } catch (caughtError) {
      setTranscript(null);
      setError(caughtError);
    } finally {
      setLoading(false);
    }
  }, []);

  const clearTranscript = useCallback(() => {
    setLoading(false);
    setLoaded(false);
    setError(null);
    setTranscript(null);
  }, []);

  return {
    loading,
    loaded,
    error,
    transcript,
    loadTranscript,
    clearTranscript
  };
}
