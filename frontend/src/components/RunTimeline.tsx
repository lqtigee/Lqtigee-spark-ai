import type { RunEventDto } from "../types/api";

interface RunTimelineProps {
  events: RunEventDto[];
}

export function RunTimeline({ events }: RunTimelineProps) {
  if (events.length === 0) {
    return <p className="empty-state">No events received yet</p>;
  }

  return (
    <ol className="run-timeline">
      {events.map((event, index) => (
        <li key={`${event.runId}-${event.timestamp}-${index}`}>
          <div className="run-timeline__head">
            <strong>{event.type}</strong>
            <time dateTime={event.timestamp}>{formatDateTime(event.timestamp)}</time>
          </div>
          <p>{event.message}</p>
        </li>
      ))}
    </ol>
  );
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
