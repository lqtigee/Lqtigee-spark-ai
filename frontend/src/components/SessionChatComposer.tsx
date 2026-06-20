import { useEffect, useMemo, useState } from "react";
import { ModelSelect } from "./ModelSelect";
import { useChatDraftState } from "../state/useChatDraftState";
import { useModelsState } from "../state/useModelsState";
import type { AgentSource, CommandMode } from "../types/api";

const COMMAND_MODES: CommandMode[] = ["ASK", "REVIEW", "EDIT", "SHELL"];

interface SessionChatComposerProps {
  source: AgentSource;
  sessionId: string;
  disabled?: boolean;
}

export function SessionChatComposer({ source, sessionId, disabled = false }: SessionChatComposerProps) {
  const { draft, setDraft } = useChatDraftState(source, sessionId);
  const modelsState = useModelsState();
  const [modelId, setModelId] = useState("");
  const [mode, setMode] = useState<CommandMode>("ASK");
  const [confirmDangerous, setConfirmDangerous] = useState(false);
  const availableModels = useMemo(
    () => modelsState.models.filter((model) => model.enabled && model.sources.includes(source)),
    [modelsState.models, source]
  );
  const selectedModelIsAvailable = availableModels.some((model) => model.id === modelId);
  const requiresDangerousConfirmation = mode === "SHELL";
  const sendDisabled =
    disabled ||
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

  return (
    <form className="chat-composer" aria-label="bottom composer">
      <div className="chat-composer__toolbar" aria-label="Composer tools">
        <ModelSelect
          className="chat-composer__field"
          disabled={disabled || modelsState.loading}
          models={modelsState.models}
          onChange={setModelId}
          source={source}
          value={modelId}
        />
        <button className="button button--danger chat-composer__tool" disabled type="button">
          Stop
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
      <button className="button button--primary chat-composer__send" disabled={sendDisabled} type="button">
        Send
      </button>
    </form>
  );
}
