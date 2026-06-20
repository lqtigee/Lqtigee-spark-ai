import { useCallback, useState } from "react";
import { listModels } from "../api/remoteApi";
import type { ModelDto } from "../types/api";

interface ModelsState {
  loading: boolean;
  error: unknown;
  models: ModelDto[];
  loadModels(): Promise<void>;
}

export function useModelsState(): ModelsState {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [models, setModels] = useState<ModelDto[]>([]);

  const loadModels = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await listModels();
      setModels(response.models);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    models,
    loadModels
  };
}
