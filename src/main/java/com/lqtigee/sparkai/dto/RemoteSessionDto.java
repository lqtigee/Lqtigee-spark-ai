package com.lqtigee.sparkai.dto;

import java.time.Instant;

public record RemoteSessionDto(
        String id,
        AgentSource source,
        String title,
        String workspace,
        String model,
        SessionStatus status,
        Instant updatedAt,
        String lastMessage,
        String rawFile
) {
}
