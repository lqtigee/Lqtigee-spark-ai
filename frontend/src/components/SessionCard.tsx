import { memo } from "react";
import { StatusBadge } from "./StatusBadge";
import type { RemoteSession } from "../types/api";

interface SessionCardProps {
  session?: RemoteSession;
  selected: boolean;
  onSelect(session: RemoteSession): void;
}

export const SessionCard = memo(function SessionCard({ session, selected, onSelect }: SessionCardProps) {
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
          <dt>工作目录</dt>
          <dd>{session.workspace}</dd>
        </div>
        <div>
          <dt>模型</dt>
          <dd>{session.model}</dd>
        </div>
        <div>
          <dt>更新时间</dt>
          <dd>{formatDateTime(session.updatedAt)}</dd>
        </div>
      </dl>
      <button className={selected ? "button button--primary" : "button button--secondary"} onClick={() => onSelect(session)} type="button">
        {selected ? "已选择" : "选择"}
      </button>
    </article>
  );
});

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
