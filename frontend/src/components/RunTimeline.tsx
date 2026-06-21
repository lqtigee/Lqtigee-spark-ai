import type { RunEventDto } from "../types/api";

interface RunTimelineProps {
  events: RunEventDto[];
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

export function RunTimeline({ events }: RunTimelineProps) {
  if (events.length === 0) {
    return <p className="empty-state">还没有收到运行事件</p>;
  }

  return (
    <ol className="run-timeline">
      {events.map((event, index) => (
        <li key={`${event.runId}-${event.timestamp}-${index}`}>
          <div className="run-timeline__head">
            <strong>{formatRunEventType(event.type)}</strong>
            <time dateTime={event.timestamp}>{formatDateTime(event.timestamp)}</time>
          </div>
          <p>{event.message}</p>
        </li>
      ))}
    </ol>
  );
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
