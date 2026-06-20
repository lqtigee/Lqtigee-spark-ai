import type { RemoteSession } from "../types/api";

interface SessionDetailProps {
  session?: RemoteSession;
}

export function SessionDetail({ session }: SessionDetailProps) {
  if (!session) {
    return <p>No session selected</p>;
  }

  return (
    <section>
      <h3>Session detail</h3>
      <dl>
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
