import { useEffect, useMemo, useRef, useState, type FormEvent, type UIEvent } from "react";
import { ErrorPanel } from "./ErrorPanel";
import { ModelSelect } from "./ModelSelect";
import { RunTimeline } from "./RunTimeline";
import { AttachmentPicker } from "./AttachmentPicker";
import { ChatOptionsDrawer } from "./ChatOptionsDrawer";
import { useAttachmentsState } from "../state/useAttachmentsState";
import { useChatDraftState } from "../state/useChatDraftState";
import { useCapabilitiesState } from "../state/useCapabilitiesState";
import { useModelsState } from "../state/useModelsState";
import type { AgentSource, CommandMode, RunEventDto, SourceCapabilityDto, StartRunRequest } from "../types/api";

const COMMAND_MODES: CommandMode[] = ["ASK", "REVIEW", "EDIT", "SHELL"];
const COMMAND_MODE_LABELS: Record<CommandMode, string> = {
  ASK: "问答",
  EDIT: "编辑",
  REVIEW: "审查",
  SHELL: "终端"
};

interface SessionChatComposerProps {
  source: AgentSource;
  sessionId: string;
  disabled?: boolean;
  starting?: boolean;
  streaming?: boolean;
  stopping?: boolean;
  runId?: string;
  terminal?: RunEventDto | null;
  nonTerminal?: boolean;
  events?: RunEventDto[];
  onStart(request: StartRunRequest): Promise<string | null>;
  onStop?(): Promise<void>;
}

