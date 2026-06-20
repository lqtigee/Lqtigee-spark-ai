import { useCallback, useState } from "react";
import { getSessionTranscript } from "../api/remoteApi";
import type { AgentSource, SessionMessageDto, SessionTranscriptDto, TranscriptPageInfoDto } from "../types/api";

const TRANSCRIPT_PAGE_LIMIT = 10;

interface SessionTranscriptState {
  loading: boolean;
  loadingNewest: boolean;
  loadingOlder: boolean;
  loaded: boolean;
  error: unknown;
  messages: SessionMessageDto[];
  pageInfo: TranscriptPageInfoDto | null;
  transcript: SessionTranscriptDto | null;
  loadNewestTranscript(source: AgentSource, id: string): Promise<void>;
  loadOlderMessages(): Promise<void>;
  loadTranscript(source: AgentSource, id: string): Promise<void>;
  clearTranscript(): void;
}

interface SelectedTranscriptRef {
  source: AgentSource;
  id: string;
}

export function useSessionTranscriptState(): SessionTranscriptState {
  const [loadingNewest, setLoadingNewest] = useState(false);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [transcript, setTranscript] = useState<SessionTranscriptDto | null>(null);
  const [messages, setMessages] = useState<SessionMessageDto[]>([]);
  const [pageInfo, setPageInfo] = useState<TranscriptPageInfoDto | null>(null);
  const [selectedRef, setSelectedRef] = useState<SelectedTranscriptRef | null>(null);

  const loadNewestTranscript = useCallback(async (source: AgentSource, id: string) => {
    setSelectedRef({ source, id });
    setLoadingNewest(true);
    setLoaded(false);
    setError(null);

    try {
      const response = await getSessionTranscript(source, id, { limit: TRANSCRIPT_PAGE_LIMIT });
      setTranscript(response);
      setMessages(response.messages);
      setPageInfo(response.pageInfo);
      setLoaded(true);
    } catch (caughtError) {
      setTranscript(null);
      setMessages([]);
      setPageInfo(null);
      setError(caughtError);
    } finally {
      setLoadingNewest(false);
    }
  }, []);

  const loadOlderMessages = useCallback(async () => {
    if (loadingOlder || !selectedRef || !pageInfo?.hasMoreBefore || !pageInfo.oldestCursor) {
      return;
    }

    setLoadingOlder(true);
    setError(null);

    try {
      const response = await getSessionTranscript(selectedRef.source, selectedRef.id, {
        limit: TRANSCRIPT_PAGE_LIMIT,
        before: pageInfo.oldestCursor
      });
      setTranscript((currentTranscript) => mergeTranscriptPage(currentTranscript, response));
      setMessages((currentMessages) => [...response.messages, ...currentMessages]);
      setPageInfo(response.pageInfo);
      setLoaded(true);
    } catch (caughtError) {
      setError(caughtError);
    } finally {
      setLoadingOlder(false);
    }
  }, [loadingOlder, pageInfo, selectedRef]);

  const loadTranscript = useCallback(
    async (source: AgentSource, id: string) => {
      await loadNewestTranscript(source, id);
    },
    [loadNewestTranscript]
  );

  const clearTranscript = useCallback(() => {
    setLoadingNewest(false);
    setLoadingOlder(false);
    setLoaded(false);
    setError(null);
    setTranscript(null);
    setMessages([]);
    setPageInfo(null);
    setSelectedRef(null);
  }, []);

  const loading = loadingNewest || loadingOlder;

  return {
    loading,
    loadingNewest,
    loadingOlder,
    loaded,
    error,
    messages,
    pageInfo,
    transcript,
    loadNewestTranscript,
    loadOlderMessages,
    loadTranscript,
    clearTranscript
  };
}

function mergeTranscriptPage(currentTranscript: SessionTranscriptDto | null, olderPage: SessionTranscriptDto): SessionTranscriptDto {
  if (!currentTranscript) {
    return olderPage;
  }
  return {
    ...currentTranscript,
    messages: [...olderPage.messages, ...currentTranscript.messages],
    pageInfo: olderPage.pageInfo
  };
}
