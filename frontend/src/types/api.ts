export type AgentSource = "CODEX" | "OPENCODE";

export type SessionStatus = "ACTIVE" | "IDLE" | "RUNNING" | "FAILED" | "UNKNOWN";

export type RunStatus = "CREATED" | "RUNNING" | "EXITED" | "FAILED" | "STOPPED";

export type CommandMode = "ASK" | "EDIT" | "REVIEW" | "SHELL";

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

export interface SessionMessageDto {
  id: string;
  role: "user" | "assistant";
  text: string;
  timestamp: string;
}

export interface TranscriptPageInfoDto {
  oldestCursor: string | null;
  newestCursor: string | null;
  hasMoreBefore: boolean;
}

export interface SessionTranscriptDto {
  session: RemoteSession;
  messages: SessionMessageDto[];
  pageInfo: TranscriptPageInfoDto;
}

export interface ModelDto {
  id: string;
  label: string;
  commandModelName: string;
  sources: AgentSource[];
  enabled: boolean;
}

export interface StartRunRequest {
  sessionId: string;
  source: AgentSource;
  modelId: string;
  mode: CommandMode;
  prompt: string;
  confirmDangerous: boolean;
}

export interface StartRunResponse {
  runId: string;
  sessionId: string;
  source: AgentSource;
  status: RunStatus;
  startedAt: string;
}

export interface RunEventDto {
  runId: string;
  type: string;
  message: string;
  timestamp: string;
  data: Record<string, unknown>;
}

export interface ApiErrorDto {
  code: string;
  message: string;
  detail: string | null;
  timestamp: string;
  path: string;
}
