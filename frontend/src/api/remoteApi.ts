import { requestJson } from "./httpClient";
import type { ModelDto, RemoteSession, StartRunRequest, StartRunResponse } from "../types/api";

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

export function startRun(request: StartRunRequest): Promise<StartRunResponse> {
  return requestJson<StartRunResponse>("/api/runs", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });
}
