package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.dto.StopRunResponse;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.runtime.CodexCommandBuilder;
import com.lqtigee.sparkai.runtime.CommandSpec;
import com.lqtigee.sparkai.runtime.OpencodeCommandBuilder;
import com.lqtigee.sparkai.runtime.ProcessLauncher;
import com.lqtigee.sparkai.runtime.ProcessOutputPump;
import com.lqtigee.sparkai.runtime.RunRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class RunService {

    private final SessionService sessionService;
    private final ModelService modelService;
    private final CodexCommandBuilder codexCommandBuilder;
    private final OpencodeCommandBuilder opencodeCommandBuilder;
    private final ProcessLauncher processLauncher;
    private final ProcessOutputPump processOutputPump;
    private final RunRegistry runRegistry;
    private final RemoteProperties remoteProperties;

    public RunService(
            SessionService sessionService,
            ModelService modelService,
            CodexCommandBuilder codexCommandBuilder,
            OpencodeCommandBuilder opencodeCommandBuilder,
            ProcessLauncher processLauncher,
            ProcessOutputPump processOutputPump,
            RunRegistry runRegistry,
            RemoteProperties remoteProperties
    ) {
        this.sessionService = sessionService;
        this.modelService = modelService;
        this.codexCommandBuilder = codexCommandBuilder;
        this.opencodeCommandBuilder = opencodeCommandBuilder;
        this.processLauncher = processLauncher;
        this.processOutputPump = processOutputPump;
        this.runRegistry = runRegistry;
        this.remoteProperties = remoteProperties;
        this.remoteProperties.validate();
    }

    public StartRunResponse start(StartRunRequest request) {
        validateRequest(request);
        buildCommandSpec(request);
        throw new UnsupportedOperationException("Run start is not implemented yet");
    }

    public SseEmitter events(String runId) {
        throw new UnsupportedOperationException("Run events are not implemented yet");
    }

    public StopRunResponse stop(String runId) {
        throw new UnsupportedOperationException("Run stop is not implemented yet");
    }

    private void validateRequest(StartRunRequest request) {
        if (request == null) {
            throw validationFailed("request");
        }
        if (request.source() == null) {
            throw validationFailed("source");
        }
        if (isBlank(request.sessionId())) {
            throw validationFailed("sessionId");
        }
        if (isBlank(request.modelId())) {
            throw validationFailed("modelId");
        }
        if (request.mode() == null) {
            throw validationFailed("mode");
        }
        if (isBlank(request.prompt())) {
            throw new ApiException(
                    ErrorCode.PROMPT_EMPTY,
                    HttpStatus.BAD_REQUEST,
                    "Prompt is required",
                    "prompt"
            );
        }
        if (request.prompt().length() > remoteProperties.getMaxPromptChars()) {
            throw new ApiException(
                    ErrorCode.PROMPT_TOO_LONG,
                    HttpStatus.BAD_REQUEST,
                    "Prompt is too long",
                    "prompt"
            );
        }
    }

    private CommandSpec buildCommandSpec(StartRunRequest request) {
        RemoteSessionDto session = sessionService.getRequiredSession(request.source(), request.sessionId());
        ModelDto model = modelService.getRequiredModel(request.modelId());
        modelService.validateModelForSource(request.modelId(), request.source());
        return switch (request.source()) {
            case CODEX -> codexCommandBuilder.build(request, session, model);
            case OPENCODE -> opencodeCommandBuilder.build(request, session, model);
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException validationFailed(String detail) {
        return new ApiException(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                detail
        );
    }
}
