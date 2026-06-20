import type { RemoteSession } from "../types/api";

interface SessionDetailProps {
  session?: RemoteSession;
}

export function SessionDetail({ session }: SessionDetailProps) {
  if (!session) {
    return <p className="empty-state">No session selected</p>;
  }

  return (
    <section className="detail-panel">
      <h3>Session detail</h3>
      <dl className="definition-grid">
        <div>
          <dt>Raw file</dt>
          <dd>{session.rawFile}</dd>
        </div>
        <div>
          <dt>Updated at</dt>
          <dd>{session.updatedAt}</dd>
        </div>
        <div>
          <dt>Last message</dt>
          <dd>{session.lastMessage}</dd>
        </div>
      </dl>
    </section>
  );
}
