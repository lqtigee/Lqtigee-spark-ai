import { useCallback, useState } from "react";
import { ApiClientError } from "../api/httpClient";
import { getHealth } from "../api/remoteApi";

type ConnectionStatus = "idle" | "checking" | "connected" | "unauthorized" | "failed";

interface ConnectionState {
  status: ConnectionStatus;
  error: unknown;
  checkConnection(): Promise<void>;
}

export function useConnectionState(): ConnectionState {
  const [status, setStatus] = useState<ConnectionStatus>("idle");
  const [error, setError] = useState<unknown>(null);

  const checkConnection = useCallback(async () => {
    setStatus("checking");
    setError(null);

    try {
      await getHealth();
      setStatus("connected");
    } catch (caughtError) {
      setError(caughtError);
      setStatus(isUnauthorized(caughtError) ? "unauthorized" : "failed");
    }
  }, []);

  return {
    status,
    error,
    checkConnection
  };
}

function isUnauthorized(error: unknown): boolean {
  return (
    error instanceof ApiClientError &&
    (error.error.code === "AUTH_TOKEN_MISSING" || error.error.code === "AUTH_TOKEN_INVALID")
  );
}
