import { useState } from "react";
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
            <p className="eyebrow">Run stream</p>
            <h2>Runs</h2>
          </div>
        </div>
        <p className="empty-state">No run selected</p>
      </section>
    );
  }

  return <RunEventsView runId={runId} />;
}

function RunEventsView({ runId }: { runId: string }) {
  const runEvents = useRunEvents(runId);
  const [stopError, setStopError] = useState<unknown>(null);
  const [stopping, setStopping] = useState(false);
  const terminal = runEvents.events.some((event) => TERMINAL_EVENT_TYPES.has(event.type));

  async function handleStop() {
    setStopError(null);
    setStopping(true);
    try {
      await stopRun(runId);
    } catch (caughtError) {
      setStopError(caughtError);
    } finally {
      setStopping(false);
    }
  }

  return (
    <section className="page-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">Run stream</p>
          <h2>Runs</h2>
        </div>
        <button className="button button--danger" disabled={terminal || stopping} onClick={() => void handleStop()} type="button">
          Stop
        </button>
      </div>
      <section className="status-strip">
        <div>
          <span>Run ID</span>
          <strong>{runId}</strong>
        </div>
        <div>
          <span>Status</span>
          <strong>{terminal ? "terminal" : "streaming"}</strong>
        </div>
      </section>
      {runEvents.error ? <ErrorPanel title="Run events error" error={runEvents.error} /> : null}
      {stopError ? <ErrorPanel title="Stop run error" error={stopError} /> : null}
      <RunTimeline events={runEvents.events} />
    </section>
  );
}
