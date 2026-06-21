import { CodexOptionsSheet } from "./CodexOptionsSheet";
import { OpencodeOptionsSheet } from "./OpencodeOptionsSheet";
import type { MouseEvent } from "react";
import type { AgentSource, SourceCapabilityDto } from "../types/api";

interface ChatOptionsDrawerProps {
  source: AgentSource;
  capability: SourceCapabilityDto | null;
  disabled?: boolean;
}

export function ChatOptionsDrawer({ source, capability, disabled = false }: ChatOptionsDrawerProps) {
  const hasCapability = Boolean(capability);
  const drawerDisabled = disabled || !hasCapability;

  function handleSummaryClick(event: MouseEvent<HTMLElement>) {
    if (drawerDisabled) {
      event.preventDefault();
    }
  }

  return (
    <details className="chat-options-drawer">
      <summary aria-disabled={drawerDisabled} onClick={handleSummaryClick}>
        <span>能力</span>
        <strong>{source}</strong>
      </summary>
      {capability ? (
        source === "CODEX" ? (
          <CodexOptionsSheet capability={capability} />
        ) : (
          <OpencodeOptionsSheet capability={capability} />
        )
      ) : (
        <p className="chat-options-drawer__empty">能力数据未加载</p>
      )}
    </details>
  );
}