export function SessionChatComposer({
  source,
  sessionId,
  disabled = false,
  starting = false,
  streaming = false,
  stopping = false,
  runId = "",
  terminal = null,
  nonTerminal = false,
  events = [],
  onStart,
  onStop
}: SessionChatComposerProps) {
  const { draft, setDraft, clearDraft } = useChatDraftState(source, sessionId);
  const attachmentsState = useAttachmentsState();
  const capabilitiesState = useCapabilitiesState();
  const modelsState = useModelsState();
  const [modelId, setModelId] = useState("");
  const [mode, setMode] = useState<CommandMode>("ASK");
  const [confirmDangerous, setConfirmDangerous] = useState(false);
  const streamRef = useRef<HTMLDivElement | null>(null);
  const streamPinnedToBottomRef = useRef(true);
  const availableModels = useMemo(
    () => modelsState.models.filter((model) => model.enabled && model.sources.includes(source)),
    [modelsState.models, source]
  );
  const sourceCapability = capabilitiesState.capabilityFor(source);
  const modelSelectionEnabled = hasRunOption(sourceCapability, "model");
  const modelDataUnavailable = modelsState.error || capabilitiesState.error;
  const attachmentEnabled = source === "CODEX"
    ? hasAttachmentCapability(sourceCapability, "image")
    : hasAttachmentCapability(sourceCapability, "file");
  const shellModeEnabled = sourceCapability?.dangerousOptions.includes("shellDangerouslySkipPermissions") ?? false;
  const selectedModelIsAvailable = availableModels.some((model) => model.id === modelId);
  const commandModes = useMemo(
    () => shellModeEnabled ? COMMAND_MODES : COMMAND_MODES.filter((commandMode) => commandMode !== "SHELL"),
    [shellModeEnabled]
  );
  const requiresDangerousConfirmation = mode === "SHELL" && shellModeEnabled;
  const formValid = validateSessionChatForm(
    modelSelectionEnabled && availableModels.length > 0,
    Boolean(modelId),
    selectedModelIsAvailable,
    draft,
    requiresDangerousConfirmation,
    confirmDangerous
  );
  const stopDisabled = !runId || Boolean(terminal) || stopping || !onStop;
  const sendDisabled =
    disabled ||
    starting ||
    nonTerminal ||
    modelsState.loading ||
    capabilitiesState.loading ||
    Boolean(modelDataUnavailable) ||
    !formValid;

  useEffect(() => {
    void modelsState.loadModels();
  }, [modelsState.loadModels]);

  useEffect(() => {
    void capabilitiesState.loadCapabilities();
  }, [capabilitiesState.loadCapabilities]);

  useEffect(() => {
    if (availableModels.length === 0) {
      setModelId("");
      return;
    }
    setModelId((currentModelId) => {
      if (availableModels.some((model) => model.id === currentModelId)) {
        return currentModelId;
      }
      return availableModels[0].id;
    });
  }, [availableModels]);

  useEffect(() => {
    if (mode === "SHELL" && !shellModeEnabled) {
      setMode("ASK");
      setConfirmDangerous(false);
    }
  }, [mode, shellModeEnabled]);

  useEffect(() => {
    if (!streamPinnedToBottomRef.current) {
      return;
    }
    requestAnimationFrame(() => {
      const streamContainer = streamRef.current;
      if (!streamContainer) {
        return;
      }
      streamContainer.scrollTop = streamContainer.scrollHeight;
    });
  }, [events.length]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (sendDisabled) {
      return;
    }

    const returnedRunId = await onStart({
      sessionId,
      source,
      modelId,
      mode,
      prompt: draft,
      confirmDangerous,
      ...buildAttachmentOptions(source, attachmentEnabled ? attachmentsState.attachmentIds : [])
    });
    if (returnedRunId) {
      clearDraft();
      attachmentsState.clearAttachments();
    }
  }

  function handleStreamScroll(event: UIEvent<HTMLDivElement>) {
    const target = event.currentTarget;
    streamPinnedToBottomRef.current = target.scrollHeight - target.scrollTop - target.clientHeight <= 16;
  }

  return (
    <form className="chat-composer" aria-label="底部输入区" onSubmit={handleSubmit}>
      {(events.length > 0 || streaming) ? (
        <section className="chat-composer__stream" aria-label="运行流输出">
          <div className="chat-composer__stream-head">
            <span>{streaming ? "正在流式输出" : "运行输出"}</span>
          </div>
          <div className="chat-composer__stream-body" onScroll={handleStreamScroll} ref={streamRef}>
            <RunTimeline events={events} />
          </div>
        </section>
      ) : null}
      <div className="chat-composer__toolbar" aria-label="输入工具">
        {modelSelectionEnabled ? (
          <ModelSelect
            className="chat-composer__field"
            disabled={disabled || modelsState.loading || capabilitiesState.loading}
            models={modelsState.models}
            onChange={setModelId}
            source={source}
            value={modelId}
          />
        ) : null}
        <ChatOptionsDrawer capability={sourceCapability} disabled={disabled || capabilitiesState.loading || Boolean(capabilitiesState.error)} source={source} />
        <button className="button button--danger chat-composer__tool" disabled={stopDisabled} onClick={() => void onStop?.()} type="button">
          {stopping ? "正在停止" : "停止"}
        </button>
      </div>
      <fieldset className="chat-composer__modes">
        <legend>模式</legend>
        <div className="chat-composer__mode-grid">
          {commandModes.map((commandMode) => (
            <label className={mode === commandMode ? "chat-composer__mode chat-composer__mode--active" : "chat-composer__mode"} key={commandMode}>
              <input
                checked={mode === commandMode}
                disabled={disabled}
                onChange={() => setMode(commandMode)}
                type="radio"
                value={commandMode}
              />
              <span>{COMMAND_MODE_LABELS[commandMode]}</span>
            </label>
          ))}
        </div>
      </fieldset>
      {requiresDangerousConfirmation ? (
        <label className="chat-composer__confirm">
          <input
            checked={confirmDangerous}
            disabled={disabled}
            onChange={(event) => setConfirmDangerous(event.target.checked)}
            type="checkbox"
          />
          <span>确认危险终端模式</span>
        </label>
      ) : null}
      {attachmentEnabled ? (
        <AttachmentPicker
          attachments={attachmentsState.attachments}
          deletingIds={attachmentsState.deletingIds}
          disabled={disabled || starting || nonTerminal}
          error={attachmentsState.error}
          onDelete={attachmentsState.deleteUploadedAttachment}
          onUpload={attachmentsState.uploadFile}
          uploading={attachmentsState.uploading}
        />
      ) : null}
      <label className="chat-composer__prompt">
        <span>消息</span>
        <textarea
          className="input-control chat-composer__textarea"
          disabled={disabled}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="继续当前会话"
          rows={2}
          value={draft}
        />
      </label>
      {capabilitiesState.error ? <ErrorPanel title="能力加载失败" error={capabilitiesState.error} /> : null}
      {modelsState.error ? <ErrorPanel title="模型加载失败" error={modelsState.error} /> : null}
      <button className="button button--primary chat-composer__send" disabled={sendDisabled} type="submit">
        {starting ? "发送中" : "发送"}
      </button>
    </form>
  );
}

function hasRunOption(capability: SourceCapabilityDto | null, option: string): boolean {
  return capability?.runOptions.includes(option) ?? false;
}

function hasAttachmentCapability(capability: SourceCapabilityDto | null, attachment: string): boolean {
  return capability?.attachments.includes(attachment) ?? false;
}

function buildAttachmentOptions(source: AgentSource, attachmentIds: string[]): Pick<StartRunRequest, "codexOptions" | "opencodeOptions"> {
  if (attachmentIds.length === 0) {
    return {};
  }
  if (source === "CODEX") {
    return {
      codexOptions: {
        imageAttachmentIds: attachmentIds
      }
    };
  }
  return {
    opencodeOptions: {
      fileAttachmentIds: attachmentIds
    }
  };
}

function validateSessionChatForm(
  hasAvailableModels: boolean,
  hasModelId: boolean,
  selectedModelIsAvailable: boolean,
  prompt: string,
  requiresDangerousConfirmation: boolean,
  confirmDangerous: boolean
): boolean {
  return (
    hasAvailableModels &&
    hasModelId &&
    selectedModelIsAvailable &&
    prompt.trim().length > 0 &&
    (!requiresDangerousConfirmation || confirmDangerous)
  );
}
