import { ErrorPanel } from "../components/ErrorPanel";
import { RunTimeline } from "../components/RunTimeline";
import { useRunEvents } from "../state/useRunEvents";

export function RunsPage() {
  const runId = new URLSearchParams(window.location.search).get("runId") ?? "";

  if (!runId) {
    return <p>No run selected</p>;
  }

  return <RunEventsView runId={runId} />;
}

function RunEventsView({ runId }: { runId: string }) {
  const runEvents = useRunEvents(runId);

  return (
    <section>
      <h2>Runs</h2>
      {runEvents.error ? <ErrorPanel title="Run events error" error={runEvents.error} /> : null}
      <RunTimeline events={runEvents.events} />
    </section>
  );
}
