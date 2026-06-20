import { requestJson } from "./httpClient";
import type { ModelDto } from "../types/api";

interface HealthDto {
  serviceName: string;
  appName: string;
  port: number;
  status: string;
  timestamp: string;
}

interface ModelsResponse {
  models: ModelDto[];
}

export function getHealth(): Promise<HealthDto> {
  return requestJson<HealthDto>("/api/health");
}

export function listModels(): Promise<ModelsResponse> {
  return requestJson<ModelsResponse>("/api/models");
}
