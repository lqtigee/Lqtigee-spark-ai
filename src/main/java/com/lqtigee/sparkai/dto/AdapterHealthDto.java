package com.lqtigee.sparkai.dto;

public record AdapterHealthDto(
        AgentSource source,
        boolean available,
        String status,
        String version,
        String lastErrorCode,
        String lastErrorMessage
) {
}
