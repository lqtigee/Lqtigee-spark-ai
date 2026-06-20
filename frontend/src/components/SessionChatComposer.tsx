import { useChatDraftState } from "../state/useChatDraftState";
import type { AgentSource } from "../types/api";

interface SessionChatComposerProps {
  source: AgentSource;
  sessionId: string;
  disabled?: boolean;
}

export function SessionChatComposer({ source, sessionId, disabled = false }: SessionChatComposerProps) {
  const { draft, setDraft } = useChatDraftState(source, sessionId);

  return (
    <form className="chat-composer" aria-label="bottom composer">
      <div className="chat-composer__toolbar" aria-label="Composer tools">
        <button className="button button--secondary chat-composer__tool" disabled={disabled} type="button">
          Options
        </button>
        <button className="button button--danger chat-composer__tool" disabled type="button">
          Stop
        </button>
      </div>
      <label className="chat-composer__prompt">
        <span>Message</span>
        <textarea
          className="input-control chat-composer__textarea"
          disabled={disabled}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="Continue this session"
          rows={2}
          value={draft}
        />
      </label>
      <button className="button button--primary chat-composer__send" disabled type="button">
        Send
      </button>
    </form>
  );
}
