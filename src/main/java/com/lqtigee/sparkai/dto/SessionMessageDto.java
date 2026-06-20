package com.lqtigee.sparkai.dto;

import java.time.Instant;

public record SessionMessageDto(
        String id,
        String role,
        String text,
        Instant timestamp
) {
}
