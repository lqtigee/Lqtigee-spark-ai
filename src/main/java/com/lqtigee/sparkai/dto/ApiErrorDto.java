package com.lqtigee.sparkai.dto;

import com.lqtigee.sparkai.error.ErrorCode;
import java.time.Instant;

public record ApiErrorDto(
        ErrorCode code,
        String message,
        String detail,
        Instant timestamp,
        String path
) {
}
