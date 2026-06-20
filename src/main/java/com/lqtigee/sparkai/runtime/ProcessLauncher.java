package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;

public class ProcessLauncher {

    public ManagedProcess start(String runId, CommandSpec spec) {
        ProcessBuilder processBuilder = new ProcessBuilder(spec.command());
        processBuilder.directory(spec.workdir().toFile());
        processBuilder.environment().putAll(spec.environment());

        try {
            Process process = processBuilder.start();
            return new ManagedProcess(runId, process, Instant.now(), spec);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.PROCESS_START_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Process start failed",
                    exception.getMessage()
            );
        }
    }
}
