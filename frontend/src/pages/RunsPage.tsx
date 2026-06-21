import { useRef, useState } from "react";
import { stopRun } from "../api/remoteApi";
import { ErrorPanel } from "../components/ErrorPanel";
import { RunTimeline } from "../components/RunTimeline";
import { useRunEvents } from "../state/useRunEvents";

const TERMINAL_EVENT_TYPES = new Set(["done", "error", "stopped"]);

export function RunsPage() {
  const runId = new URLSearchParams(window.location.search).get("runId") ?? "";

  if (!runId) {
    return (
      <section className="page-stack">
        <div className="page-heading">
          <div>
            <p className="eyebrow">运行流</p>
            <h2>运行</h2>
          </div>
        </div>
        <p className="empty-state">未选择运行</p>
      </section>
    );
  }

  return <RunEventsView runId={runId} />;
}

function RunEventsView({ runId }: { runId: string }) {
  const runEvents = useRunEvents(runId);
  const [stopError, setStopError] = useState<unknown>(null);
  const [stopping, setStopping] = useState(false);
  const stopInFlightRef = useRef(false);
  const terminal = runEvents.events.some((event) => TERMINAL_EVENT_TYPES.has(event.type));

  async function handleStop() {
    if (terminal || stopping || stopInFlightRef.current) {
      return;
    }

    setStopError(null);
    setStopping(true);
    stopInFlightRef.current = true;
    try {
      await stopRun(runId);
    } catch (caughtError) {
      setStopError(caughtError);
    } finally {
      stopInFlightRef.current = false;
      setStopping(false);
    }
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">运行流</p>
          <h2>运行</h2>
        </div>
        <button className="button button--danger" disabled={terminal || stopping} onClick={() => void handleStop()} type="button">
          停止
        </button>
      </div>
      <section className="status-strip">
        <div>
          <span>运行编号</span>
          <strong>{runId}</strong>
        </div>
        <div>
          <span>状态</span>
          <strong>{terminal ? "已结束" : "流式输出中"}</strong>
        </div>
      </section>
      {runEvents.error ? <ErrorPanel title="运行事件加载失败" error={runEvents.error} /> : null}
      {stopError ? <ErrorPanel title="停止运行失败" error={stopError} /> : null}
      <RunTimeline events={runEvents.events} />
    </section>
  );
}
