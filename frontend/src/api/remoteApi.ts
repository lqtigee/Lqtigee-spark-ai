import { ApiClientError, getRequiredToken, requestJson, toApiUrl } from "./httpClient";
import type {
  ApiErrorDto,
  AttachmentDto,
  DeleteAttachmentResponse,
  ModelDto,
  RemoteSession,
  RunEventDto,
  RunStatus,
  SessionTranscriptDto,
  SourceCapabilityDto,
  StartRunRequest,
  StartRunResponse
} from "../types/api";

const TERMINAL_EVENT_TYPES = new Set(["done", "error", "stopped"]);

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

interface CapabilitiesResponse {
  capabilities: SourceCapabilityDto[];
}

interface SessionTranscriptOptions {
  limit?: number;
  before?: string;
}

interface StopRunResponse {
  runId: string;
  status: RunStatus;
}

interface RunEventHandlers {
  onEvent(event: RunEventDto): void;
  onError(error: unknown): void;
}

interface RunEventStream {
  close(): void;
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

export function getCapabilities(): Promise<CapabilitiesResponse> {
  return requestJson<CapabilitiesResponse>("/api/capabilities");
}

export function getSessionTranscript(source: string, id: string, options: SessionTranscriptOptions = {}): Promise<SessionTranscriptDto> {
  const query = new URLSearchParams();
  if (options.limit !== undefined) {
    query.set("limit", String(options.limit));
  }
  if (options.before) {
    query.set("before", options.before);
  }
  const queryString = query.toString();
  const path = `/api/sessions/${encodeURIComponent(source)}/${encodeURIComponent(id)}/transcript${queryString ? `?${queryString}` : ""}`;
  return requestJson<SessionTranscriptDto>(path);
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

export async function uploadAttachment(file: File): Promise<AttachmentDto> {
  const body = new FormData();
  body.append("file", file);

  return requestJson<AttachmentDto>("/api/attachments", {
    method: "POST",
    body
  });
}

export function deleteAttachment(id: string): Promise<DeleteAttachmentResponse> {
  return requestJson<DeleteAttachmentResponse>(`/api/attachments/${encodeURIComponent(id)}`, {
    method: "DELETE"
  });
}

export function stopRun(runId: string): Promise<StopRunResponse> {
  return requestJson<StopRunResponse>(`/api/runs/${encodeURIComponent(runId)}/stop`, {
    method: "POST"
  });
}

export function openRunEvents(runId: string, handlers: RunEventHandlers): RunEventStream {
  const controller = new AbortController();
  let closedByTerminalEvent = false;

  void readRunEvents(runId, handlers, controller, () => {
    closedByTerminalEvent = true;
  }).catch((error: unknown) => {
    if (!closedByTerminalEvent) {
      handlers.onError(error);
    }
  });

  return {
    close() {
      controller.abort();
    }
  };
}

async function readRunEvents(
  runId: string,
  handlers: RunEventHandlers,
  controller: AbortController,
  markTerminalClosed: () => void
): Promise<void> {
  const response = await fetch(toApiUrl(`/api/runs/${encodeURIComponent(runId)}/events`), {
    headers: {
      Accept: "text/event-stream",
      Authorization: `Bearer ${getRequiredToken()}`
    },
    signal: controller.signal
  });

  if (!response.ok) {
    throw new ApiClientError((await response.json()) as ApiErrorDto);
  }
  if (!response.body) {
    throw new Error("SSE response body is missing");
  }

  await readSseFrames(response.body.getReader(), (frame) => {
    const event = parseRunEvent(frame);
    handlers.onEvent(event);
    if (TERMINAL_EVENT_TYPES.has(event.type)) {
      markTerminalClosed();
      controller.abort();
      return true;
    }
    return false;
  });
}

async function readSseFrames(
  reader: ReadableStreamDefaultReader<Uint8Array>,
  onFrame: (frame: string) => boolean
): Promise<void> {
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const result = consumeFrames(buffer, onFrame);
      buffer = result.remaining;
      if (result.closed) {
        return;
      }
    }

    buffer += decoder.decode();
    if (buffer.trim()) {
      onFrame(buffer);
    }
  } finally {
    reader.releaseLock();
  }
}

function consumeFrames(buffer: string, onFrame: (frame: string) => boolean): { remaining: string; closed: boolean } {
  let remaining = buffer.replace(/\r\n/g, "\n");

  while (true) {
    const separatorIndex = remaining.indexOf("\n\n");
    if (separatorIndex < 0) {
      return { remaining, closed: false };
    }

    const frame = remaining.slice(0, separatorIndex);
    remaining = remaining.slice(separatorIndex + 2);
    if (frame.trim() && onFrame(frame)) {
      return { remaining, closed: true };
    }
  }
}

function parseRunEvent(frame: string): RunEventDto {
  const dataLines = frame
    .split("\n")
    .filter((line) => line.startsWith("data:"))
    .map((line) => line.slice("data:".length).trimStart());
  if (!dataLines.length) {
    throw new Error("SSE frame is missing data");
  }

  return JSON.parse(dataLines.join("\n")) as RunEventDto;
}
