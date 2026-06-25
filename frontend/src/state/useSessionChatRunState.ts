import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { openRunWebSocket, openRunWebSocketForRun, stopRun } from "../api/remoteApi";
import type { AgentSource, RunEventDto, StartRunRequest } from "../types/api";

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

export interface SessionRunView {
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
}

type TerminalHandler = (event: RunEventDto) => void | Promise<void>;

interface QueuedSessionRunInternal extends QueuedSessionRun {
  request: StartRunRequest;
  onTerminal?: TerminalHandler;
}

interface SessionRunBucket {
  starting: boolean;
  streaming: boolean;
  stopping: boolean;
  terminal: RunEventDto | null;
  error: unknown;
  runId: string;
  events: RunEventDto[];
  activeSessionRef: ActiveSessionRef;
  queuedRuns: QueuedSessionRun[];
}

interface SessionChatRunState {
  queuedRuns: QueuedSessionRun[];
  stateForSession(activeSessionRef: ActiveSessionRef | null): SessionRunView;
  startSessionRun(request: StartRunRequest, activeSessionRef: ActiveSessionRef, onTerminal?: TerminalHandler): Promise<StartSessionRunResult>;
  attachSessionRun(runId: string, activeSessionRef: ActiveSessionRef, onTerminal?: TerminalHandler): void;
  stopSessionRun(activeSessionRef: ActiveSessionRef): Promise<void>;
  clearRun(activeSessionRef?: ActiveSessionRef): void;
}

const EMPTY_SESSION_RUN_VIEW: SessionRunView = {
  starting: false,
  streaming: false,
  stopping: false,
  terminal: null,
  error: null,
  runId: "",
  events: [],
  activeSessionRef: null,
  queuedRuns: [],
  queueLength: 0,
  nonTerminal: false
};

