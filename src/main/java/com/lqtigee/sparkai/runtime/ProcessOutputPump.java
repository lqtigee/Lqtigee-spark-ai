package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.persistence.RunRecordRepository;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ProcessOutputPump {

    private final RunEventBus eventBus;
    private final RunRegistry runRegistry;
    private final RunRecordRepository runRecordRepository;
    private final Executor executor;

    public ProcessOutputPump(RunEventBus eventBus) {
        this(eventBus, null, null, Executors.newCachedThreadPool());
    }

    public ProcessOutputPump(RunEventBus eventBus, RunRegistry runRegistry) {
        this(eventBus, runRegistry, null, Executors.newCachedThreadPool());
    }

    public ProcessOutputPump(RunEventBus eventBus, RunRegistry runRegistry, RunRecordRepository runRecordRepository) {
        this(eventBus, runRegistry, runRecordRepository, Executors.newCachedThreadPool());
    }

    ProcessOutputPump(RunEventBus eventBus, Executor executor) {
        this(eventBus, null, null, executor);
    }

    ProcessOutputPump(RunEventBus eventBus, RunRegistry runRegistry, Executor executor) {
        this(eventBus, runRegistry, null, executor);
    }

    ProcessOutputPump(
            RunEventBus eventBus,
            RunRegistry runRegistry,
            RunRecordRepository runRecordRepository,
            Executor executor
    ) {
        this.eventBus = eventBus;
        this.runRegistry = runRegistry;
        this.runRecordRepository = runRecordRepository;
        this.executor = executor;
    }

    public void attach(String runId, ManagedProcess process) {
        executor.execute(() -> publishTerminalEvent(runId, process));
    }

    private void publishTerminalEvent(String runId, ManagedProcess managedProcess) {
        try {
            int exitCode = managedProcess.process().waitFor();
            if (isAlreadyTerminal(runId)) {
                return;
            }
            if (exitCode == 0) {
                publishExited(runId, exitCode);
            } else {
                publishFailed(runId, "Process exited with code " + exitCode, "Process exited with non-zero status", exitCode);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (isAlreadyTerminal(runId)) {
                return;
            }
            publishFailed(runId, "Process output pump interrupted", "Process output pump interrupted", null);
        }
    }

    private boolean isAlreadyTerminal(String runId) {
        return runRegistry != null && runRegistry.isTerminal(runId);
    }

    private void publishExited(String runId, int exitCode) {
        try {
            if (runRecordRepository != null) {
                runRecordRepository.markExited(runId);
            }
            markExited(runId, exitCode);
            eventBus.publish(runId, terminalEvent(runId, "done", "Process exited successfully", exitCode));
        } catch (ApiException exception) {
            markFailed(runId, exception.getMessage());
            eventBus.publish(runId, terminalEvent(runId, "error", "Process terminal persistence failed", exitCode));
        }
    }

    private void publishFailed(String runId, String stateMessage, String eventMessage, Integer exitCode) {
        ApiException persistenceFailure = null;
        try {
            if (runRecordRepository != null) {
                runRecordRepository.markFailed(runId);
            }
        } catch (ApiException exception) {
            persistenceFailure = exception;
        }
        markFailed(runId, persistenceFailure == null ? stateMessage : persistenceFailure.getMessage());
        eventBus.publish(runId, terminalEvent(runId, "error", eventMessage, exitCode));
    }

    private void markExited(String runId, int exitCode) {
        if (runRegistry != null) {
            runRegistry.markExited(runId, exitCode);
        }
    }

    private void markFailed(String runId, String message) {
        if (runRegistry != null) {
            runRegistry.markFailed(runId, message);
        }
    }

    private RunEventDto terminalEvent(String runId, String type, String message, Integer exitCode) {
        Map<String, Object> data = exitCode == null ? Map.of() : Map.of("exitCode", exitCode);
        return new RunEventDto(runId, type, message, Instant.now(), data);
    }
}
