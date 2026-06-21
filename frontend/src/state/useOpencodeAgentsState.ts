import { useCallback, useState } from "react";
import { requestJson } from "../api/httpClient";

const TOKEN_KEY = "lqtigee_token";

export interface OpencodeAgent {
  id: string;
  name: string;
  source: string;
}

interface OpencodeAgentsResponse {
  opencodeAgents: OpencodeAgent[];
}

interface OpencodeAgentsState {
  agents: OpencodeAgent[];
  loading: boolean;
  loaded: boolean;
  error: unknown;
  loadAgents(): Promise<void>;
}

export function useOpencodeAgentsState(): OpencodeAgentsState {
  const [agents, setAgents] = useState<OpencodeAgent[]>([]);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const loadAgents = useCallback(async () => {
    if (!localStorage.getItem(TOKEN_KEY)) {
      setAgents([]);
      setLoaded(false);
      setError(null);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await requestJson<OpencodeAgentsResponse>("/api/opencode/agents");
      setAgents(response.opencodeAgents);
      setLoaded(true);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    agents,
    loading,
    loaded,
    error,
    loadAgents
  };
}
