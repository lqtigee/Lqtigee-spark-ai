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
      setSessions([]);
      setModels([]);
      setProtectedError(caughtError);
      setProtectedStatus("failed");
    }
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">端口 20261</p>
          <h2>实时控制台</h2>
        </div>
        <button className="button button--secondary" disabled={healthStatus === "loading"} onClick={() => void loadHealth()} type="button">
          刷新
        </button>
      </div>

      {healthStatus === "loading" ? <LoadingBlock label="正在检查服务" /> : null}
      {healthError ? <ErrorPanel title="服务连接失败" error={healthError} /> : null}

      {health ? (
        <section className="status-strip">
          <div>
            <span>服务</span>
            <strong>{health.serviceName}</strong>
          </div>
          <div>
            <span>端口</span>
            <strong>{health.port}</strong>
          </div>
          <div>
            <span>连接</span>
            <StatusBadge status="ACTIVE" label="已连接" />
          </div>
          <div>
            <span>服务状态</span>
            <StatusBadge status={health.status} label={health.status} />
          </div>
        </section>
      ) : null}

      {health?.adapters?.length ? (
        <section className="adapter-health" aria-label="适配器健康状态">
          <div className="section-title">
            <h3>适配器</h3>
          </div>
          <div className="adapter-health__grid">
            {health.adapters.map((adapter) => (
              <article className="adapter-health__item" key={adapter.source}>
                <div>
                  <span>{adapter.source}</span>
                  <StatusBadge status={adapter.available ? adapter.status : "UNAVAILABLE"} label={adapter.available ? adapter.status : "UNAVAILABLE"} />
                </div>
                <p>{adapter.available ? adapter.version ?? "版本未知" : adapter.lastErrorCode ?? adapter.status}</p>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {!hasToken ? (
        <section className="action-panel action-panel--warning">
          <div>
            <h3>需要令牌</h3>
            <p>实时会话和模型数据需要访问令牌。</p>
          </div>
          <a className="button button--primary" href="/settings">
            设置
          </a>
        </section>
      ) : null}

      {hasToken ? (
        <section className="page-stack">
          <div className="section-title">
            <h3>当前会话</h3>
            <button className="button button--secondary" disabled={protectedStatus === "loading"} onClick={() => void loadProtectedData()} type="button">
              刷新
            </button>
          </div>
          {protectedStatus === "loading" ? <LoadingBlock label="正在加载会话和模型" /> : null}
          {protectedError ? <ErrorPanel title="实时数据加载失败" error={protectedError} /> : null}
          {protectedStatus === "loaded" && !protectedError ? (
            <>
              <div className="metric-grid">
                <div className="metric-tile">
                  <span>会话总数</span>
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
                  <span>已启用模型</span>
                  <strong>{enabledModels}</strong>
                </div>
              </div>
              <div className="action-grid">
                <a className="button button--primary" href="/sessions">
                  查看会话
                </a>
                <a className="button button--secondary" href="/control">
                  远程控制
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
