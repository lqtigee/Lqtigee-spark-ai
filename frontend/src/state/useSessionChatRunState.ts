import { useCallback, useState } from "react";
import { startRun } from "../api/remoteApi";
import type { StartRunRequest } from "../types/api";

interface SessionChatRunState {
  starting: boolean;
  error: unknown;
  runId: string;
  startSessionRun(request: StartRunRequest): Promise<string | null>;
}

export function useSessionChatRunState(): SessionChatRunState {
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [runId, setRunId] = useState("");

  const startSessionRun = useCallback(async (request: StartRunRequest) => {
    setStarting(true);
    setError(null);

    try {
      const response = await startRun(request);
      setRunId(response.runId);
      return response.runId;
    } catch (caughtError) {
      setError(caughtError);
      return null;
    } finally {
      setStarting(false);
    }
  }, []);

  return {
    starting,
    error,
    runId,
    startSessionRun
  };
}
