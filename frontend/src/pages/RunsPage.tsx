import { useState } from "react";
import { stopRun } from "../api/remoteApi";
import { ErrorPanel } from "../components/ErrorPanel";
import { RunTimeline } from "../components/RunTimeline";
import { useRunEvents } from "../state/useRunEvents";

const TERMINAL_EVENT_TYPES = new Set(["done", "error", "stopped"]);

export function RunsPage() {
  const runId = new URLSearchParams(window.location.search).get("runId") ?? "";

  if (!runId) {
    return <p>No run selected</p>;
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
    <section>
      <h2>Runs</h2>
      {runEvents.error ? <ErrorPanel title="Run events error" error={runEvents.error} /> : null}
      {stopError ? <ErrorPanel title="Stop run error" error={stopError} /> : null}
      <button disabled={terminal || stopping} onClick={() => void handleStop()} type="button">
        Stop
      </button>
      <RunTimeline events={runEvents.events} />
    </section>
  );
}
