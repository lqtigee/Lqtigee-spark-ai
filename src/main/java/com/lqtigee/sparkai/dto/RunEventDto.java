package com.lqtigee.sparkai.dto;

import java.time.Instant;
import java.util.Map;

public record RunEventDto(
        String runId,
        String type,
        String message,
        Instant timestamp,
        Map<String, Object> data
) {
}
