import { useEffect, useMemo, useState } from "react";
import { getHealth, listModels, listSessions } from "../api/remoteApi";
import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { StatusBadge } from "../components/StatusBadge";
import type { AgentSource, ModelDto, RemoteSession } from "../types/api";

const TOKEN_KEY = "lqtigee_token";

type HealthDto = Awaited<ReturnType<typeof getHealth>>;

type LoadStatus = "idle" | "loading" | "loaded" | "failed";

export function OverviewPage() {
  const hasToken = Boolean((localStorage.getItem(TOKEN_KEY) ?? "").trim());
  const [healthStatus, setHealthStatus] = useState<LoadStatus>("idle");
  const [health, setHealth] = useState<HealthDto | null>(null);
  const [healthError, setHealthError] = useState<unknown>(null);
  const [protectedStatus, setProtectedStatus] = useState<LoadStatus>("idle");
  const [protectedError, setProtectedError] = useState<unknown>(null);
  const [sessions, setSessions] = useState<RemoteSession[]>([]);
  const [models, setModels] = useState<ModelDto[]>([]);
  const sourceCounts = useMemo(() => countSessionsBySource(sessions), [sessions]);
  const enabledModels = useMemo(() => models.filter((model) => model.enabled).length, [models]);

  useEffect(() => {
    void loadHealth();
  }, []);

  useEffect(() => {
    if (!hasToken) {
      setProtectedStatus("idle");
      setProtectedError(null);
      setSessions([]);
      setModels([]);
      return;
    }

    void loadProtectedData();
  }, [hasToken]);

  async function loadHealth() {
    setHealthStatus("loading");
    setHealthError(null);
    try {
      setHealth(await getHealth());
      setHealthStatus("loaded");
    } catch (caughtError) {
      setHealthError(caughtError);
      setHealthStatus("failed");
    }
  }

  async function loadProtectedData() {
    setProtectedStatus("loading");
    setProtectedError(null);
    try {
      const [sessionsResponse, modelsResponse] = await Promise.all([listSessions(), listModels()]);
      setSessions(sessionsResponse.sessions);
      setModels(modelsResponse.models);
      setProtectedStatus("loaded");
    } catch (caughtError) {
      setProtectedError(caughtError);
      setProtectedStatus("failed");
    }
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Port 20261</p>
          <h2>Live console</h2>
        </div>
        <button className="button button--secondary" disabled={healthStatus === "loading"} onClick={() => void loadHealth()} type="button">
          Refresh
        </button>
      </div>

      {healthStatus === "loading" ? <LoadingBlock label="Checking service" /> : null}
      {healthError ? <ErrorPanel title="Service connection error" error={healthError} /> : null}

      {health ? (
        <section className="status-strip">
          <div>
            <span>Service</span>
            <strong>{health.serviceName}</strong>
          </div>
          <div>
            <span>Port</span>
            <strong>{health.port}</strong>
          </div>
          <div>
            <span>Status</span>
            <StatusBadge status={health.status} label={health.status} />
          </div>
        </section>
      ) : null}

      {!hasToken ? (
        <section className="action-panel action-panel--warning">
          <div>
            <h3>Token required</h3>
            <p>Protected live session and model data require the API token.</p>
          </div>
          <a className="button button--primary" href="/settings">
            Settings
          </a>
        </section>
      ) : null}

      {hasToken ? (
        <section className="page-stack">
          <div className="section-title">
            <h3>Current sessions</h3>
            <button className="button button--secondary" disabled={protectedStatus === "loading"} onClick={() => void loadProtectedData()} type="button">
              Reload
            </button>
          </div>
          {protectedStatus === "loading" ? <LoadingBlock label="Loading sessions and models" /> : null}
          {protectedError ? <ErrorPanel title="Live data error" error={protectedError} /> : null}
          {protectedStatus === "loaded" && !protectedError ? (
            <>
              <div className="metric-grid">
                <div className="metric-tile">
                  <span>Total sessions</span>
                  <strong>{sessions.length}</strong>
                </div>
                <div className="metric-tile">
                  <span>Codex</span>
                  <strong>{sourceCounts.CODEX}</strong>
                </div>
                <div className="metric-tile">
                  <span>opencode</span>
                  <strong>{sourceCounts.OPENCODE}</strong>
                </div>
                <div className="metric-tile">
                  <span>Enabled models</span>
                  <strong>{enabledModels}</strong>
                </div>
              </div>
              <div className="action-grid">
                <a className="button button--primary" href="/sessions">
                  View sessions
                </a>
                <a className="button button--secondary" href="/control">
                  Remote control
                </a>
              </div>
            </>
          ) : null}
        </section>
      ) : null}
    </section>
  );
}

function countSessionsBySource(sessions: RemoteSession[]): Record<AgentSource, number> {
  return sessions.reduce<Record<AgentSource, number>>(
    (counts, session) => {
      counts[session.source] += 1;
      return counts;
    },
    { CODEX: 0, OPENCODE: 0 }
  );
}
