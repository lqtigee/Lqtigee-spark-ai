package com.lqtigee.sparkai.dto;

import java.time.Instant;

public record AttachmentDto(
        String id,
        String filename,
        String contentType,
        long sizeBytes,
        Instant createdAt
) {
}
