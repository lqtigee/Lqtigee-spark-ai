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
        runs.put(runId, new RunState(request, RunStatus.CREATED, null, null, null));
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

    public void attachProcess(String runId, ManagedProcess process) {
        runs.compute(runId, (id, current) -> {
            RunState state = requireExisting(id, current);
            return new RunState(state.request(), state.status(), state.exitCode(), state.message(), process);
        });
    }

    public ManagedProcess getRequiredProcess(String runId) {
        RunState state = requireExisting(runId, runs.get(runId));
        if (isFinished(state.status())) {
            throw alreadyFinished(runId);
        }
        if (state.process() == null) {
            throw new ApiException(
                    ErrorCode.PROCESS_STOP_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Run process is not attached",
                    runId
            );
        }
        return state.process();
    }

    public RunStatus statusOf(String runId) {
        RunState state = runs.get(runId);
        if (state == null) {
            throw new ApiException(
                    ErrorCode.RUN_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Run was not found",
                    runId
            );
        }
        return state.status();
    }

    private void updateStatus(String runId, RunStatus nextStatus, Integer exitCode, String message) {
        runs.compute(runId, (id, current) -> {
            RunState state = requireExisting(id, current);
            if (isFinished(current.status())) {
                throw alreadyFinished(id);
            }
            return new RunState(state.request(), nextStatus, exitCode, message, state.process());
        });
    }

    private boolean isFinished(RunStatus status) {
        return status == RunStatus.EXITED || status == RunStatus.FAILED || status == RunStatus.STOPPED;
    }

    private RunState requireExisting(String runId, RunState state) {
        if (state == null) {
            throw new ApiException(
                    ErrorCode.RUN_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Run was not found",
                    runId
            );
        }
        return state;
    }

    private ApiException alreadyFinished(String runId) {
        return new ApiException(
                ErrorCode.RUN_ALREADY_FINISHED,
                HttpStatus.CONFLICT,
                "Run is already finished",
                runId
        );
    }

    private record RunState(
            StartRunRequest request,
            RunStatus status,
            Integer exitCode,
            String message,
            ManagedProcess process
    ) {
    }
}
