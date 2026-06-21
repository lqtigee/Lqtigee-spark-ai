import { useCallback, useState } from "react";
import { getCapabilities } from "../api/remoteApi";
import type { AgentSource, SourceCapabilityDto } from "../types/api";

const TOKEN_KEY = "lqtigee_token";

interface CapabilitiesState {
  loading: boolean;
  error: unknown;
  capabilities: SourceCapabilityDto[];
  loadCapabilities(): Promise<void>;
  capabilityFor(source: AgentSource): SourceCapabilityDto | null;
}

export function useCapabilitiesState(): CapabilitiesState {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [capabilities, setCapabilities] = useState<SourceCapabilityDto[]>([]);

  const loadCapabilities = useCallback(async () => {
    if (!localStorage.getItem(TOKEN_KEY)) {
      setCapabilities([]);
      setError(null);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await getCapabilities();
      setCapabilities(response.capabilities);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setLoading(false);
    }
  }, []);

  const capabilityFor = useCallback(
    (source: AgentSource) => capabilities.find((capability) => capability.source === source) ?? null,
    [capabilities]
  );

  return {
    loading,
    error,
    capabilities,
    loadCapabilities,
    capabilityFor
  };
}
