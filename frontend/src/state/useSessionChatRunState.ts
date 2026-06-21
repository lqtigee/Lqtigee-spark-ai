import { useCallback, useEffect, useRef, useState } from "react";
import { openRunEvents, startRun, stopRun } from "../api/remoteApi";
import type { RunEventDto, StartRunRequest } from "../types/api";

const TERMINAL_EVENT_TYPES = new Set(["done", "error", "stopped"]);

interface RunEventStreamRef {
  close(): void;
}

interface ActiveSessionRef {
  source: StartRunRequest["source"];
  id: string;
}

interface SessionChatRunState {
  starting: boolean;
  streaming: boolean;
  stopping: boolean;
  terminal: RunEventDto | null;
  error: unknown;
  runId: string;
  events: RunEventDto[];
  activeSessionRef: ActiveSessionRef | null;
  nonTerminal: boolean;
  startSessionRun(request: StartRunRequest, activeSessionRef: ActiveSessionRef, onTerminal?: (event: RunEventDto) => void): Promise<string | null>;
  stopActiveRun(): Promise<void>;
  clearRun(): void;
}

export function useSessionChatRunState(): SessionChatRunState {
  const [starting, setStarting] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const [stopping, setStopping] = useState(false);
  const [terminal, setTerminal] = useState<RunEventDto | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [runId, setRunId] = useState("");
  const [events, setEvents] = useState<RunEventDto[]>([]);
  const [activeSessionRef, setActiveSessionRef] = useState<ActiveSessionRef | null>(null);
  const streamRef = useRef<RunEventStreamRef | null>(null);
  const runBusyRef = useRef(false);
  const stopInFlightRef = useRef(false);
  const terminalCallbackCalledRef = useRef(false);
  const nonTerminal = starting || Boolean(runId && !terminal);

  const closeActiveStream = useCallback(() => {
    streamRef.current?.close();
    streamRef.current = null;
  }, []);

  const clearRun = useCallback(() => {
    closeActiveStream();
    runBusyRef.current = false;
    stopInFlightRef.current = false;
    setStarting(false);
    setStreaming(false);
    setStopping(false);
    setTerminal(null);
    setError(null);
    setRunId("");
    setEvents([]);
    setActiveSessionRef(null);
  }, [closeActiveStream]);

  const startSessionRun = useCallback(async (request: StartRunRequest, nextActiveSessionRef: ActiveSessionRef, onTerminal?: (event: RunEventDto) => void) => {
    if (runBusyRef.current || starting || nonTerminal) {
      setError(new Error(starting ? "A chat run is already starting" : "A non-terminal run is already active"));
      return null;
    }

    closeActiveStream();
    runBusyRef.current = true;
    setStarting(true);
    setStreaming(false);
    setTerminal(null);
    setError(null);
    setEvents([]);
    setActiveSessionRef(nextActiveSessionRef);
    terminalCallbackCalledRef.current = false;

    try {
      const response = await startRun(request);
      setRunId(response.runId);
      setStreaming(true);
      streamRef.current = openRunEvents(response.runId, {
        onEvent(event) {
          setEvents((currentEvents) => [...currentEvents, event]);
          if (TERMINAL_EVENT_TYPES.has(event.type)) {
            runBusyRef.current = false;
            setTerminal(event);
            setStreaming(false);
            streamRef.current = null;
            if (!terminalCallbackCalledRef.current) {
              terminalCallbackCalledRef.current = true;
              onTerminal?.(event);
            }
          }
        },
        onError(caughtError) {
          runBusyRef.current = false;
          setError(caughtError);
          setStreaming(false);
          setRunId("");
          setActiveSessionRef(null);
          streamRef.current = null;
        }
      });
      return response.runId;
    } catch (caughtError) {
      runBusyRef.current = false;
      setError(caughtError);
      setStreaming(false);
      setActiveSessionRef(null);
      return null;
    } finally {
      setStarting(false);
    }
  }, [closeActiveStream, nonTerminal, starting]);

  const stopActiveRun = useCallback(async () => {
    if (!runId || terminal || stopping || stopInFlightRef.current) {
      return;
    }

    stopInFlightRef.current = true;
    setStopping(true);
    setError(null);
    try {
      await stopRun(runId);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      stopInFlightRef.current = false;
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
    activeSessionRef,
    nonTerminal,
    startSessionRun,
    stopActiveRun,
    clearRun
  };
}
