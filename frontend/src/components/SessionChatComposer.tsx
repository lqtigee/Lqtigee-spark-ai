import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { ErrorPanel } from "./ErrorPanel";
import { AttachmentPicker } from "./AttachmentPicker";
import { ChatOptionsDrawer } from "./ChatOptionsDrawer";
import { listCodexSkills } from "../api/remoteApi";
import { useAttachmentsState } from "../state/useAttachmentsState";
import { useChatDraftState } from "../state/useChatDraftState";
import { useCapabilitiesState } from "../state/useCapabilitiesState";
import { useModelsState } from "../state/useModelsState";
import type {
  AgentSource,
  CodexReasoningEffort,
  CodexSkillDto,
  CommandMode,
  RunEventDto,
  SessionStatus,
  SourceCapabilityDto,
  StartRunRequest
} from "../types/api";

const COMMAND_MODES: CommandMode[] = ["ASK", "REVIEW", "EDIT", "SHELL"];
const OPENCODE_OPTIONS_KEY = "lqtigee_opencode_options";
const TOKEN_KEY = "lqtigee_token";
const CODEX_REASONING_EFFORTS: CodexReasoningEffort[] = ["low", "medium", "high", "xhigh"];
const COMMAND_MODE_LABELS: Record<CommandMode, string> = {
  ASK: "问答",
  EDIT: "编辑",
  REVIEW: "审查",
  SHELL: "终端"
};

interface SessionChatComposerProps {
  source: AgentSource;
  sessionId: string;
  sessionStatus: SessionStatus;
  disabled?: boolean;
  starting?: boolean;
  streaming?: boolean;
  stopping?: boolean;
  runId?: string;
  terminal?: RunEventDto | null;
  nonTerminal?: boolean;
  onStart(request: StartRunRequest): Promise<string | null>;
  onStop?(): Promise<void>;
}

interface StoredOpencodeOptions {
  agent?: string;
  fork?: boolean;
  replay?: boolean;
  replayLimit?: number;
  share?: boolean;
  thinking?: boolean;
  variant?: string;
}

