import { useEffect, useRef, type UIEvent } from "react";
import { ErrorPanel } from "./ErrorPanel";
import { LoadingBlock } from "./LoadingBlock";
import { SessionActionMenu } from "./SessionActionMenu";
import { SessionChatComposer } from "./SessionChatComposer";
import { useCapabilitiesState } from "../state/useCapabilitiesState";
import type {
  RemoteSession,
  RunEventDto,
  SessionActionResponse,
  SessionMessageDto,
  SessionTranscriptDto,
  StartRunRequest,
  TranscriptPageInfoDto
} from "../types/api";

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
  chatRunNonTerminal?: boolean;
  chatRunOtherSessionNonTerminal?: boolean;
  chatRunEvents?: RunEventDto[];
  chatRunError?: unknown;
  chatRunId?: string;
  actionInFlight?: boolean;
  actionResult?: SessionActionResponse | null;
  actionError?: unknown;
  loaded?: boolean;
  error?: unknown;
  onBack?(): void;
  onLoadOlder?(): void;
  onStartAction?(action: string, confirmDestructive: boolean): Promise<void>;
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
  chatRunNonTerminal = false,
  chatRunOtherSessionNonTerminal = false,
  chatRunEvents = [],
  chatRunError = null,
  chatRunId = "",
  actionInFlight = false,
  actionResult = null,
  actionError = null,
  loaded = false,
  error = null,
  onBack,
  onLoadOlder,
  onStartAction,
  onStartChatRun,
  onStopChatRun
}: SessionDetailProps) {
  const capabilitiesState = useCapabilitiesState();
  const visibleMessages = messages ?? transcript?.messages ?? [];
  const canLoadOlder = Boolean(pageInfo?.hasMoreBefore && onLoadOlder);
  const canShowTranscript = loaded && !loadingNewest && !error;
  const composerDisabled = loadingNewest || chatRunOtherSessionNonTerminal;
  const sessionCapability = session ? capabilitiesState.capabilityFor(session.source) : null;
  const scrollRef = useRef<HTMLOListElement | null>(null);
  const activeSessionIdRef = useRef<string | null>(null);
  const initialBottomAppliedRef = useRef(false);
  const pendingOlderAnchorRef = useRef<ScrollAnchor | null>(null);
  const olderRequestInFlightRef = useRef(false);

  useEffect(() => {
    void capabilitiesState.loadCapabilities();
  }, [capabilitiesState.loadCapabilities]);

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
    return <p className="empty-state">未选择会话</p>;
  }

  return (
    <section className="detail-panel chat-panel">
      <div className="chat-panel__header">
        {onBack ? (
          <button className="button button--secondary chat-panel__back" onClick={onBack} type="button">
            返回
          </button>
        ) : null}
        <div className="chat-panel__identity">
          <h3>{session.title}</h3>
          <p>
            <span>{session.source}</span>
            <span>{session.model}</span>
          </p>
        </div>
        <SessionActionMenu
          actionInFlight={actionInFlight}
          actions={sessionCapability?.sessionActions ?? []}
          capabilitiesError={capabilitiesState.error}
          capabilitiesLoading={capabilitiesState.loading}
          onStartAction={onStartAction}
        />
      </div>
      <dl className="chat-panel__meta">
        <div>
          <dt>工作目录</dt>
          <dd>{session.workspace}</dd>
        </div>
        <div>
          <dt>更新时间</dt>
          <dd>{formatDateTime(session.updatedAt)}</dd>
        </div>
      </dl>
      {loadingNewest ? <LoadingBlock label="正在加载聊天" /> : null}
      {error ? <ErrorPanel title="聊天加载失败" error={error} /> : null}
      {chatRunError ? <ErrorPanel title="运行失败" error={chatRunError} /> : null}
      {chatRunOtherSessionNonTerminal ? <p className="ready-state">其他会话正在运行，请等待其结束后再发送。</p> : null}
      {actionError ? <ErrorPanel title="会话操作失败" error={actionError} /> : null}
      {chatRunId ? <p className="ready-state">运行已启动：{chatRunId}</p> : null}
      {actionResult ? (
        <p className="ready-state">
          操作已启动：{formatActionLabel(actionResult.action)} · {formatActionStatus(actionResult.status)}
        </p>
      ) : null}
      {canShowTranscript && visibleMessages.length === 0 ? <p className="empty-state">没有可显示的聊天消息</p> : null}
      {canShowTranscript && visibleMessages.length > 0 ? (
        <ol className="chat-message-list chat-scroll" onScroll={handleMessageScroll} ref={scrollRef}>
          {canLoadOlder ? (
            <li className="chat-history-top">
              <button className="button button--secondary" disabled={loadingOlder} onClick={loadOlderFromCurrentAnchor} type="button">
                {loadingOlder ? "正在加载更早消息" : "加载更早消息"}
              </button>
            </li>
          ) : null}
          {visibleMessages.map((message) => (
            <li className={`chat-message chat-message--${message.role}`} key={message.id}>
              <div className="chat-message__head">
                <strong>{formatMessageRole(message.role)}</strong>
                <time dateTime={message.timestamp}>{formatDateTime(message.timestamp)}</time>
              </div>
              <p>{message.text}</p>
            </li>
          ))}
        </ol>
      ) : null}
      {onStartChatRun ? (
        <SessionChatComposer
          disabled={composerDisabled}
          events={chatRunEvents}
          nonTerminal={chatRunNonTerminal}
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

function formatMessageRole(role: string): string {
  if (role === "user") {
    return "用户";
  }
  if (role === "assistant") {
    return "助手";
  }
  return role;
}

function formatActionLabel(action: string): string {
  if (action === "archive") {
    return "归档";
  }
  if (action === "delete") {
    return "删除";
  }
  if (action === "export") {
    return "导出";
  }
  if (action === "fork") {
    return "Fork";
  }
  if (action === "import") {
    return "导入";
  }
  if (action === "unarchive") {
    return "取消归档";
  }
  return action;
}

function formatActionStatus(status: string): string {
  if (status === "STARTED") {
    return "已启动";
  }
  if (status === "COMPLETED") {
    return "已完成";
  }
  if (status === "FAILED") {
    return "失败";
  }
  if (status === "REJECTED") {
    return "已拒绝";
  }
  return status;
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}
