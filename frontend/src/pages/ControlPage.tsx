import { useEffect, useMemo, useState } from "react";
import { startRun } from "../api/remoteApi";
import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { ModelSelect } from "../components/ModelSelect";
import { PromptComposer } from "../components/PromptComposer";
import { SessionDetail } from "../components/SessionDetail";
import { useModelsState } from "../state/useModelsState";
import { useSessionsState } from "../state/useSessionsState";
import type { AgentSource, CommandMode, RemoteSession, SelectedSessionRef } from "../types/api";

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
  const selectedSession = sessionsState.sessions.find((session) => isSelectedSession(session, sessionsState.selectedSessionRef));
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
          <p className="eyebrow">选中会话运行</p>
          <h2>控制</h2>
        </div>
        <button className="button button--secondary" disabled={!hasToken || loading} onClick={() => void reloadControlData(sessionsState.loadSessions, modelsState.loadModels)} type="button">
          刷新
        </button>
      </div>

      {!hasToken ? (
        <section className="action-panel action-panel--warning">
          <div>
            <h3>需要令牌</h3>
            <p>远程命令需要 API 令牌。</p>
          </div>
          <a className="button button--primary" href="/settings">
            设置
          </a>
        </section>
      ) : null}

      {loading ? <LoadingBlock label="正在加载控制数据" /> : null}
      {sessionsState.error ? <ErrorPanel title="会话加载失败" error={sessionsState.error} /> : null}
      {modelsState.error ? <ErrorPanel title="模型加载失败" error={modelsState.error} /> : null}
      {runError ? <ErrorPanel title="运行失败" error={runError} /> : null}

      {hasToken ? (
        <>
          <section className="control-panel">
            <div className="section-title">
              <h3>会话</h3>
              <a className="text-link" href="/sessions">
                浏览
              </a>
            </div>
            <div className="filter-bar">
              <label className="field field--compact">
                <span>搜索</span>
                <input className="input-control" onChange={(event) => setQuery(event.target.value)} value={query} type="search" />
              </label>
              <div className="segmented-control" role="group" aria-label="控制页会话来源筛选">
                <button className={sourceFilter === "ALL" ? "is-active" : ""} onClick={() => setSourceFilter("ALL")} type="button">
                  全部
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
              <span>已选会话</span>
              <select
                className="input-control"
                disabled={filteredSessions.length === 0}
                onChange={(event) => sessionsState.selectSession(findSessionBySelectValue(filteredSessions, event.target.value))}
                value={selectedSession ? sessionSelectValue(selectedSession) : ""}
              >
                <option value="">选择会话</option>
                {filteredSessions.map((session) => (
                  <option key={`${session.source}:${session.id}`} value={sessionSelectValue(session)}>
                    {session.source} - {session.title}
                  </option>
                ))}
              </select>
            </label>
            <SessionDetail session={selectedSession} />
          </section>

          <section className="control-panel">
            <div className="section-title">
              <h3>命令</h3>
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
              <p className="ready-state">已就绪</p>
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

function isSelectedSession(session: RemoteSession, selectedSessionRef: SelectedSessionRef | null): boolean {
  return Boolean(selectedSessionRef && session.source === selectedSessionRef.source && session.id === selectedSessionRef.id);
}

function sessionSelectValue(session: RemoteSession): string {
  return `${session.source}:${session.id}`;
}

function findSessionBySelectValue(sessions: RemoteSession[], value: string): RemoteSession | null {
  if (!value) {
    return null;
  }
  return sessions.find((session) => sessionSelectValue(session) === value) ?? null;
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
    errors.push("必须选择会话");
  }
  if (!hasModelId) {
    errors.push("必须选择模型");
  }
  if (hasModelId && !hasSelectedModel) {
    errors.push("模型必须支持所选会话来源");
  }
  if (!prompt.trim()) {
    errors.push("必须填写提示词");
  }
  if (mode === "SHELL" && !confirmDangerous) {
    errors.push("必须确认危险 Shell 模式");
  }

  return errors;
}
