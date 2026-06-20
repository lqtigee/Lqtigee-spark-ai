import { StatusBadge } from "./StatusBadge";
import type { RemoteSession } from "../types/api";

interface SessionCardProps {
  session?: RemoteSession;
  selected: boolean;
  onSelect(sessionId: string): void;
}

export function SessionCard({ session, selected, onSelect }: SessionCardProps) {
  if (!session) {
    return null;
  }

  return (
    <article className={selected ? "session-card session-card--selected" : "session-card"} aria-current={selected ? "true" : undefined}>
      <div className="session-card__topline">
        <span className={`source-pill source-pill--${session.source.toLowerCase()}`}>{session.source}</span>
        <StatusBadge status={session.status} label={session.status} />
      </div>
      <h3>{session.title}</h3>
      <p className="session-card__message">{session.lastMessage}</p>
      <dl className="session-card__meta">
        <div>
          <dt>Workspace</dt>
          <dd>{session.workspace}</dd>
        </div>
        <div>
          <dt>Model</dt>
          <dd>{session.model}</dd>
        </div>
        <div>
          <dt>Updated</dt>
          <dd>{formatDateTime(session.updatedAt)}</dd>
        </div>
      </dl>
      <button className={selected ? "button button--primary" : "button button--secondary"} onClick={() => onSelect(session.id)} type="button">
        {selected ? "Selected" : "Select"}
      </button>
    </article>
  );
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
