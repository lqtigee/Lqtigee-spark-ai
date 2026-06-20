import type { RunEventDto } from "../types/api";

interface RunTimelineProps {
  events: RunEventDto[];
}

export function RunTimeline({ events }: RunTimelineProps) {
  return (
    <ol>
      {events.map((event, index) => (
        <li key={`${event.runId}-${event.timestamp}-${index}`}>
          <strong>{event.type}</strong>
          <p>{event.message}</p>
          <time dateTime={event.timestamp}>{event.timestamp}</time>
        </li>
      ))}
    </ol>
  );
}
