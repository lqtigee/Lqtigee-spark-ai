package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.RunEventDto;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ProcessOutputPump {

    private final RunEventBus eventBus;
    private final RunRegistry runRegistry;
    private final Executor executor;

    public ProcessOutputPump(RunEventBus eventBus) {
        this(eventBus, null, Executors.newCachedThreadPool());
    }

    ProcessOutputPump(RunEventBus eventBus, Executor executor) {
        this(eventBus, null, executor);
    }

    ProcessOutputPump(RunEventBus eventBus, RunRegistry runRegistry, Executor executor) {
        this.eventBus = eventBus;
        this.runRegistry = runRegistry;
        this.executor = executor;
    }

    public void attach(String runId, ManagedProcess process) {
        executor.execute(() -> publishTerminalEvent(runId, process));
    }

    private void publishTerminalEvent(String runId, ManagedProcess managedProcess) {
        try {
            int exitCode = managedProcess.process().waitFor();
            if (exitCode == 0) {
                markExited(runId, exitCode);
                eventBus.publish(runId, terminalEvent(runId, "done", "Process exited successfully", exitCode));
            } else {
                markFailed(runId, "Process exited with code " + exitCode);
                eventBus.publish(runId, terminalEvent(runId, "error", "Process exited with non-zero status", exitCode));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            markFailed(runId, "Process output pump interrupted");
            eventBus.publish(runId, terminalEvent(runId, "error", "Process output pump interrupted", null));
        }
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
