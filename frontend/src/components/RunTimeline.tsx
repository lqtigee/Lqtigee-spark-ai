import type { RunEventDto } from "../types/api";

interface RunTimelineProps {
  events: RunEventDto[];
  starting?: boolean;
  stopping?: boolean;
  streaming?: boolean;
  terminal?: RunEventDto | null;
}

const RUN_EVENT_LABELS: Record<string, string> = {
  done: "完成",
  error: "错误",
  status: "状态",
  stderr: "错误输出",
  stdout: "标准输出",
  stopped: "已停止",
  tool: "工具"
};

interface DisplayRunEvent {
  title: string;
  body: string;
  tone: "info" | "assistant" | "error" | "terminal" | "tool";
}

export function RunTimeline({ events, starting = false, stopping = false, streaming = false, terminal = null }: RunTimelineProps) {
  const state = formatLiveState(starting, stopping, streaming, terminal);

  if (events.length === 0) {
    return (
      <section className="sse-live" aria-label="实时流">
        <div className="sse-live__head">
          <span className={`sse-live__dot sse-live__dot--${state.tone}`} aria-hidden="true" />
          <strong>{state.label}</strong>
          <span>0 条事件</span>
        </div>
        <p className="empty-state">正在等待 SSE 事件</p>
      </section>
    );
  }

  return (
    <section className="sse-live" aria-label="实时流">
      <div className="sse-live__head">
        <span className={`sse-live__dot sse-live__dot--${state.tone}`} aria-hidden="true" />
        <strong>{state.label}</strong>
        <span>{events.length} 条事件</span>
      </div>
      <ol className="run-timeline">
        {events.map((event, index) => {
          const displayEvent = formatRunEvent(event);
          return (
            <li className={`run-timeline__item run-timeline__item--${displayEvent.tone}`} key={`${event.runId}-${event.timestamp}-${index}`}>
              <div className="run-timeline__head">
                <strong>{displayEvent.title}</strong>
                <time dateTime={event.timestamp}>{formatDateTime(event.timestamp)}</time>
              </div>
              <p>{displayEvent.body}</p>
            </li>
          );
        })}
      </ol>
    </section>
  );
}

function formatLiveState(
  starting: boolean,
  stopping: boolean,
  streaming: boolean,
  terminal: RunEventDto | null
): { label: string; tone: "idle" | "live" | "done" | "error" } {
  if (stopping) {
    return { label: "正在停止 SSE", tone: "live" };
  }
  if (starting) {
    return { label: "正在连接 SSE", tone: "live" };
  }
  if (streaming) {
    return { label: "SSE 实时流", tone: "live" };
  }
  if (terminal?.type === "error") {
    return { label: "SSE 已错误结束", tone: "error" };
  }
  if (terminal) {
    return { label: "SSE 已结束", tone: "done" };
  }
  return { label: "SSE 未连接", tone: "idle" };
}

function formatRunEvent(event: RunEventDto): DisplayRunEvent {
  if (event.type === "stdout") {
    return parseCodexStdoutMessage(event.message) ?? {
      title: "标准输出",
      body: event.message,
      tone: "info"
    };
  }
  if (event.type === "stderr") {
    return {
      title: "错误输出",
      body: event.message,
      tone: "error"
    };
  }
  if (event.type === "done" || event.type === "stopped") {
    return {
      title: formatRunEventType(event.type),
      body: event.message,
      tone: "terminal"
    };
  }
  if (event.type === "error") {
    return {
      title: "错误",
      body: event.message,
      tone: "error"
    };
  }
  return {
    title: formatRunEventType(event.type),
    body: event.message,
    tone: "info"
  };
}

function parseCodexStdoutMessage(message: string): DisplayRunEvent | null {
  let parsed: unknown;
  try {
    parsed = JSON.parse(message);
  } catch {
    return null;
  }
  if (!isRecord(parsed)) {
    return null;
  }

  const type = stringValue(parsed.type);
  if (type === "thread.started") {
    return { title: "线程已连接", body: stringValue(parsed.thread_id) ?? "Codex thread started", tone: "info" };
  }
  if (type === "turn.started") {
    return { title: "开始生成", body: "Codex 正在处理当前输入", tone: "info" };
  }
  if (type === "turn.completed") {
    return { title: "生成完成", body: formatUsage(parsed.usage), tone: "terminal" };
  }
  if (type === "item.completed") {
    return formatCompletedItem(parsed.item);
  }
  return null;
}

function formatCompletedItem(item: unknown): DisplayRunEvent | null {
  if (!isRecord(item)) {
    return null;
  }
  const itemType = stringValue(item.type);
  if (itemType === "agent_message") {
    return {
      title: "助手输出",
      body: stringValue(item.text) ?? "Assistant message completed",
      tone: "assistant"
    };
  }
  if (itemType === "tool_call") {
    return {
      title: "工具调用",
      body: stringValue(item.name) ?? "Tool call completed",
      tone: "tool"
    };
  }
  if (itemType === "reasoning") {
    return {
      title: "推理步骤",
      body: "Reasoning item completed",
      tone: "info"
    };
  }
  return null;
}

function formatUsage(usage: unknown): string {
  if (!isRecord(usage)) {
    return "Codex turn completed";
  }
  const parts = [
    numberLabel(usage.input_tokens, "输入"),
    numberLabel(usage.output_tokens, "输出"),
    numberLabel(usage.reasoning_output_tokens, "推理")
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : "Codex turn completed";
}

function numberLabel(value: unknown, label: string): string {
  return typeof value === "number" ? `${label} ${value}` : "";
}

function formatRunEventType(type: string): string {
  return RUN_EVENT_LABELS[type] ?? type;
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function stringValue(value: unknown): string | null {
  return typeof value === "string" && value.trim() ? value : null;
}
