import { requestJson } from "./httpClient";
import type { ModelDto, RemoteSession, RunStatus, StartRunRequest, StartRunResponse } from "../types/api";

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

interface StopRunResponse {
  runId: string;
  status: RunStatus;
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

export function stopRun(runId: string): Promise<StopRunResponse> {
  return requestJson<StopRunResponse>(`/api/runs/${encodeURIComponent(runId)}/stop`, {
    method: "POST"
  });
}
