import { useEffect, useMemo, useState } from "react";
import { startRun } from "../api/remoteApi";
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
  const [submitting, setSubmitting] = useState(false);
  const [runError, setRunError] = useState<unknown>(null);
  const selectedSession = sessionsState.sessions.find((session) => session.id === sessionsState.selectedSessionId);
  const availableModels = useMemo(
    () =>
      selectedSession
        ? modelsState.models.filter((model) => model.enabled && model.sources.includes(selectedSession.source))
        : [],
    [modelsState.models, selectedSession]
  );
  const selectedModel = availableModels.find((model) => model.id === modelId);
  const validationErrors = useMemo(
    () => validateControlForm(Boolean(selectedSession), Boolean(modelId), Boolean(selectedModel), prompt, mode, confirmDangerous),
    [selectedSession, modelId, selectedModel, prompt, mode, confirmDangerous]
  );
  const loading = sessionsState.loading || modelsState.loading || submitting;

  useEffect(() => {
    void sessionsState.loadSessions();
    void modelsState.loadModels();
  }, [sessionsState.loadSessions, modelsState.loadModels]);

  useEffect(() => {
    if (modelId && !selectedModel) {
      setModelId("");
    }
  }, [modelId, selectedModel]);

  async function handleSubmit() {
    if (!selectedSession || !selectedModel || validationErrors.length > 0) {
      return;
    }

    setRunError(null);
    setSubmitting(true);
    try {
      const response = await startRun({
        sessionId: selectedSession.id,
        source: selectedSession.source,
        modelId: selectedModel.id,
        mode,
        prompt,
        confirmDangerous
      });
      window.location.href = `/runs?runId=${encodeURIComponent(response.runId)}`;
    } catch (caughtError) {
      setRunError(caughtError);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <h2>Control</h2>
      {loading ? <LoadingBlock label="Loading control data" /> : null}
      {sessionsState.error ? <ErrorPanel title="Sessions error" error={sessionsState.error} /> : null}
      {modelsState.error ? <ErrorPanel title="Models error" error={modelsState.error} /> : null}
      {runError ? <ErrorPanel title="Run error" error={runError} /> : null}
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
        onSubmit={() => void handleSubmit()}
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
  hasModelId: boolean,
  hasSelectedModel: boolean,
  prompt: string,
  mode: CommandMode,
  confirmDangerous: boolean
): string[] {
  const errors: string[] = [];

  if (!hasSelectedSession) {
    errors.push("Session is required");
  }
  if (!hasModelId) {
    errors.push("Model is required");
  }
  if (hasModelId && !hasSelectedModel) {
    errors.push("Model must support selected session source");
  }
  if (!prompt.trim()) {
    errors.push("Prompt is required");
  }
  if (mode === "SHELL" && !confirmDangerous) {
    errors.push("Dangerous shell mode must be confirmed");
  }

  return errors;
}
