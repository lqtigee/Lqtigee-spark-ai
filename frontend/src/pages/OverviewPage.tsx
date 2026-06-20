import { ErrorPanel } from "../components/ErrorPanel";
import { LoadingBlock } from "../components/LoadingBlock";
import { useConnectionState } from "../state/useConnectionState";

export function OverviewPage() {
  const connection = useConnectionState();

  return (
    <section>
      <h2>Overview</h2>
      <button
        disabled={connection.status === "checking"}
        onClick={() => void connection.checkConnection()}
        type="button"
      >
        Check connection
      </button>
      {connection.status === "checking" ? <LoadingBlock label="Checking connection" /> : null}
      {connection.status === "connected" ? <p>Connected</p> : null}
      {connection.status === "unauthorized" || connection.status === "failed" ? (
        <ErrorPanel title="Connection error" error={connection.error} />
      ) : null}
    </section>
  );
}
