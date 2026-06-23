package com.lqtigee.sparkai.dto;

import java.time.Instant;

public record RunRecordDto(
        String runId,
        String sessionId,
        AgentSource source,
        String modelId,
        CommandMode mode,
        RunStatus status,
        Integer exitCode,
        String message,
        Instant createdAt,
        boolean processAttached
) {
}
