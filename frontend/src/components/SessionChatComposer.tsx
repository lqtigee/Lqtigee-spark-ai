import { useEffect, useMemo, useRef, useState, type FormEvent, type UIEvent } from "react";
import { ModelSelect } from "./ModelSelect";
import { RunTimeline } from "./RunTimeline";
import { useChatDraftState } from "../state/useChatDraftState";
import { useModelsState } from "../state/useModelsState";
import type { AgentSource, CommandMode, RunEventDto, StartRunRequest } from "../types/api";

const COMMAND_MODES: CommandMode[] = ["ASK", "REVIEW", "EDIT", "SHELL"];

interface SessionChatComposerProps {
  source: AgentSource;
  sessionId: string;
  disabled?: boolean;
  starting?: boolean;
  streaming?: boolean;
  stopping?: boolean;
  runId?: string;
  terminal?: RunEventDto | null;
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
  events = [],
  onStart,
  onStop
}: SessionChatComposerProps) {
  const { draft, setDraft, clearDraft } = useChatDraftState(source, sessionId);
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
  const selectedModelIsAvailable = availableModels.some((model) => model.id === modelId);
  const requiresDangerousConfirmation = mode === "SHELL";
  const stopDisabled = !runId || Boolean(terminal) || stopping || !onStop;
  const sendDisabled =
    disabled ||
    starting ||
    modelsState.loading ||
    !selectedModelIsAvailable ||
    draft.trim().length === 0 ||
    (requiresDangerousConfirmation && !confirmDangerous);

  useEffect(() => {
    void modelsState.loadModels();
  }, [modelsState.loadModels]);

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
      confirmDangerous
    });
    if (returnedRunId) {
      clearDraft();
    }
  }

  function handleStreamScroll(event: UIEvent<HTMLDivElement>) {
    const target = event.currentTarget;
    streamPinnedToBottomRef.current = target.scrollHeight - target.scrollTop - target.clientHeight <= 16;
  }

  return (
    <form className="chat-composer" aria-label="bottom composer" onSubmit={handleSubmit}>
      {(events.length > 0 || streaming) ? (
        <section className="chat-composer__stream" aria-label="Run stream">
          <div className="chat-composer__stream-head">
            <span>{streaming ? "Streaming" : "Run output"}</span>
          </div>
          <div className="chat-composer__stream-body" onScroll={handleStreamScroll} ref={streamRef}>
            <RunTimeline events={events} />
          </div>
        </section>
      ) : null}
      <div className="chat-composer__toolbar" aria-label="Composer tools">
        <ModelSelect
          className="chat-composer__field"
          disabled={disabled || modelsState.loading}
          models={modelsState.models}
          onChange={setModelId}
          source={source}
          value={modelId}
        />
        <button className="button button--danger chat-composer__tool" disabled={stopDisabled} onClick={() => void onStop?.()} type="button">
          {stopping ? "Stopping" : "Stop"}
        </button>
      </div>
      <fieldset className="chat-composer__modes">
        <legend>Mode</legend>
        <div className="chat-composer__mode-grid">
          {COMMAND_MODES.map((commandMode) => (
            <label className={mode === commandMode ? "chat-composer__mode chat-composer__mode--active" : "chat-composer__mode"} key={commandMode}>
              <input
                checked={mode === commandMode}
                disabled={disabled}
                onChange={() => setMode(commandMode)}
                type="radio"
                value={commandMode}
              />
              <span>{commandMode}</span>
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
          <span>Confirm dangerous shell mode</span>
        </label>
      ) : null}
      <label className="chat-composer__prompt">
        <span>Message</span>
        <textarea
          className="input-control chat-composer__textarea"
          disabled={disabled}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="Continue this session"
          rows={2}
          value={draft}
        />
      </label>
      {modelsState.error ? <p className="chat-composer__error">Models failed to load</p> : null}
      <button className="button button--primary chat-composer__send" disabled={sendDisabled} type="submit">
        {starting ? "Sending" : "Send"}
      </button>
    </form>
  );
}