export function useSessionChatRunState(): SessionChatRunState {
  const [buckets, setBuckets] = useState<Record<string, SessionRunBucket>>({});
  const bucketsRef = useRef<Record<string, SessionRunBucket>>({});
  const streamRefs = useRef(new Map<string, RunEventStreamRef>());
  const runBusyRefs = useRef(new Map<string, boolean>());
  const queueDrainRefs = useRef(new Map<string, boolean>());
  const stopInFlightRefs = useRef(new Map<string, boolean>());
  const terminalCallbackCalledRefs = useRef(new Map<string, boolean>());
  const queueRefs = useRef(new Map<string, QueuedSessionRunInternal[]>());
  const queueIdCounterRef = useRef(0);

  const setBucket = useCallback((key: string, updater: (current: SessionRunBucket | null) => SessionRunBucket | null) => {
    setBuckets((currentBuckets) => {
      const currentBucket = currentBuckets[key] ?? null;
      const nextBucket = updater(currentBucket);
      const nextBuckets = { ...currentBuckets };
      if (nextBucket) {
        nextBuckets[key] = nextBucket;
      } else {
        delete nextBuckets[key];
      }
      bucketsRef.current = nextBuckets;
      return nextBuckets;
    });
  }, []);

  const closeSessionStream = useCallback((key: string) => {
    streamRefs.current.get(key)?.close();
    streamRefs.current.delete(key);
  }, []);

  const closeAllStreams = useCallback(() => {
    streamRefs.current.forEach((stream) => stream.close());
    streamRefs.current.clear();
  }, []);

  const clearRun = useCallback((activeSessionRef?: ActiveSessionRef) => {
    if (activeSessionRef) {
      const key = sessionKey(activeSessionRef);
      closeSessionStream(key);
      runBusyRefs.current.delete(key);
      queueDrainRefs.current.delete(key);
      stopInFlightRefs.current.delete(key);
      terminalCallbackCalledRefs.current.delete(key);
      queueRefs.current.delete(key);
      setBucket(key, () => null);
      return;
    }

    closeAllStreams();
    runBusyRefs.current.clear();
    queueDrainRefs.current.clear();
    stopInFlightRefs.current.clear();
    terminalCallbackCalledRefs.current.clear();
    queueRefs.current.clear();
    bucketsRef.current = {};
    setBuckets({});
  }, [closeAllStreams, closeSessionStream, setBucket]);

  const startSessionRun = useCallback(async (
    request: StartRunRequest,
    activeSessionRef: ActiveSessionRef,
    onTerminal?: TerminalHandler
  ): Promise<StartSessionRunResult> => {
    const key = sessionKey(activeSessionRef);
    const bucket = bucketsRef.current[key];
    const sessionNonTerminal = isBucketNonTerminal(bucket);
    const sessionQueue = queueRefs.current.get(key) ?? [];
    if (runBusyRefs.current.get(key) || queueDrainRefs.current.get(key) || sessionQueue.length > 0 || sessionNonTerminal) {
      const queuedRun = enqueueSessionRun(key, request, activeSessionRef, onTerminal);
      setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, { queuedRuns: sessionQueueToPublic(queueRefs.current.get(key) ?? []) }));
      return { status: "QUEUED", queueId: queuedRun.id };
    }

    const startedRunId = await startRunNow(key, request, activeSessionRef, onTerminal);
    return startedRunId ? { status: "STARTED", runId: startedRunId } : { status: "REJECTED" };
  }, [closeSessionStream, setBucket]);

  const attachSessionRun = useCallback((runId: string, activeSessionRef: ActiveSessionRef, onTerminal?: TerminalHandler) => {
    const key = sessionKey(activeSessionRef);
    const bucket = bucketsRef.current[key];
    if (!runId || runBusyRefs.current.get(key) || bucket?.runId === runId) {
      return;
    }
    closeSessionStream(key);
    runBusyRefs.current.set(key, true);
    queueDrainRefs.current.set(key, false);
    stopInFlightRefs.current.set(key, false);
    terminalCallbackCalledRefs.current.set(key, false);
    setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
      starting: false,
      streaming: true,
      stopping: false,
      terminal: null,
      error: null,
      runId,
      events: []
    }));
    streamRefs.current.set(key, openRunWebSocketForRun(runId, {
      onStarted(response) {
        setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
          runId: response.runId,
          starting: false,
          streaming: response.status === "RUNNING" || response.status === "CREATED"
        }));
      },
      onEvent(event) {
        setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
          events: appendUniqueEvent(currentBucket?.events ?? [], event)
        }));
        if (TERMINAL_EVENT_TYPES.has(event.type)) {
          finishTerminalRun(key, event, activeSessionRef, onTerminal);
        }
      },
      onError(caughtError) {
        runBusyRefs.current.set(key, false);
        setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
          error: caughtError,
          streaming: false
        }));
        closeSessionStream(key);
      },
      onClose() {
        if (runBusyRefs.current.get(key)) {
          runBusyRefs.current.set(key, false);
          setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
            streaming: false
          }));
        }
      }
    }));
  }, [closeSessionStream, setBucket]);

  function startRunNow(
    key: string,
    request: StartRunRequest,
    activeSessionRef: ActiveSessionRef,
    onTerminal?: TerminalHandler
  ): Promise<string | null> {
    closeSessionStream(key);
    runBusyRefs.current.set(key, true);
    queueDrainRefs.current.set(key, false);
    stopInFlightRefs.current.set(key, false);
    terminalCallbackCalledRefs.current.set(key, false);
    setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
      starting: true,
      streaming: false,
      stopping: false,
      terminal: null,
      error: null,
      runId: "",
      events: []
    }));

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
        runBusyRefs.current.set(key, false);
        queueDrainRefs.current.set(key, (queueRefs.current.get(key) ?? []).length > 0);
        setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
          error: caughtError,
          starting: false,
          streaming: false,
          stopping: false,
          runId: ""
        }));
        closeSessionStream(key);
        resolveOnce(null);
        scheduleNextQueuedRun(key);
      };

      try {
        streamRefs.current.set(key, openRunWebSocket(request, {
          onStarted(response) {
            started = true;
            setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
              runId: response.runId,
              starting: false,
              streaming: true
            }));
            resolveOnce(response.runId);
          },
          onEvent(event) {
            setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
              events: appendUniqueEvent(currentBucket?.events ?? [], event)
            }));
            if (TERMINAL_EVENT_TYPES.has(event.type)) {
              finishTerminalRun(key, event, activeSessionRef, onTerminal);
            }
          },
          onError(caughtError) {
            failRun(caughtError);
          },
          onClose() {
            if (runBusyRefs.current.get(key)) {
              failRun(new Error(started ? "WebSocket connection closed during run" : "WebSocket connection closed before run started"));
            }
          }
        }));
      } catch (caughtError) {
        failRun(caughtError);
      }
    });
  }

  function finishTerminalRun(key: string, event: RunEventDto, activeSessionRef: ActiveSessionRef, onTerminal?: TerminalHandler) {
    runBusyRefs.current.set(key, false);
    queueDrainRefs.current.set(key, (queueRefs.current.get(key) ?? []).length > 0);
    setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
      terminal: event,
      streaming: false,
      stopping: false
    }));
    closeSessionStream(key);
    if (terminalCallbackCalledRefs.current.get(key)) {
      scheduleNextQueuedRun(key);
      return;
    }
    terminalCallbackCalledRefs.current.set(key, true);
    Promise.resolve(onTerminal?.(event))
      .catch(() => undefined)
      .finally(() => scheduleNextQueuedRun(key));
  }

  function enqueueSessionRun(key: string, request: StartRunRequest, activeSessionRef: ActiveSessionRef, onTerminal?: TerminalHandler): QueuedSessionRunInternal {
    const nextQueue = [
      ...(queueRefs.current.get(key) ?? []),
      {
        id: `queue-${Date.now()}-${queueIdCounterRef.current++}`,
        activeSessionRef,
        prompt: request.prompt,
        mode: request.mode,
        modelId: request.modelId,
        queuedAt: new Date().toISOString(),
        request,
        onTerminal
      }
    ];
    queueRefs.current.set(key, nextQueue);
    publishQueue(key, activeSessionRef, nextQueue);
    return nextQueue[nextQueue.length - 1];
  }

  function scheduleNextQueuedRun(key: string) {
    window.setTimeout(() => startNextQueuedRun(key), 0);
  }

  function startNextQueuedRun(key: string) {
    if (runBusyRefs.current.get(key)) {
      return;
    }
    const [nextQueuedRun, ...remainingQueuedRuns] = queueRefs.current.get(key) ?? [];
    if (!nextQueuedRun) {
      queueDrainRefs.current.set(key, false);
      return;
    }
    queueRefs.current.set(key, remainingQueuedRuns);
    publishQueue(key, nextQueuedRun.activeSessionRef, remainingQueuedRuns);
    void startRunNow(key, nextQueuedRun.request, nextQueuedRun.activeSessionRef, nextQueuedRun.onTerminal);
  }

  function publishQueue(key: string, activeSessionRef: ActiveSessionRef, nextQueue: QueuedSessionRunInternal[]) {
    setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
      queuedRuns: sessionQueueToPublic(nextQueue)
    }));
  }

  const stopSessionRun = useCallback(async (activeSessionRef: ActiveSessionRef) => {
    const key = sessionKey(activeSessionRef);
    const bucket = bucketsRef.current[key];
    if (!bucket?.runId || bucket.terminal || bucket.stopping || stopInFlightRefs.current.get(key)) {
      return;
    }

    stopInFlightRefs.current.set(key, true);
    setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
      stopping: true,
      error: null
    }));
    try {
      await stopRun(bucket.runId);
    } catch (caughtError) {
      setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
        error: caughtError
      }));
    } finally {
      stopInFlightRefs.current.set(key, false);
      setBucket(key, (currentBucket) => ensureBucket(currentBucket, activeSessionRef, {
        stopping: false
      }));
    }
  }, [setBucket]);

  const stateForSession = useCallback((activeSessionRef: ActiveSessionRef | null): SessionRunView => {
    if (!activeSessionRef) {
      return EMPTY_SESSION_RUN_VIEW;
    }
    return bucketToView(buckets[sessionKey(activeSessionRef)] ?? null);
  }, [buckets]);

  const queuedRuns = useMemo(
    () => Object.values(buckets).flatMap((bucket) => bucket.queuedRuns),
    [buckets]
  );

  useEffect(() => closeAllStreams, [closeAllStreams]);

  return {
    queuedRuns,
    stateForSession,
    startSessionRun,
    attachSessionRun,
    stopSessionRun,
    clearRun
  };
}

