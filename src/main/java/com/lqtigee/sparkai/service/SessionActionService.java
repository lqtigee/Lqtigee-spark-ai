package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.SessionActionRequest;
import com.lqtigee.sparkai.dto.SessionActionResponse;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.runtime.CodexSessionActionCommandBuilder;
import com.lqtigee.sparkai.runtime.CommandSpec;
import com.lqtigee.sparkai.runtime.ManagedProcess;
import com.lqtigee.sparkai.runtime.OpencodeSessionActionCommandBuilder;
import com.lqtigee.sparkai.runtime.ProcessLauncher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.http.HttpStatus;

public class SessionActionService {

    private final SessionService sessionService;
    private final CapabilityService capabilityService;
    private final CodexSessionActionCommandBuilder codexCommandBuilder;
    private final OpencodeSessionActionCommandBuilder opencodeCommandBuilder;
    private final ProcessLauncher processLauncher;
    private final Executor outputDiscardExecutor;

    public SessionActionService(
            SessionService sessionService,
            CapabilityService capabilityService,
            CodexSessionActionCommandBuilder codexCommandBuilder,
            OpencodeSessionActionCommandBuilder opencodeCommandBuilder,
            ProcessLauncher processLauncher
    ) {
        this(
                sessionService,
                capabilityService,
                codexCommandBuilder,
                opencodeCommandBuilder,
                processLauncher,
                Executors.newCachedThreadPool()
        );
    }

    SessionActionService(
            SessionService sessionService,
            CapabilityService capabilityService,
            CodexSessionActionCommandBuilder codexCommandBuilder,
            OpencodeSessionActionCommandBuilder opencodeCommandBuilder,
            ProcessLauncher processLauncher,
            Executor outputDiscardExecutor
    ) {
        this.sessionService = sessionService;
        this.capabilityService = capabilityService;
        this.codexCommandBuilder = codexCommandBuilder;
        this.opencodeCommandBuilder = opencodeCommandBuilder;
        this.processLauncher = processLauncher;
        this.outputDiscardExecutor = outputDiscardExecutor;
    }

    public SessionActionResponse startAction(AgentSource source, String sessionId, SessionActionRequest request) {
        String action = validateAction(request);
        sessionService.getRequiredSession(source, sessionId);
        requireCapability(source, action);

        String actionId = "act_" + UUID.randomUUID();
        CommandSpec commandSpec = buildCommandSpec(source, sessionId, action, Boolean.TRUE.equals(request.confirmDestructive()));
        ManagedProcess process = processLauncher.start(actionId, commandSpec);
        detach(process.process());

        return new SessionActionResponse(
                actionId,
                source,
                sessionId,
                action,
                "STARTED",
                process.startedAt()
        );
    }

    private String validateAction(SessionActionRequest request) {
        if (request == null || request.action() == null || request.action().isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Session action is required",
                    "action"
            );
        }
        return request.action().trim().toLowerCase(Locale.ROOT);
    }

    private void requireCapability(AgentSource source, String action) {
        List<String> enabledActions = capabilityService.listCapabilities().stream()
                .filter(capability -> capability.source() == source)
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        ErrorCode.VALIDATION_FAILED,
                        HttpStatus.BAD_REQUEST,
                        "Source capability is not available",
                        source.name()
                ))
                .sessionActions();
        if (!enabledActions.contains(action)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Session action is not enabled for source",
                    action
            );
        }
    }

    private CommandSpec buildCommandSpec(
            AgentSource source,
            String sessionId,
            String action,
            boolean confirmDestructive
    ) {
        return switch (source) {
            case CODEX -> buildCodexCommand(sessionId, action, confirmDestructive);
            case OPENCODE -> buildOpencodeCommand(sessionId, action, confirmDestructive);
        };
    }

    private CommandSpec buildCodexCommand(String sessionId, String action, boolean confirmDestructive) {
        return switch (action) {
            case "archive" -> codexCommandBuilder.archive(sessionId);
            case "delete" -> codexCommandBuilder.delete(sessionId, confirmDestructive);
            case "unarchive" -> codexCommandBuilder.unarchive(sessionId);
            case "fork" -> codexCommandBuilder.fork(sessionId);
            default -> throw unsupportedAction(action);
        };
    }

    private CommandSpec buildOpencodeCommand(String sessionId, String action, boolean confirmDestructive) {
        return switch (action) {
            case "delete" -> opencodeCommandBuilder.delete(sessionId, confirmDestructive);
            case "export" -> opencodeCommandBuilder.export(sessionId);
            default -> throw unsupportedAction(action);
        };
    }

    private ApiException unsupportedAction(String action) {
        return new ApiException(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "Session action is unsupported",
                action
        );
    }

    private void detach(Process process) {
        closeInput(process.getOutputStream());
        outputDiscardExecutor.execute(() -> drain(process.getInputStream()));
        outputDiscardExecutor.execute(() -> drain(process.getErrorStream()));
        outputDiscardExecutor.execute(() -> awaitExit(process));
    }

    private void closeInput(OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException ignored) {
        }
    }

    private void drain(InputStream inputStream) {
        try (inputStream) {
            inputStream.transferTo(OutputStream.nullOutputStream());
        } catch (IOException ignored) {
        }
    }

    private void awaitExit(Process process) {
        try {
            process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
