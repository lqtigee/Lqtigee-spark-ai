import { useEffect, useMemo, useState } from "react";
import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { ModelSelect } from "../components/ModelSelect";
import { PromptComposer } from "../components/PromptComposer";
import { useModelsState } from "../state/useModelsState";
import { useSessionsState } from "../state/useSessionsState";
import type { CommandMode } from "../types/api";

export function ControlPage() {
  const sessionsState = useSessionsState();
  const modelsState = useModelsState();
  const [modelId, setModelId] = useState("");
  const [prompt, setPrompt] = useState("");
  const [mode, setMode] = useState<CommandMode>("ASK");
  const [confirmDangerous, setConfirmDangerous] = useState(false);
  const selectedSession = sessionsState.sessions.find((session) => session.id === sessionsState.selectedSessionId);
  const validationErrors = useMemo(
    () => validateControlForm(Boolean(selectedSession), modelId, prompt, mode, confirmDangerous),
    [selectedSession, modelId, prompt, mode, confirmDangerous]
  );
  const loading = sessionsState.loading || modelsState.loading;

  useEffect(() => {
    void sessionsState.loadSessions();
    void modelsState.loadModels();
  }, [sessionsState.loadSessions, modelsState.loadModels]);

  return (
    <section>
      <h2>Control</h2>
      {loading ? <LoadingBlock label="Loading control data" /> : null}
      {sessionsState.error ? <ErrorPanel title="Sessions error" error={sessionsState.error} /> : null}
      {modelsState.error ? <ErrorPanel title="Models error" error={modelsState.error} /> : null}
      <label>
        Session
        <select
          disabled={sessionsState.sessions.length === 0}
          onChange={(event) => sessionsState.selectSession(event.target.value)}
          value={sessionsState.selectedSessionId}
        >
          <option value="">Select session</option>
          {sessionsState.sessions.map((session) => (
            <option key={session.id} value={session.id}>
              {session.title}
            </option>
          ))}
        </select>
      </label>
      {selectedSession ? (
        <ModelSelect models={modelsState.models} onChange={setModelId} source={selectedSession.source} value={modelId} />
      ) : null}
      <PromptComposer
        confirmDangerous={confirmDangerous}
        disabled={loading}
        mode={mode}
        onConfirmDangerousChange={setConfirmDangerous}
        onModeChange={setMode}
        onPromptChange={setPrompt}
        onSubmit={() => undefined}
        prompt={prompt}
      />
      {validationErrors.length > 0 ? (
        <ul>
          {validationErrors.map((error) => (
            <li key={error}>{error}</li>
          ))}
        </ul>
      ) : (
        <p>Ready</p>
      )}
    </section>
  );
}

function validateControlForm(
  hasSelectedSession: boolean,
  modelId: string,
  prompt: string,
  mode: CommandMode,
  confirmDangerous: boolean
): string[] {
  const errors: string[] = [];

  if (!hasSelectedSession) {
    errors.push("Session is required");
  }
  if (!modelId) {
    errors.push("Model is required");
  }
  if (!prompt.trim()) {
    errors.push("Prompt is required");
  }
  if (mode === "SHELL" && !confirmDangerous) {
    errors.push("Dangerous shell mode must be confirmed");
  }

  return errors;
}
