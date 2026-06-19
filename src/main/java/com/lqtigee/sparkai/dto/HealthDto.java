package com.lqtigee.sparkai.dto;

import java.time.Instant;

public record HealthDto(
        String serviceName,
        String appName,
        int port,
        String status,
        Instant timestamp
) {
}
