import { useEffect, useMemo, useState } from "react";
import { startRun } from "../api/remoteApi";
import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { ModelSelect } from "../components/ModelSelect";
import { PromptComposer } from "../components/PromptComposer";
import { SessionDetail } from "../components/SessionDetail";
import { useModelsState } from "../state/useModelsState";
import { useSessionsState } from "../state/useSessionsState";
import type { AgentSource, CommandMode, RemoteSession } from "../types/api";

const TOKEN_KEY = "lqtigee_token";

type SourceFilter = "ALL" | AgentSource;

export function ControlPage() {
  const sessionsState = useSessionsState();
  const modelsState = useModelsState();
  const hasToken = Boolean((localStorage.getItem(TOKEN_KEY) ?? "").trim());
  const [query, setQuery] = useState("");
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>("ALL");
  const [modelId, setModelId] = useState("");
  const [prompt, setPrompt] = useState("");
  const [mode, setMode] = useState<CommandMode>("ASK");
  const [confirmDangerous, setConfirmDangerous] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [runError, setRunError] = useState<unknown>(null);
  const selectedSession = sessionsState.sessions.find((session) => session.id === sessionsState.selectedSessionId);
  const filteredSessions = useMemo(
    () => filterSessions(sessionsState.sessions, sourceFilter, query),
    [query, sessionsState.sessions, sourceFilter]
  );
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
    if (hasToken) {
      void sessionsState.loadSessions();
      void modelsState.loadModels();
    }
  }, [hasToken, sessionsState.loadSessions, modelsState.loadModels]);

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
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Selected-session run</p>
          <h2>Control</h2>
        </div>
        <button className="button button--secondary" disabled={!hasToken || loading} onClick={() => void reloadControlData(sessionsState.loadSessions, modelsState.loadModels)} type="button">
          Reload
        </button>
      </div>

      {!hasToken ? (
        <section className="action-panel action-panel--warning">
          <div>
            <h3>Token required</h3>
            <p>Remote commands require the API token.</p>
          </div>
          <a className="button button--primary" href="/settings">
            Settings
          </a>
        </section>
      ) : null}

      {loading ? <LoadingBlock label="Loading control data" /> : null}
      {sessionsState.error ? <ErrorPanel title="Sessions error" error={sessionsState.error} /> : null}
      {modelsState.error ? <ErrorPanel title="Models error" error={modelsState.error} /> : null}
      {runError ? <ErrorPanel title="Run error" error={runError} /> : null}

      {hasToken ? (
        <>
          <section className="control-panel">
            <div className="section-title">
              <h3>Session</h3>
              <a className="text-link" href="/sessions">
                Browse
              </a>
            </div>
            <div className="filter-bar">
              <label className="field field--compact">
                <span>Search</span>
                <input className="input-control" onChange={(event) => setQuery(event.target.value)} value={query} type="search" />
              </label>
              <div className="segmented-control" role="group" aria-label="Control session source filter">
                <button className={sourceFilter === "ALL" ? "is-active" : ""} onClick={() => setSourceFilter("ALL")} type="button">
                  All
                </button>
                <button className={sourceFilter === "CODEX" ? "is-active" : ""} onClick={() => setSourceFilter("CODEX")} type="button">
                  Codex
                </button>
                <button className={sourceFilter === "OPENCODE" ? "is-active" : ""} onClick={() => setSourceFilter("OPENCODE")} type="button">
                  opencode
                </button>
              </div>
            </div>
            <label className="field">
              <span>Selected session</span>
              <select
                className="input-control"
                disabled={filteredSessions.length === 0}
                onChange={(event) => sessionsState.selectSession(event.target.value)}
                value={sessionsState.selectedSessionId}
              >
                <option value="">Select session</option>
                {filteredSessions.map((session) => (
                  <option key={session.id} value={session.id}>
                    {session.source} - {session.title}
                  </option>
                ))}
              </select>
            </label>
            <SessionDetail session={selectedSession} />
          </section>

          <section className="control-panel">
            <div className="section-title">
              <h3>Command</h3>
              {selectedSession ? <span className="source-pill">{selectedSession.source}</span> : null}
            </div>
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
              submitDisabled={validationErrors.length > 0}
            />
            {validationErrors.length > 0 ? (
              <ul className="validation-list">
                {validationErrors.map((error) => (
                  <li key={error}>{error}</li>
                ))}
              </ul>
            ) : (
              <p className="ready-state">Ready</p>
            )}
          </section>
        </>
      ) : null}
    </section>
  );
}

async function reloadControlData(loadSessions: () => Promise<void>, loadModels: () => Promise<void>): Promise<void> {
  await Promise.all([loadSessions(), loadModels()]);
}

function filterSessions(sessions: RemoteSession[], sourceFilter: SourceFilter, query: string): RemoteSession[] {
  const normalizedQuery = query.trim().toLowerCase();
  return sessions.filter((session) => {
    const sourceMatches = sourceFilter === "ALL" || session.source === sourceFilter;
    const queryMatches =
      !normalizedQuery ||
      [session.title, session.workspace, session.model, session.lastMessage, session.rawFile]
        .join(" ")
        .toLowerCase()
        .includes(normalizedQuery);
    return sourceMatches && queryMatches;
  });
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
