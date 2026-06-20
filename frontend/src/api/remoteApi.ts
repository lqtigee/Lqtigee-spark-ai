import { requestJson } from "./httpClient";

interface HealthDto {
  serviceName: string;
  appName: string;
  port: number;
  status: string;
  timestamp: string;
}

export function getHealth(): Promise<HealthDto> {
  return requestJson<HealthDto>("/api/health");
}
