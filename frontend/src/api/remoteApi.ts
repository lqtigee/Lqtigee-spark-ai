import { requestJson } from "./httpClient";
import type { ModelDto, RemoteSession } from "../types/api";

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

interface SessionsResponse {
  sessions: RemoteSession[];
}

export function getHealth(): Promise<HealthDto> {
  return requestJson<HealthDto>("/api/health");
}

export function listModels(): Promise<ModelsResponse> {
  return requestJson<ModelsResponse>("/api/models");
}

export function listSessions(): Promise<SessionsResponse> {
  return requestJson<SessionsResponse>("/api/sessions");
}
