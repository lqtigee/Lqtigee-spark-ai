export type AgentSource = "CODEX" | "OPENCODE";

export type SessionStatus = "ACTIVE" | "IDLE" | "RUNNING" | "FAILED" | "UNKNOWN";

export type RunStatus = "CREATED" | "RUNNING" | "EXITED" | "FAILED" | "STOPPED";

export interface RemoteSession {
  id: string;
  source: AgentSource;
  title: string;
  workspace: string;
  model: string;
  status: SessionStatus;
  updatedAt: string;
  lastMessage: string;
  rawFile: string;
}

export interface ModelDto {
  id: string;
  label: string;
  commandModelName: string;
  sources: AgentSource[];
  enabled: boolean;
}

export interface ApiErrorDto {
  code: string;
  message: string;
  detail: string | null;
  timestamp: string;
  path: string;
}
