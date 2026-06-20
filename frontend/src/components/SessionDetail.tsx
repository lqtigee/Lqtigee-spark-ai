import { ErrorPanel } from "./ErrorPanel";
import { LoadingBlock } from "./LoadingBlock";
import type { RemoteSession, SessionTranscriptDto } from "../types/api";

interface SessionDetailProps {
  session?: RemoteSession;
  transcript?: SessionTranscriptDto | null;
  loading?: boolean;
  loaded?: boolean;
  error?: unknown;
  onBack?(): void;
}

export function SessionDetail({ session, transcript = null, loading = false, loaded = false, error = null, onBack }: SessionDetailProps) {
  if (!session) {
    return <p className="empty-state">No session selected</p>;
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
      {loading ? <LoadingBlock label="Loading chat" /> : null}
      {error ? <ErrorPanel title="Chat error" error={error} /> : null}
      {loaded && !error && transcript && transcript.messages.length === 0 ? <p className="empty-state">No visible chat messages found</p> : null}
      {transcript && transcript.messages.length > 0 ? (
        <ol className="chat-message-list">
          {transcript.messages.map((message) => (
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
