import { useEffect, useState } from "react";
import { openRunEvents } from "../api/remoteApi";
import type { RunEventDto } from "../types/api";

interface RunEventsState {
  events: RunEventDto[];
  error: unknown;
}

export function useRunEvents(runId: string): RunEventsState {
  const [events, setEvents] = useState<RunEventDto[]>([]);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    setEvents([]);
    setError(null);

    if (!runId) {
      return undefined;
    }

    const stream = openRunEvents(runId, {
      onEvent(event) {
        setEvents((currentEvents) => [...currentEvents, event]);
      },
      onError(caughtError) {
        setError(caughtError);
      }
    });

    return () => {
      stream.close();
    };
  }, [runId]);

  return {
    events,
    error
  };
}
