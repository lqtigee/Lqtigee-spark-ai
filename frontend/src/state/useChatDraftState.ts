import { useCallback, useEffect, useMemo, useState } from "react";
import type { AgentSource } from "../types/api";

const CHAT_DRAFT_STORAGE_PREFIX = "lqtigee_chat_draft:";

interface ChatDraftState {
  draft: string;
  setDraft(draft: string): void;
  clearDraft(): void;
}

export function useChatDraftState(source: AgentSource, id: string): ChatDraftState {
  const storageKey = useMemo(() => buildDraftKey(source, id), [source, id]);
  const [draft, setDraftValue] = useState("");

  useEffect(() => {
    setDraftValue(localStorage.getItem(storageKey) ?? "");
  }, [storageKey]);

  const setDraft = useCallback(
    (nextDraft: string) => {
      setDraftValue(nextDraft);
      localStorage.setItem(storageKey, nextDraft);
    },
    [storageKey]
  );

  const clearDraft = useCallback(() => {
    setDraftValue("");
    localStorage.removeItem(storageKey);
  }, [storageKey]);

  return {
    draft,
    setDraft,
    clearDraft
  };
}

function buildDraftKey(source: AgentSource, id: string): string {
  return `${CHAT_DRAFT_STORAGE_PREFIX}${source}:${id}`;
}
