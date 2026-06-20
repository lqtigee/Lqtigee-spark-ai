import type { UIEvent } from "react";
import { ErrorPanel } from "./ErrorPanel";
import { LoadingBlock } from "./LoadingBlock";
import type { RemoteSession, SessionMessageDto, SessionTranscriptDto, TranscriptPageInfoDto } from "../types/api";

interface SessionDetailProps {
  session?: RemoteSession;
  transcript?: SessionTranscriptDto | null;
  messages?: SessionMessageDto[];
  pageInfo?: TranscriptPageInfoDto | null;
  loading?: boolean;
  loadingNewest?: boolean;
  loadingOlder?: boolean;
  loaded?: boolean;
  error?: unknown;
  onBack?(): void;
  onLoadOlder?(): void;
}

export function SessionDetail({
  session,
  transcript = null,
  messages,
  pageInfo = null,
  loading = false,
  loadingNewest = loading,
  loadingOlder = false,
  loaded = false,
  error = null,
  onBack,
  onLoadOlder
}: SessionDetailProps) {
  if (!session) {
    return <p className="empty-state">No session selected</p>;
  }

  const visibleMessages = messages ?? transcript?.messages ?? [];
  const canLoadOlder = Boolean(pageInfo?.hasMoreBefore && onLoadOlder);

  function handleMessageScroll(event: UIEvent<HTMLOListElement>) {
    if (!canLoadOlder || loadingOlder) {
      return;
    }
    if (event.currentTarget.scrollTop <= 48) {
      onLoadOlder?.();
    }
  }

  return (
    <section className="detail-panel chat-panel">
      <div className="chat-panel__header">
        {onBack ? (
          <button className="button button--secondary chat-panel__back" onClick={onBack} type="button">
            Back
          </button>
        ) : null}
        <div>
          <h3>{session.title}</h3>
          <p>{session.source} / {session.model}</p>
        </div>
      </div>
      <dl className="chat-panel__meta">
        <div>
          <dt>Workspace</dt>
          <dd>{session.workspace}</dd>
        </div>
        <div>
          <dt>Updated</dt>
          <dd>{formatDateTime(session.updatedAt)}</dd>
        </div>
      </dl>
      {loadingNewest ? <LoadingBlock label="Loading chat" /> : null}
      {error ? <ErrorPanel title="Chat error" error={error} /> : null}
      {loaded && !error && visibleMessages.length === 0 ? <p className="empty-state">No visible chat messages found</p> : null}
      {visibleMessages.length > 0 ? (
        <ol className="chat-message-list" onScroll={handleMessageScroll}>
          {canLoadOlder ? (
            <li className="chat-history-top">
              <button className="button button--secondary" disabled={loadingOlder} onClick={onLoadOlder} type="button">
                {loadingOlder ? "Loading earlier messages" : "Load earlier messages"}
              </button>
            </li>
          ) : null}
          {visibleMessages.map((message) => (
            <li className={`chat-message chat-message--${message.role}`} key={message.id}>
              <div className="chat-message__head">
                <strong>{message.role}</strong>
                <time dateTime={message.timestamp}>{formatDateTime(message.timestamp)}</time>
              </div>
              <p>{message.text}</p>
            </li>
          ))}
        </ol>
      ) : null}
    </section>
  );
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
