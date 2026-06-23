import { useCallback, useRef, useState } from "react";
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
  refreshNewestTranscript(source: AgentSource, id: string): Promise<void>;
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
  const selectedRefRef = useRef<SelectedTranscriptRef | null>(null);
  const requestScopeRef = useRef(0);

  const isCurrentTranscriptRequest = useCallback(
    (scope: number, ref: SelectedTranscriptRef) =>
      requestScopeRef.current === scope && isSameTranscriptRef(selectedRefRef.current, ref),
    []
  );

  const loadNewestTranscript = useCallback(async (source: AgentSource, id: string) => {
    const requestRef = { source, id };
    const sameRequestRef = isSameTranscriptRef(selectedRefRef.current, requestRef);
    requestScopeRef.current += 1;
    const requestScope = requestScopeRef.current;

    selectedRefRef.current = requestRef;
    setSelectedRef(requestRef);
    setLoadingNewest(true);
    setLoadingOlder(false);
    setError(null);
    if (!sameRequestRef) {
      setLoaded(false);
      setTranscript(null);
      setMessages([]);
      setPageInfo(null);
    }

    try {
      const response = await getSessionTranscript(source, id, { limit: TRANSCRIPT_PAGE_LIMIT });
      if (!isCurrentTranscriptRequest(requestScope, requestRef)) {
        return;
      }
      setTranscript(response);
      setMessages(response.messages);
      setPageInfo(response.pageInfo);
      setLoaded(true);
    } catch (caughtError) {
      if (!isCurrentTranscriptRequest(requestScope, requestRef)) {
        return;
      }
      setTranscript(null);
      setMessages([]);
      setPageInfo(null);
      setError(caughtError);
    } finally {
      if (isCurrentTranscriptRequest(requestScope, requestRef)) {
        setLoadingNewest(false);
      }
    }
  }, [isCurrentTranscriptRequest]);

  const loadOlderMessages = useCallback(async () => {
    if (loadingOlder || !selectedRef || !pageInfo?.hasMoreBefore || !pageInfo.oldestCursor) {
      return;
    }

    const requestRef = selectedRef;
    const requestScope = requestScopeRef.current;
    const beforeCursor = pageInfo.oldestCursor;
    setLoadingOlder(true);
    setError(null);

    try {
      const response = await getSessionTranscript(requestRef.source, requestRef.id, {
        limit: TRANSCRIPT_PAGE_LIMIT,
        before: beforeCursor
      });
      if (!isCurrentTranscriptRequest(requestScope, requestRef)) {
        return;
      }
      setTranscript((currentTranscript) => mergeTranscriptPage(currentTranscript, response));
      setMessages((currentMessages) => [...response.messages, ...currentMessages]);
      setPageInfo(response.pageInfo);
      setLoaded(true);
    } catch (caughtError) {
      if (!isCurrentTranscriptRequest(requestScope, requestRef)) {
        return;
      }
      setError(caughtError);
    } finally {
      if (isCurrentTranscriptRequest(requestScope, requestRef)) {
        setLoadingOlder(false);
      }
    }
  }, [isCurrentTranscriptRequest, loadingOlder, pageInfo, selectedRef]);

  const refreshNewestTranscript = useCallback(async (source: AgentSource, id: string) => {
    const requestRef = { source, id };
    const requestScope = requestScopeRef.current;
    if (!isCurrentTranscriptRequest(requestScope, requestRef)) {
      return;
    }

    try {
      const response = await getSessionTranscript(source, id, { limit: TRANSCRIPT_PAGE_LIMIT });
      if (!isCurrentTranscriptRequest(requestScope, requestRef)) {
        return;
      }
      setTranscript(response);
      setMessages(response.messages);
      setPageInfo(response.pageInfo);
      setLoaded(true);
      setError(null);
    } catch (caughtError) {
      if (isCurrentTranscriptRequest(requestScope, requestRef)) {
        setError(caughtError);
      }
    }
  }, [isCurrentTranscriptRequest]);

  const loadTranscript = useCallback(
    async (source: AgentSource, id: string) => {
      await loadNewestTranscript(source, id);
    },
    [loadNewestTranscript]
  );

  const clearTranscript = useCallback(() => {
    requestScopeRef.current += 1;
    selectedRefRef.current = null;
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
    refreshNewestTranscript,
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

function isSameTranscriptRef(left: SelectedTranscriptRef | null, right: SelectedTranscriptRef | null): boolean {
  return Boolean(left && right && left.source === right.source && left.id === right.id);
}
