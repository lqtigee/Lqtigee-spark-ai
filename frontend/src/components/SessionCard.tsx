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
    <article aria-current={selected ? "true" : undefined}>
      <h3>{session.title}</h3>
      <dl>
        <div>
          <dt>Source</dt>
          <dd>{session.source}</dd>
        </div>
        <div>
          <dt>Workspace</dt>
          <dd>{session.workspace}</dd>
        </div>
        <div>
          <dt>Model</dt>
          <dd>{session.model}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>
            <StatusBadge status={session.status} label={session.status} />
          </dd>
        </div>
      </dl>
      <button onClick={() => onSelect(session.id)} type="button">
        Select
      </button>
    </article>
  );
}
