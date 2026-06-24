import { useEffect, useRef, type UIEvent } from "react";
import { ErrorPanel } from "./ErrorPanel";
import { LoadingBlock } from "./LoadingBlock";
import { SessionActionMenu } from "./SessionActionMenu";
import { SessionChatComposer } from "./SessionChatComposer";
import { useCapabilitiesState } from "../state/useCapabilitiesState";
import type {
  ActiveSessionRef,
  QueuedSessionRun,
  StartSessionRunResult
} from "../state/useSessionChatRunState";
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
  chatRunQueuedRuns?: QueuedSessionRun[];
  actionInFlight?: boolean;
  actionResult?: SessionActionResponse | null;
  actionError?: unknown;
  loaded?: boolean;
  error?: unknown;
  onBack?(): void;
  onLoadOlder?(): void;
  onStartAction?(action: string, confirmDestructive: boolean): Promise<void>;
  onStartChatRun?(request: StartRunRequest): Promise<StartSessionRunResult>;
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
  chatRunQueuedRuns = [],
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
  const hasVisibleMessages = visibleMessages.length > 0;
  const canShowTranscript = loaded && !error;
  const showRunMessage = Boolean(
    chatRunStarting || chatRunStreaming || chatRunStopping || chatRunTerminal || chatRunEvents.length > 0 || chatRunId
  );
  const visibleQueuedRuns = session ? chatRunQueuedRuns.filter((queuedRun) => isSameActiveSessionRef(queuedRun.activeSessionRef, { source: session.source, id: session.id })) : [];
  const canShowEmptyTranscript = loaded && !loadingNewest && !error && !hasVisibleMessages && !showRunMessage && visibleQueuedRuns.length === 0;
  const canShowChatList = canShowTranscript && (hasVisibleMessages || showRunMessage || visibleQueuedRuns.length > 0);
  const composerDisabled = loadingNewest || chatRunOtherSessionNonTerminal;
  const sessionCapability = session ? capabilitiesState.capabilityFor(session.source) : null;
  const runChatTimestamp = formatRunChatTimestamp(chatRunEvents, chatRunTerminal);
  const scrollRef = useRef<HTMLOListElement | null>(null);
  const activeSessionKeyRef = useRef<string | null>(null);
  const initialBottomAppliedRef = useRef(false);
  const bottomPinnedRef = useRef(true);
  const userScrollIntentRef = useRef(false);
  const userScrollIntentTimerRef = useRef<number | null>(null);
  const programmaticScrollRef = useRef(false);
  const programmaticScrollTimerRef = useRef<number | null>(null);
  const lastScrollTopRef = useRef(0);
  const pendingOlderAnchorRef = useRef<ScrollAnchor | null>(null);
  const olderRequestInFlightRef = useRef(false);

  useEffect(() => {
    void capabilitiesState.loadCapabilities();
  }, [capabilitiesState.loadCapabilities]);

  useEffect(() => {
    const sessionKey = session ? `${session.source}:${session.id}` : null;
    if (activeSessionKeyRef.current === sessionKey) {
      return;
    }

    activeSessionKeyRef.current = sessionKey;
    initialBottomAppliedRef.current = false;
    bottomPinnedRef.current = true;
    userScrollIntentRef.current = false;
    programmaticScrollRef.current = false;
    lastScrollTopRef.current = 0;
    pendingOlderAnchorRef.current = null;
    olderRequestInFlightRef.current = false;
  }, [session?.id, session?.source]);

  useEffect(() => {
    return () => {
      if (userScrollIntentTimerRef.current !== null) {
        window.clearTimeout(userScrollIntentTimerRef.current);
      }
      if (programmaticScrollTimerRef.current !== null) {
        window.clearTimeout(programmaticScrollTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const scrollContainer = scrollRef.current;
    if (!scrollContainer || !session || !loaded || loadingNewest || loadingOlder || !canShowChatList) {
      return;
    }
    if (initialBottomAppliedRef.current || pendingOlderAnchorRef.current) {
      return;
    }

    scheduleScrollToChatBottom();
    initialBottomAppliedRef.current = true;
  }, [canShowChatList, loaded, loadingNewest, loadingOlder, session, visibleMessages.length]);

  useEffect(() => {
    const scrollContainer = scrollRef.current;
    if (!scrollContainer || !canShowChatList || loadingOlder || pendingOlderAnchorRef.current || !bottomPinnedRef.current) {
      return;
    }

    scheduleScrollToChatBottom();
  }, [
    canShowChatList,
    chatRunEvents.length,
    visibleQueuedRuns.length,
    chatRunStarting,
    chatRunStopping,
    chatRunStreaming,
    chatRunTerminal?.timestamp,
    loadingOlder,
    visibleMessages.length
  ]);

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
    const target = event.currentTarget;
    const nearBottom = isNearScrollBottom(target);
    const scrollTopChanged = Math.abs(target.scrollTop - lastScrollTopRef.current) > 1;
    if (programmaticScrollRef.current) {
      bottomPinnedRef.current = true;
    } else if (nearBottom) {
      bottomPinnedRef.current = true;
    } else if (userScrollIntentRef.current && scrollTopChanged) {
      bottomPinnedRef.current = false;
    }
    lastScrollTopRef.current = target.scrollTop;
    if (!canLoadOlder || loadingOlder) {
      return;
    }
    if (target.scrollTop <= 96) {
      loadOlderFromCurrentAnchor();
    }
  }

  function markUserScrollIntent() {
    userScrollIntentRef.current = true;
    if (userScrollIntentTimerRef.current !== null) {
      window.clearTimeout(userScrollIntentTimerRef.current);
    }
    userScrollIntentTimerRef.current = window.setTimeout(() => {
      userScrollIntentRef.current = false;
      userScrollIntentTimerRef.current = null;
    }, 700);
  }

  function scheduleScrollToChatBottom() {
    requestAnimationFrame(() => {
      scrollToChatBottom();
      requestAnimationFrame(scrollToChatBottom);
    });
  }

  function scrollToChatBottom() {
    const scrollContainer = scrollRef.current;
    if (!scrollContainer) {
      return;
    }
    programmaticScrollRef.current = true;
    bottomPinnedRef.current = true;
    scrollContainer.scrollTop = scrollContainer.scrollHeight;
    lastScrollTopRef.current = scrollContainer.scrollTop;
    if (programmaticScrollTimerRef.current !== null) {
      window.clearTimeout(programmaticScrollTimerRef.current);
    }
    programmaticScrollTimerRef.current = window.setTimeout(() => {
      programmaticScrollRef.current = false;
      programmaticScrollTimerRef.current = null;
    }, 120);
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
          sessionKey={`${session.source}:${session.id}`}
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
      {actionResult ? (
        <p className="ready-state">
          操作已启动：{formatActionLabel(actionResult.action)} · {formatActionStatus(actionResult.status)}
        </p>
      ) : null}
      {canShowEmptyTranscript ? <p className="empty-state">没有可显示的聊天消息</p> : null}
      {canShowTranscript && hasVisibleMessages && canLoadOlder ? (
        <div className="chat-history-control">
          <span>还有更早消息</span>
          <button className="button button--secondary" disabled={loadingOlder} onClick={loadOlderFromCurrentAnchor} type="button">
            {loadingOlder ? "正在加载" : "加载更早"}
          </button>
        </div>
      ) : null}
      {canShowChatList ? (
        <ol
          className="chat-message-list chat-scroll"
          onKeyDown={markUserScrollIntent}
          onPointerDown={markUserScrollIntent}
          onScroll={handleMessageScroll}
          onTouchMove={markUserScrollIntent}
          onWheel={markUserScrollIntent}
          ref={scrollRef}
        >
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
          {showRunMessage ? (
            <li className={chatRunTerminal?.type === "error" ? "chat-message chat-message--assistant chat-message--run chat-message--run-error" : "chat-message chat-message--assistant chat-message--run"}>
              <div className="chat-message__head">
                <strong>{formatRunChatRole(chatRunTerminal)}</strong>
                <time dateTime={runChatTimestamp}>{formatDateTime(runChatTimestamp)}</time>
              </div>
              <div className="chat-run-card" aria-live={chatRunStreaming || chatRunStarting ? "polite" : "off"}>
                <div className="chat-run-card__status">
                  <span className={`chat-run-card__dot chat-run-card__dot--${formatRunChatTone(chatRunStarting, chatRunStreaming, chatRunStopping, chatRunTerminal)}`} />
                  <strong>{formatRunChatTitle(chatRunStarting, chatRunStreaming, chatRunStopping, chatRunTerminal)}</strong>
                  <span className="chat-run-card__meta">{formatRunChatMeta(chatRunId, chatRunEvents.length)}</span>
                </div>
                {chatRunEvents.length > 0 ? (
                  <ol className="chat-run-events" aria-label="运行实时输出">
                    {chatRunEvents.map((event, index) => (
                      <li className={`chat-run-event chat-run-event--${formatRunEventTone(event.type)}`} key={`${event.runId}-${event.timestamp}-${event.type}-${index}`}>
                        <span>{formatRunEventLabel(event.type)}</span>
                        <p>{formatRunEventMessage(event)}</p>
                      </li>
                    ))}
                  </ol>
                ) : (
                  <p className="chat-run-card__pending">等待当前会话返回实时输出</p>
                )}
              </div>
            </li>
          ) : null}
          {visibleQueuedRuns.map((queuedRun, index) => (
            <li className="chat-message chat-message--user chat-message--queued" key={queuedRun.id}>
              <div className="chat-message__head">
                <strong>{index === 0 && !chatRunNonTerminal ? "下一条" : "排队中"}</strong>
                <time dateTime={queuedRun.queuedAt}>{formatDateTime(queuedRun.queuedAt)}</time>
              </div>
              <p>{queuedRun.prompt}</p>
              <span className="chat-message__queue-meta">
                {formatQueuedRunMode(queuedRun.mode)} · {queuedRun.modelId}
              </span>
            </li>
          ))}
        </ol>
      ) : null}
      {onStartChatRun ? (
        <SessionChatComposer
          disabled={composerDisabled}
          nonTerminal={chatRunNonTerminal}
          onStart={onStartChatRun}
          onStop={onStopChatRun}
          runId={chatRunId}
          queuedCount={visibleQueuedRuns.length}
          sessionId={session.id}
          sessionStatus={session.status}
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

function isSameActiveSessionRef(left: ActiveSessionRef, right: ActiveSessionRef): boolean {
  return left.source === right.source && left.id === right.id;
}

function formatQueuedRunMode(mode: string): string {
  if (mode === "ASK") {
    return "问答";
  }
  if (mode === "REVIEW") {
    return "审查";
  }
  if (mode === "EDIT") {
    return "编辑";
  }
  if (mode === "SHELL") {
    return "终端";
  }
  return mode;
}

function isNearScrollBottom(element: HTMLElement): boolean {
  return element.scrollHeight - element.scrollTop - element.clientHeight <= 48;
}

function formatRunChatRole(terminal: RunEventDto | null): string {
  if (terminal?.type === "error") {
    return "错误";
  }
  if (terminal?.type === "stopped") {
    return "已停止";
  }
  return "助手";
}

function formatRunChatTitle(
  starting: boolean,
  streaming: boolean,
  stopping: boolean,
  terminal: RunEventDto | null
): string {
  if (stopping) {
    return "正在停止当前会话";
  }
  if (starting) {
    return "正在发送到当前会话";
  }
  if (streaming) {
    return "Codex 正在处理";
  }
  if (terminal?.type === "done") {
    return "Codex 已完成";
  }
  if (terminal?.type === "error") {
    return "Codex 运行失败";
  }
  if (terminal?.type === "stopped") {
    return "Codex 已停止";
  }
  return "Codex 运行输出";
}

function formatRunChatTone(
  starting: boolean,
  streaming: boolean,
  stopping: boolean,
  terminal: RunEventDto | null
): string {
  if (terminal?.type === "error") {
    return "error";
  }
  if (terminal?.type === "done") {
    return "done";
  }
  if (terminal?.type === "stopped" || stopping) {
    return "stopped";
  }
  if (starting || streaming) {
    return "live";
  }
  return "idle";
}

function formatRunChatMeta(runId: string, eventCount: number): string {
  const runText = runId ? `#${runId.length <= 8 ? runId : runId.slice(0, 8)}` : "等待 run id";
  return `${runText} · ${eventCount} 条事件`;
}

function formatRunChatTimestamp(events: RunEventDto[], terminal: RunEventDto | null): string {
  return terminal?.timestamp ?? events[events.length - 1]?.timestamp ?? new Date().toISOString();
}

function formatRunEventTone(type: string): string {
  if (type === "assistant") {
    return "assistant";
  }
  if (type === "tool" || type === "command") {
    return "tool";
  }
  if (type === "error" || type === "stderr") {
    return "error";
  }
  if (type === "done") {
    return "done";
  }
  if (type === "stopped") {
    return "stopped";
  }
  return "status";
}

function formatRunEventLabel(type: string): string {
  if (type === "assistant") {
    return "助手";
  }
  if (type === "tool") {
    return "工具";
  }
  if (type === "command") {
    return "命令";
  }
  if (type === "stdout") {
    return "输出";
  }
  if (type === "stderr") {
    return "错误";
  }
  if (type === "done") {
    return "完成";
  }
  if (type === "error") {
    return "失败";
  }
  if (type === "stopped") {
    return "停止";
  }
  return "状态";
}

function formatRunEventMessage(event: RunEventDto): string {
  const detail = event.data && typeof event.data.detail === "string" ? event.data.detail.trim() : "";
  if (detail && detail !== event.message) {
    return `${event.message}\n${detail}`;
  }
  return event.message || formatRunEventLabel(event.type);
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
    return "派生";
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
