package com.lqtigee.sparkai.dto;

import java.time.Instant;

public record SessionActionResponse(
        String actionId,
        AgentSource source,
        String sessionId,
        String action,
        String status,
        Instant startedAt
) {
}
