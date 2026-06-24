import { useCallback, useEffect, useRef, useState } from "react";
import { openRunWebSocket, stopRun } from "../api/remoteApi";
import type { RunEventDto, StartRunRequest } from "../types/api";

const TERMINAL_EVENT_TYPES = new Set(["done", "error", "stopped"]);

interface RunEventStreamRef {
  close(): void;
}

export interface ActiveSessionRef {
  source: StartRunRequest["source"];
  id: string;
}

export interface QueuedSessionRun {
  id: string;
  activeSessionRef: ActiveSessionRef;
  prompt: string;
  mode: StartRunRequest["mode"];
  modelId: string;
  queuedAt: string;
}

export type StartSessionRunResult =
  | { status: "STARTED"; runId: string }
  | { status: "QUEUED"; queueId: string }
  | { status: "REJECTED" };

type TerminalHandler = (event: RunEventDto) => void | Promise<void>;

interface QueuedSessionRunInternal extends QueuedSessionRun {
  request: StartRunRequest;
  onTerminal?: TerminalHandler;
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
  queuedRuns: QueuedSessionRun[];
  queueLength: number;
  nonTerminal: boolean;
  startSessionRun(request: StartRunRequest, activeSessionRef: ActiveSessionRef, onTerminal?: TerminalHandler): Promise<StartSessionRunResult>;
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
  const [queuedRuns, setQueuedRuns] = useState<QueuedSessionRun[]>([]);
  const streamRef = useRef<RunEventStreamRef | null>(null);
  const runBusyRef = useRef(false);
  const queueDrainRef = useRef(false);
  const stopInFlightRef = useRef(false);
  const terminalCallbackCalledRef = useRef(false);
  const queueRef = useRef<QueuedSessionRunInternal[]>([]);
  const queueIdCounterRef = useRef(0);
  const nonTerminal = starting || Boolean(runId && !terminal);

  const closeActiveStream = useCallback(() => {
    streamRef.current?.close();
    streamRef.current = null;
  }, []);

  const clearRun = useCallback(() => {
    closeActiveStream();
    runBusyRef.current = false;
    queueDrainRef.current = false;
    stopInFlightRef.current = false;
    publishQueue([]);
    setStarting(false);
    setStreaming(false);
    setStopping(false);
    setTerminal(null);
    setError(null);
    setRunId("");
    setEvents([]);
    setActiveSessionRef(null);
  }, [closeActiveStream]);

  const startSessionRun = useCallback(async (
    request: StartRunRequest,
    nextActiveSessionRef: ActiveSessionRef,
    onTerminal?: TerminalHandler
  ): Promise<StartSessionRunResult> => {
    if (runBusyRef.current || queueDrainRef.current || queueRef.current.length > 0 || starting || nonTerminal) {
      const queuedRun = enqueueSessionRun(request, nextActiveSessionRef, onTerminal);
      setError(null);
      return { status: "QUEUED", queueId: queuedRun.id };
    }

    const startedRunId = await startRunNow(request, nextActiveSessionRef, onTerminal);
    return startedRunId ? { status: "STARTED", runId: startedRunId } : { status: "REJECTED" };
  }, [closeActiveStream, nonTerminal, starting]);

  function startRunNow(
    request: StartRunRequest,
    nextActiveSessionRef: ActiveSessionRef,
    onTerminal?: TerminalHandler
  ): Promise<string | null> {
    closeActiveStream();
    runBusyRef.current = true;
    queueDrainRef.current = false;
    stopInFlightRef.current = false;
    setStarting(true);
    setStreaming(false);
    setTerminal(null);
    setError(null);
    setRunId("");
    setEvents([]);
    setActiveSessionRef(nextActiveSessionRef);
    terminalCallbackCalledRef.current = false;

    return new Promise<string | null>((resolve) => {
      let started = false;
      let resolved = false;
      const resolveOnce = (value: string | null) => {
        if (!resolved) {
          resolved = true;
          resolve(value);
        }
      };
      const failRun = (caughtError: unknown) => {
        runBusyRef.current = false;
        queueDrainRef.current = queueRef.current.length > 0;
        setError(caughtError);
        setStarting(false);
        setStreaming(false);
        setStopping(false);
        setRunId("");
        if (queueRef.current.length === 0) {
          setActiveSessionRef(null);
        }
        closeActiveStream();
        resolveOnce(null);
        scheduleNextQueuedRun();
      };

      try {
        streamRef.current = openRunWebSocket(request, {
          onStarted(response) {
            started = true;
            setRunId(response.runId);
            setStarting(false);
            setStreaming(true);
            resolveOnce(response.runId);
          },
          onEvent(event) {
            setEvents((currentEvents) => [...currentEvents, event]);
            if (TERMINAL_EVENT_TYPES.has(event.type)) {
              finishTerminalRun(event, onTerminal);
            }
          },
          onError(caughtError) {
            failRun(caughtError);
          },
          onClose() {
            if (runBusyRef.current) {
              failRun(new Error(started ? "WebSocket connection closed during run" : "WebSocket connection closed before run started"));
            }
          }
        });
      } catch (caughtError) {
        failRun(caughtError);
      }
    });
  }

  function finishTerminalRun(event: RunEventDto, onTerminal?: TerminalHandler) {
    runBusyRef.current = false;
    queueDrainRef.current = queueRef.current.length > 0;
    setTerminal(event);
    setStreaming(false);
    setStopping(false);
    closeActiveStream();
    if (terminalCallbackCalledRef.current) {
      scheduleNextQueuedRun();
      return;
    }
    terminalCallbackCalledRef.current = true;
    Promise.resolve(onTerminal?.(event))
      .catch((caughtError: unknown) => setError(caughtError))
      .finally(scheduleNextQueuedRun);
  }

  function enqueueSessionRun(request: StartRunRequest, nextActiveSessionRef: ActiveSessionRef, onTerminal?: TerminalHandler): QueuedSessionRunInternal {
    const queuedRun: QueuedSessionRunInternal = {
      id: `queue-${Date.now()}-${queueIdCounterRef.current++}`,
      activeSessionRef: nextActiveSessionRef,
      prompt: request.prompt,
      mode: request.mode,
      modelId: request.modelId,
      queuedAt: new Date().toISOString(),
      request,
      onTerminal
    };
    publishQueue([...queueRef.current, queuedRun]);
    return queuedRun;
  }

  function scheduleNextQueuedRun() {
    window.setTimeout(startNextQueuedRun, 0);
  }

  function startNextQueuedRun() {
    if (runBusyRef.current) {
      return;
    }
    const [nextQueuedRun, ...remainingQueuedRuns] = queueRef.current;
    if (!nextQueuedRun) {
      queueDrainRef.current = false;
      return;
    }
    publishQueue(remainingQueuedRuns);
    void startRunNow(nextQueuedRun.request, nextQueuedRun.activeSessionRef, nextQueuedRun.onTerminal);
  }

  function publishQueue(nextQueue: QueuedSessionRunInternal[]) {
    queueRef.current = nextQueue;
    setQueuedRuns(nextQueue.map(toQueuedSessionRun));
  }

  function toQueuedSessionRun(queuedRun: QueuedSessionRunInternal): QueuedSessionRun {
    return {
      id: queuedRun.id,
      activeSessionRef: queuedRun.activeSessionRef,
      prompt: queuedRun.prompt,
      mode: queuedRun.mode,
      modelId: queuedRun.modelId,
      queuedAt: queuedRun.queuedAt
    };
  }

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
    queuedRuns,
    queueLength: queuedRuns.length,
    nonTerminal,
    startSessionRun,
    stopActiveRun,
    clearRun
  };
}
