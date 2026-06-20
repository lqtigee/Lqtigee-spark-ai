package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.RunStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;

public class RunRegistry {

    private final Map<String, RunState> runs = new ConcurrentHashMap<>();

    public String create(StartRunRequest request) {
        String runId = UUID.randomUUID().toString();
        runs.put(runId, new RunState(request, RunStatus.CREATED, null, null));
        return runId;
    }

    public void markRunning(String runId) {
        updateStatus(runId, RunStatus.RUNNING, null, null);
    }

    public void markExited(String runId, int exitCode) {
        updateStatus(runId, RunStatus.EXITED, exitCode, null);
    }

    public void markFailed(String runId, String message) {
        updateStatus(runId, RunStatus.FAILED, null, message);
    }

    public void markStopped(String runId) {
        updateStatus(runId, RunStatus.STOPPED, null, null);
    }

    private void updateStatus(String runId, RunStatus nextStatus, Integer exitCode, String message) {
        runs.compute(runId, (id, current) -> {
            if (current == null) {
                throw new ApiException(
                        ErrorCode.RUN_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Run was not found",
                        id
                );
            }
            if (isFinished(current.status())) {
                throw new ApiException(
                        ErrorCode.RUN_ALREADY_FINISHED,
                        HttpStatus.CONFLICT,
                        "Run is already finished",
                        id
                );
            }
            return new RunState(current.request(), nextStatus, exitCode, message);
        });
    }

    private boolean isFinished(RunStatus status) {
        return status == RunStatus.EXITED || status == RunStatus.FAILED || status == RunStatus.STOPPED;
    }

    private record RunState(
            StartRunRequest request,
            RunStatus status,
            Integer exitCode,
            String message
    ) {
    }
}