export function SessionChatComposer({
  source,
  sessionId,
  sessionStatus,
  disabled = false,
  starting = false,
  streaming = false,
  stopping = false,
  runId = "",
  terminal = null,
  nonTerminal = false,
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
  const [codexSkills, setCodexSkills] = useState<CodexSkillDto[]>([]);
  const [codexSkillsLoading, setCodexSkillsLoading] = useState(false);
  const [codexSkillsError, setCodexSkillsError] = useState<unknown>(null);
  const [selectedSkillId, setSelectedSkillId] = useState("");
  const [selectedReasoningEffort, setSelectedReasoningEffort] = useState<CodexReasoningEffort | "">("");
  const composerSessionKeyRef = useRef(`${source}:${sessionId}`);
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
  const currentRunStatus = formatRunStatus(starting, streaming, stopping, terminal, runId);
  const selectedSkill = useMemo(
    () => codexSkills.find((skill) => skill.id === selectedSkillId) ?? null,
    [codexSkills, selectedSkillId]
  );
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
    if (source !== "CODEX") {
      setCodexSkills([]);
      setCodexSkillsLoading(false);
      setCodexSkillsError(null);
      setSelectedSkillId("");
      setSelectedReasoningEffort("");
      return;
    }
    if (!localStorage.getItem(TOKEN_KEY)) {
      setCodexSkills([]);
      setCodexSkillsLoading(false);
      setCodexSkillsError(null);
      return;
    }

    let cancelled = false;
    setCodexSkillsLoading(true);
    setCodexSkillsError(null);
    void listCodexSkills()
      .then((response) => {
        if (cancelled) {
          return;
        }
        setCodexSkills(response.codexSkills);
      })
      .catch((caughtError: unknown) => {
        if (cancelled) {
          return;
        }
        setCodexSkills([]);
        setSelectedSkillId("");
        setCodexSkillsError(caughtError);
      })
      .finally(() => {
        if (!cancelled) {
          setCodexSkillsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [source]);

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
    const nextComposerSessionKey = `${source}:${sessionId}`;
    if (composerSessionKeyRef.current === nextComposerSessionKey) {
      return;
    }
    composerSessionKeyRef.current = nextComposerSessionKey;
    attachmentsState.clearAttachments();
    setMode("ASK");
    setConfirmDangerous(false);
    setSelectedSkillId("");
    setSelectedReasoningEffort("");
  }, [attachmentsState.clearAttachments, source, sessionId]);

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
      prompt: buildPromptWithSkill(draft, selectedSkill),
      confirmDangerous,
      ...buildSourceOptions(source, sourceCapability, attachmentEnabled ? attachmentsState.attachmentIds : [], selectedReasoningEffort)
    });
    if (returnedRunId) {
      clearDraft();
      attachmentsState.clearAttachments();
      setSelectedSkillId("");
    }
  }

  return (
    <form className="chat-composer" aria-label="底部输入区" onSubmit={handleSubmit}>
      <div className="chat-composer__box">
        <div className="chat-composer__status" aria-live="polite">
          <span>
            会话：<strong>{formatSessionStatusLabel(sessionStatus)}</strong>
          </span>
          <span>
            运行：<strong>{currentRunStatus.label}</strong>
            <em>{currentRunStatus.detail}</em>
          </span>
        </div>
        <div className="chat-composer__input-row">
          {selectedSkill ? (
            <div className="chat-composer__directives" aria-label="已选择指令">
              <button
                className="chat-composer__directive"
                disabled={disabled || starting || nonTerminal}
                onClick={() => setSelectedSkillId("")}
                type="button"
              >
                ${selectedSkill.name}
              </button>
            </div>
          ) : null}
          <textarea
            aria-label="消息"
            className="chat-composer__textarea"
            disabled={disabled}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="继续当前会话"
            rows={2}
            value={draft}
          />
        </div>
        <div className="chat-composer__tool-row" aria-label="输入工具">
          {modelSelectionEnabled ? (
            <select
              aria-label="选择模型"
              className="chat-composer__model-select"
              disabled={disabled || modelsState.loading || capabilitiesState.loading}
              onChange={(event) => setModelId(event.target.value)}
              value={availableModels.length === 0 ? "" : modelId}
            >
              {availableModels.length > 0 ? <option value="">模型</option> : null}
              {availableModels.length === 0 ? <option value="">暂无模型</option> : null}
              {availableModels.map((model) => (
                <option key={model.id} value={model.id}>
                  {model.label}
                </option>
              ))}
            </select>
          ) : null}
          {source === "CODEX" ? (
            <select
              aria-label="选择 Skill"
              className="chat-composer__skill-select"
              disabled={disabled || starting || nonTerminal || codexSkillsLoading || codexSkills.length === 0}
              onChange={(event) => setSelectedSkillId(event.target.value)}
              value={selectedSkillId}
            >
              <option value="">{codexSkillsLoading ? "加载 Skill" : codexSkills.length === 0 ? "暂无 Skill" : "Skill"}</option>
              {codexSkills.map((skill) => (
                <option key={skill.id} value={skill.id}>
                  {skill.name}
                </option>
              ))}
            </select>
          ) : null}
          {source === "CODEX" ? (
            <select
              aria-label="选择推理档位"
              className="chat-composer__reasoning"
              disabled={disabled || starting || nonTerminal}
              onChange={(event) => setSelectedReasoningEffort(toCodexReasoningEffort(event.target.value))}
              value={selectedReasoningEffort}
            >
              <option value="">推理</option>
              {CODEX_REASONING_EFFORTS.map((effort) => (
                <option key={effort} value={effort}>
                  {effort}
                </option>
              ))}
            </select>
          ) : null}
          <div className="chat-composer__mode-pills" aria-label="输入模式">
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
          {source === "CODEX" && attachmentEnabled ? (
            <AttachmentPicker
              accept="image/*"
              attachments={attachmentsState.attachments}
              deletingIds={attachmentsState.deletingIds}
              disabled={disabled || starting || nonTerminal}
              error={attachmentsState.error}
              onDelete={attachmentsState.deleteUploadedAttachment}
              onUpload={attachmentsState.uploadFile}
              uploading={attachmentsState.uploading}
            />
          ) : null}
          {source === "OPENCODE" && attachmentEnabled ? (
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
          {source === "OPENCODE" ? (
            <ChatOptionsDrawer capability={sourceCapability} disabled={disabled || capabilitiesState.loading || Boolean(capabilitiesState.error)} source={source} />
          ) : null}
          <button className="button button--danger chat-composer__tool" disabled={stopDisabled} onClick={() => void onStop?.()} type="button">
            {stopping ? "正在停止" : "停止"}
          </button>
          <button className="button button--primary chat-composer__send" disabled={sendDisabled} type="submit">
            {starting ? "发送中" : "发送"}
          </button>
        </div>
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
      </div>
      {capabilitiesState.error ? <ErrorPanel title="能力加载失败" error={capabilitiesState.error} /> : null}
      {modelsState.error ? <ErrorPanel title="模型加载失败" error={modelsState.error} /> : null}
      {codexSkillsError ? <ErrorPanel title="Skill 加载失败" error={codexSkillsError} /> : null}
    </form>
  );
}

function formatRunStatus(
  starting: boolean,
  streaming: boolean,
  stopping: boolean,
  terminal: RunEventDto | null,
  runId: string
): { label: string; detail: string } {
  if (stopping) {
    return { label: "正在停止", detail: runId ? shortRunId(runId) : "等待结束" };
  }
  if (starting) {
    return { label: "执行中", detail: "正在启动" };
  }
  if (streaming) {
    return { label: "执行中", detail: runId ? shortRunId(runId) : "实时输出" };
  }
  if (terminal) {
    return { label: "已结束", detail: formatRunEventType(terminal.type) };
  }
  return { label: "未运行", detail: "等待发送" };
}

function formatSessionStatusLabel(status: SessionStatus): string {
  if (status === "ACTIVE") {
    return "活跃";
  }
  if (status === "IDLE") {
    return "空闲";
  }
  if (status === "RUNNING") {
    return "运行中";
  }
  if (status === "FAILED") {
    return "失败";
  }
  return "未知";
}

function formatRunEventType(type: string): string {
  if (type === "done") {
    return "完成";
  }
  if (type === "error") {
    return "错误";
  }
  if (type === "stopped") {
    return "已停止";
  }
  return type;
}

function shortRunId(runId: string): string {
  return runId.length <= 8 ? runId : runId.slice(0, 8);
}

function hasRunOption(capability: SourceCapabilityDto | null, option: string): boolean {
  return capability?.runOptions.includes(option) ?? false;
}

function hasAttachmentCapability(capability: SourceCapabilityDto | null, attachment: string): boolean {
  return capability?.attachments.includes(attachment) ?? false;
}

function buildSourceOptions(
  source: AgentSource,
  capability: SourceCapabilityDto | null,
  attachmentIds: string[],
  selectedReasoningEffort: CodexReasoningEffort | ""
): Pick<StartRunRequest, "codexOptions" | "opencodeOptions"> {
  if (source === "CODEX") {
    const configOverrides = selectedReasoningEffort
      ? [{ key: "model_reasoning_effort", value: selectedReasoningEffort }]
      : null;
    if (attachmentIds.length === 0 && !configOverrides) {
      return {};
    }
    return {
      codexOptions: {
        imageAttachmentIds: attachmentIds.length > 0 ? attachmentIds : null,
        configOverrides
      }
    };
  }

  const storedOptions = readStoredOpencodeOptions();
  const enabledRunOptions = capability?.runOptions ?? [];

  return {
    opencodeOptions: {
      agent: enabledRunOptions.includes("agent") ? nonBlankString(storedOptions.agent) : null,
      fileAttachmentIds: attachmentIds.length > 0 ? attachmentIds : null,
      fork: enabledRunOptions.includes("fork") ? storedOptions.fork ?? null : null,
      replay: enabledRunOptions.includes("replay") ? storedOptions.replay ?? null : null,
      replayLimit: enabledRunOptions.includes("replayLimit") ? validReplayLimit(storedOptions.replayLimit) : null,
      share: enabledRunOptions.includes("share") ? storedOptions.share ?? null : null,
      thinking: enabledRunOptions.includes("thinking") ? storedOptions.thinking ?? null : null,
      variant: enabledRunOptions.includes("variant") ? nonBlankString(storedOptions.variant) : null
    }
  };
}

function buildPromptWithSkill(draft: string, selectedSkill: CodexSkillDto | null): string {
  if (!selectedSkill) {
    return draft;
  }
  return `$${selectedSkill.name}\n\n${draft}`;
}

function toCodexReasoningEffort(value: string): CodexReasoningEffort | "" {
  return CODEX_REASONING_EFFORTS.includes(value as CodexReasoningEffort) ? value as CodexReasoningEffort : "";
}

function readStoredOpencodeOptions(): StoredOpencodeOptions {
  try {
    const rawValue = localStorage.getItem(OPENCODE_OPTIONS_KEY);
    if (!rawValue) {
      return {};
    }
    const parsed = JSON.parse(rawValue) as StoredOpencodeOptions;
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function nonBlankString(value: string | undefined): string | null {
  if (!value || !value.trim()) {
    return null;
  }
  return value.trim();
}

function validReplayLimit(value: number | undefined): number | null {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return null;
  }
  return value;
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
