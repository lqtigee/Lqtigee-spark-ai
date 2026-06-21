package com.lqtigee.sparkai.dto;

import java.time.Instant;
import java.util.List;

public record HealthDto(
        String serviceName,
        String appName,
        int port,
        String status,
        Instant timestamp,
        List<AdapterHealthDto> adapters
) {
}
