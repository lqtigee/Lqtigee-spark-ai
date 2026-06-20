import { useCallback, useEffect, useRef, useState } from "react";
import { openRunEvents, startRun, stopRun } from "../api/remoteApi";
import type { RunEventDto, StartRunRequest } from "../types/api";

const TERMINAL_EVENT_TYPES = new Set(["done", "error", "stopped"]);

interface RunEventStreamRef {
  close(): void;
}

interface SessionChatRunState {
  starting: boolean;
  streaming: boolean;
  stopping: boolean;
  terminal: RunEventDto | null;
  error: unknown;
  runId: string;
  events: RunEventDto[];
  startSessionRun(request: StartRunRequest): Promise<string | null>;
  stopActiveRun(): Promise<void>;
}

export function useSessionChatRunState(): SessionChatRunState {
  const [starting, setStarting] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const [stopping, setStopping] = useState(false);
  const [terminal, setTerminal] = useState<RunEventDto | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [runId, setRunId] = useState("");
  const [events, setEvents] = useState<RunEventDto[]>([]);
  const streamRef = useRef<RunEventStreamRef | null>(null);

  const closeActiveStream = useCallback(() => {
    streamRef.current?.close();
    streamRef.current = null;
  }, []);

  const startSessionRun = useCallback(async (request: StartRunRequest) => {
    closeActiveStream();
    setStarting(true);
    setStreaming(false);
    setTerminal(null);
    setError(null);
    setEvents([]);

    try {
      const response = await startRun(request);
      setRunId(response.runId);
      setStreaming(true);
      streamRef.current = openRunEvents(response.runId, {
        onEvent(event) {
          setEvents((currentEvents) => [...currentEvents, event]);
          if (TERMINAL_EVENT_TYPES.has(event.type)) {
            setTerminal(event);
            setStreaming(false);
            streamRef.current = null;
          }
        },
        onError(caughtError) {
          setError(caughtError);
          setStreaming(false);
          streamRef.current = null;
        }
      });
      return response.runId;
    } catch (caughtError) {
      setError(caughtError);
      setStreaming(false);
      return null;
    } finally {
      setStarting(false);
    }
  }, [closeActiveStream]);

  const stopActiveRun = useCallback(async () => {
    if (!runId || terminal || stopping) {
      return;
    }

    setStopping(true);
    setError(null);
    try {
      await stopRun(runId);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setStopping(false);
    }
  }, [runId, stopping, terminal]);

  useEffect(() => closeActiveStream, [closeActiveStream]);

  return {
    starting,
    streaming,
    stopping,
    terminal,
    error,
    runId,
    events,
    startSessionRun,
    stopActiveRun
  };
}