function ensureBucket(
  currentBucket: SessionRunBucket | null,
  activeSessionRef: ActiveSessionRef,
  patch: Partial<SessionRunBucket>
): SessionRunBucket {
  return {
    starting: false,
    streaming: false,
    stopping: false,
    terminal: null,
    error: null,
    runId: "",
    events: [],
    queuedRuns: [],
    ...currentBucket,
    ...patch,
    activeSessionRef
  };
}

function bucketToView(bucket: SessionRunBucket | null): SessionRunView {
  if (!bucket) {
    return EMPTY_SESSION_RUN_VIEW;
  }
  return {
    ...bucket,
    queueLength: bucket.queuedRuns.length,
    nonTerminal: isBucketNonTerminal(bucket)
  };
}

function isBucketNonTerminal(bucket: SessionRunBucket | null | undefined): boolean {
  return Boolean(bucket && (bucket.starting || (bucket.runId && !bucket.terminal)));
}

function sessionQueueToPublic(queue: QueuedSessionRunInternal[]): QueuedSessionRun[] {
  return queue.map((queuedRun) => ({
    id: queuedRun.id,
    activeSessionRef: queuedRun.activeSessionRef,
    prompt: queuedRun.prompt,
    mode: queuedRun.mode,
    modelId: queuedRun.modelId,
    queuedAt: queuedRun.queuedAt
  }));
}

function sessionKey(activeSessionRef: ActiveSessionRef): string {
  return `${activeSessionRef.source}:${activeSessionRef.id}`;
}

function appendUniqueEvent(events: RunEventDto[], event: RunEventDto): RunEventDto[] {
  const eventKey = runEventKey(event);
  if (events.some((currentEvent) => runEventKey(currentEvent) === eventKey)) {
    return events;
  }
  return [...events, event];
}

function runEventKey(event: RunEventDto): string {
  return `${event.runId}:${event.timestamp}:${event.type}:${event.message}`;
}
