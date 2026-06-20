import { useEffect, useRef, type UIEvent } from "react";
import { ErrorPanel } from "./ErrorPanel";
import { LoadingBlock } from "./LoadingBlock";
import { SessionChatComposer } from "./SessionChatComposer";
import type { RemoteSession, RunEventDto, SessionMessageDto, SessionTranscriptDto, StartRunRequest, TranscriptPageInfoDto } from "../types/api";

interface ScrollAnchor {
  messageCount: number;
  scrollHeight: number;
  scrollTop: number;
}

interface SessionDetailProps {
  session?: RemoteSession;
  transcript?: SessionTranscriptDto | null;
  messages?: SessionMessageDto[];
  pageInfo?: TranscriptPageInfoDto | null;
  loading?: boolean;
  loadingNewest?: boolean;
  loadingOlder?: boolean;
  chatRunStarting?: boolean;
  chatRunStreaming?: boolean;
  chatRunStopping?: boolean;
  chatRunTerminal?: RunEventDto | null;
  chatRunEvents?: RunEventDto[];
  chatRunError?: unknown;
  chatRunId?: string;
  loaded?: boolean;
  error?: unknown;
  onBack?(): void;
  onLoadOlder?(): void;
  onStartChatRun?(request: StartRunRequest): Promise<string | null>;
  onStopChatRun?(): Promise<void>;
}

export function SessionDetail({
  session,
  transcript = null,
  messages,
  pageInfo = null,
  loading = false,
  loadingNewest = loading,
  loadingOlder = false,
  chatRunStarting = false,
  chatRunStreaming = false,
  chatRunStopping = false,
  chatRunTerminal = null,
  chatRunEvents = [],
  chatRunError = null,
  chatRunId = "",
  loaded = false,
  error = null,
  onBack,
  onLoadOlder,
  onStartChatRun,
  onStopChatRun
}: SessionDetailProps) {
  const visibleMessages = messages ?? transcript?.messages ?? [];
  const canLoadOlder = Boolean(pageInfo?.hasMoreBefore && onLoadOlder);
  const scrollRef = useRef<HTMLOListElement | null>(null);
  const activeSessionIdRef = useRef<string | null>(null);
  const initialBottomAppliedRef = useRef(false);
  const pendingOlderAnchorRef = useRef<ScrollAnchor | null>(null);
  const olderRequestInFlightRef = useRef(false);

  useEffect(() => {
    const sessionId = session?.id ?? null;
    if (activeSessionIdRef.current === sessionId) {
      return;
    }

    activeSessionIdRef.current = sessionId;
    initialBottomAppliedRef.current = false;
    pendingOlderAnchorRef.current = null;
    olderRequestInFlightRef.current = false;
  }, [session?.id]);

  useEffect(() => {
    const scrollContainer = scrollRef.current;
    if (!scrollContainer || !session || !loaded || loadingNewest || loadingOlder || visibleMessages.length === 0) {
      return;
    }
    if (initialBottomAppliedRef.current || pendingOlderAnchorRef.current) {
      return;
    }

    requestAnimationFrame(() => {
      const currentScrollContainer = scrollRef.current;
      if (!currentScrollContainer) {
        return;
      }
      currentScrollContainer.scrollTop = currentScrollContainer.scrollHeight;
      initialBottomAppliedRef.current = true;
    });
  }, [loaded, loadingNewest, loadingOlder, session, visibleMessages.length]);

  useEffect(() => {
    const anchor = pendingOlderAnchorRef.current;
    const scrollContainer = scrollRef.current;
    if (!anchor || !scrollContainer || loadingOlder) {
      return;
    }

    olderRequestInFlightRef.current = false;
    if (visibleMessages.length <= anchor.messageCount) {
      pendingOlderAnchorRef.current = null;
      return;
    }

    requestAnimationFrame(() => {
      const currentScrollContainer = scrollRef.current;
      const currentAnchor = pendingOlderAnchorRef.current;
      if (!currentScrollContainer || !currentAnchor) {
        return;
      }

      currentScrollContainer.scrollTop =
        currentScrollContainer.scrollHeight - currentAnchor.scrollHeight + currentAnchor.scrollTop;
      pendingOlderAnchorRef.current = null;
    });
  }, [loadingOlder, visibleMessages.length]);

  function handleMessageScroll(event: UIEvent<HTMLOListElement>) {
    if (!canLoadOlder || loadingOlder) {
      return;
    }
    if (event.currentTarget.scrollTop <= 48) {
      loadOlderFromCurrentAnchor();
    }
  }

  function loadOlderFromCurrentAnchor() {
    if (!canLoadOlder || loadingOlder || olderRequestInFlightRef.current) {
      return;
    }

    const scrollContainer = scrollRef.current;
    if (scrollContainer) {
      pendingOlderAnchorRef.current = {
        messageCount: visibleMessages.length,
        scrollHeight: scrollContainer.scrollHeight,
        scrollTop: scrollContainer.scrollTop
      };
    }
    olderRequestInFlightRef.current = true;
    void onLoadOlder?.();
  }

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
      {loadingNewest ? <LoadingBlock label="Loading chat" /> : null}
      {error ? <ErrorPanel title="Chat error" error={error} /> : null}
      {chatRunError ? <ErrorPanel title="Run error" error={chatRunError} /> : null}
      {chatRunId ? <p className="ready-state">Run started: {chatRunId}</p> : null}
      {loaded && !error && visibleMessages.length === 0 ? <p className="empty-state">No visible chat messages found</p> : null}
      {visibleMessages.length > 0 ? (
        <ol className="chat-message-list chat-scroll" onScroll={handleMessageScroll} ref={scrollRef}>
          {canLoadOlder ? (
            <li className="chat-history-top">
              <button className="button button--secondary" disabled={loadingOlder} onClick={loadOlderFromCurrentAnchor} type="button">
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
      {onStartChatRun ? (
        <SessionChatComposer
          disabled={loadingNewest}
          events={chatRunEvents}
          onStart={onStartChatRun}
          onStop={onStopChatRun}
          runId={chatRunId}
          sessionId={session.id}
          source={session.source}
          starting={chatRunStarting}
          stopping={chatRunStopping}
          streaming={chatRunStreaming}
          terminal={chatRunTerminal}
        />
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
