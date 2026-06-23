package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.persistence.RunRecordRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
        executor.execute(() -> pumpProcess(runId, process));
    }

    private void pumpProcess(String runId, ManagedProcess managedProcess) {
        AtomicReference<IOException> streamFailure = new AtomicReference<>();
        AtomicReference<String> latestStdout = new AtomicReference<>();
        AtomicReference<String> latestStderr = new AtomicReference<>();
        Thread stdoutReader = startStreamReader(runId, managedProcess.process().getInputStream(), "stdout", streamFailure, latestStdout);
        Thread stderrReader = startStreamReader(runId, managedProcess.process().getErrorStream(), "stderr", streamFailure, latestStderr);
        try {
            int exitCode = managedProcess.process().waitFor();
            joinReader(stdoutReader);
            joinReader(stderrReader);
            if (isAlreadyTerminal(runId)) {
                return;
            }
            if (streamFailure.get() != null) {
                publishFailed(runId, "Process stream read failed", "Process stream read failed", null, latestStdout.get(), latestStderr.get());
                return;
            }
            if (exitCode == 0) {
                publishExited(runId, exitCode);
            } else {
                publishFailed(runId, "Process exited with code " + exitCode, "Process exited with non-zero status", exitCode, latestStdout.get(), latestStderr.get());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (isAlreadyTerminal(runId)) {
                return;
            }
            publishFailed(runId, "Process output pump interrupted", "Process output pump interrupted", null, latestStdout.get(), latestStderr.get());
        }
    }

    private Thread startStreamReader(
            String runId,
            InputStream inputStream,
            String type,
            AtomicReference<IOException> streamFailure,
            AtomicReference<String> latestLine
    ) {
        Thread thread = new Thread(
                () -> publishStreamLines(runId, inputStream, type, streamFailure, latestLine),
                "lqtigee-run-" + type + "-" + runId
        );
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void publishStreamLines(
            String runId,
            InputStream inputStream,
            String type,
            AtomicReference<IOException> streamFailure,
            AtomicReference<String> latestLine
    ) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isAlreadyTerminal(runId)) {
                    return;
                }
                latestLine.set(line);
                eventBus.publish(runId, new RunEventDto(runId, type, line, Instant.now(), Map.of()));
            }
        } catch (IOException exception) {
            streamFailure.compareAndSet(null, exception);
        }
    }

    private void joinReader(Thread reader) throws InterruptedException {
        reader.join();
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

    private void publishFailed(
            String runId,
            String stateMessage,
            String eventMessage,
            Integer exitCode,
            String latestStdout,
            String latestStderr
    ) {
        ApiException persistenceFailure = null;
        try {
            if (runRecordRepository != null) {
                runRecordRepository.markFailed(runId, exitCode, failureSummary(eventMessage, latestStdout, latestStderr));
            }
        } catch (ApiException exception) {
            persistenceFailure = exception;
        }
        markFailed(runId, persistenceFailure == null ? stateMessage : persistenceFailure.getMessage());
        eventBus.publish(runId, terminalEvent(runId, "error", eventMessage, exitCode, latestStdout, latestStderr));
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
        return terminalEvent(runId, type, message, exitCode, null, null);
    }

    private RunEventDto terminalEvent(
            String runId,
            String type,
            String message,
            Integer exitCode,
            String latestStdout,
            String latestStderr
    ) {
        Map<String, Object> data = new HashMap<>();
        if (exitCode != null) {
            data.put("exitCode", exitCode);
        }
        if (latestStdout != null && !latestStdout.isBlank()) {
            data.put("latestStdout", latestStdout);
        }
        if (latestStderr != null && !latestStderr.isBlank()) {
            data.put("latestStderr", latestStderr);
        }
        return new RunEventDto(runId, type, message, Instant.now(), data);
    }

    private String failureSummary(String eventMessage, String latestStdout, String latestStderr) {
        StringBuilder summary = new StringBuilder(eventMessage);
        if (latestStderr != null && !latestStderr.isBlank()) {
            summary.append("; stderr: ").append(latestStderr);
        }
        if (latestStdout != null && !latestStdout.isBlank()) {
            summary.append("; stdout: ").append(latestStdout);
        }
        return summary.toString();
    }
}
