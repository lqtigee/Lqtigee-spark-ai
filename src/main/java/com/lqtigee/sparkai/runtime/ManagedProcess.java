package com.lqtigee.sparkai.runtime;

import java.time.Instant;

public record ManagedProcess(
        String runId,
        Process process,
        Instant startedAt,
        CommandSpec commandSpec
) {
}
