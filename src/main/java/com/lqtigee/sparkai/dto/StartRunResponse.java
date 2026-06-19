package com.lqtigee.sparkai.dto;

import java.time.Instant;

public record StartRunResponse(
        String runId,
        String sessionId,
        AgentSource source,
        RunStatus status,
        Instant startedAt
) {
}
