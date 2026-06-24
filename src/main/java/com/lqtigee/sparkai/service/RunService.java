package com.lqtigee.sparkai.service;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.dto.RunRecordDto;
import com.lqtigee.sparkai.dto.RunStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.dto.StopRunResponse;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.persistence.RunRecordRepository;
import com.lqtigee.sparkai.runtime.CommandSpec;
import com.lqtigee.sparkai.runtime.ManagedProcess;
import com.lqtigee.sparkai.runtime.OpencodeCommandBuilder;
import com.lqtigee.sparkai.runtime.ProcessLauncher;
import com.lqtigee.sparkai.runtime.ProcessOutputPump;
import com.lqtigee.sparkai.runtime.RunEventBus;
import com.lqtigee.sparkai.runtime.RunRegistry;
import com.lqtigee.sparkai.runtime.VscodeCodexRunBridge;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class RunService {

    private static final long PROCESS_STOP_TIMEOUT_SECONDS = 2L;
    private static final int OPENCODE_REPLAY_LIMIT_MIN = 1;
    private static final int OPENCODE_REPLAY_LIMIT_MAX = 200;
    private static final Set<String> CODEX_SANDBOX_VALUES = Set.of("read-only", "workspace-write", "danger-full-access");
    private static final Set<String> CODEX_APPROVAL_POLICIES = Set.of("untrusted", "on-failure", "on-request", "never");
    private static final String CODEX_REASONING_EFFORT_KEY = "model_reasoning_effort";
    private static final Set<String> CODEX_REASONING_EFFORT_VALUES = Set.of("low", "medium", "high", "xhigh");

    private final SessionService sessionService;
    private final ModelService modelService;
    private final OpencodeCommandBuilder opencodeCommandBuilder;
    private final ProcessLauncher processLauncher;
    private final ProcessOutputPump processOutputPump;
    private final RunEventBus runEventBus;
    private final RunRegistry runRegistry;
    private final RemoteProperties remoteProperties;
    private final RunRecordRepository runRecordRepository;
    private final VscodeCodexRunBridge vscodeCodexRunBridge;

    public RunService(
            SessionService sessionService,
            ModelService modelService,
            OpencodeCommandBuilder opencodeCommandBuilder,
            ProcessLauncher processLauncher,
            ProcessOutputPump processOutputPump,
            RunEventBus runEventBus,
            RunRegistry runRegistry,
            RemoteProperties remoteProperties,
            RunRecordRepository runRecordRepository,
            VscodeCodexRunBridge vscodeCodexRunBridge
    ) {
        this.sessionService = sessionService;
        this.modelService = modelService;
        this.opencodeCommandBuilder = opencodeCommandBuilder;
        this.processLauncher = processLauncher;
        this.processOutputPump = processOutputPump;
        this.runEventBus = runEventBus;
        this.runRegistry = runRegistry;
        this.remoteProperties = remoteProperties;
        this.runRecordRepository = runRecordRepository;
        this.vscodeCodexRunBridge = vscodeCodexRunBridge;
        this.remoteProperties.validate();
    }

    public StartRunResponse start(StartRunRequest request) {
        validateRequest(request);
        RemoteSessionDto session = sessionService.getRequiredSession(request.source(), request.sessionId());
        ModelDto model = modelService.getRequiredModel(request.modelId());
        modelService.validateModelForSource(request.modelId(), request.source());
        String runId = runRegistry.create(request);
        runRecordRepository.saveStarted(runId, request.source().name(), request.sessionId(), request.modelId());
        if (request.source() == AgentSource.CODEX) {
            startVscodeCodexRun(runId, request, session, model);
            return new StartRunResponse(
                    runId,
                    request.sessionId(),
                    request.source(),
                    RunStatus.RUNNING,
                    Instant.now()
            );
        }

        CommandSpec spec = opencodeCommandBuilder.build(request, session, model);
        ManagedProcess process;
        try {
            process = processLauncher.start(runId, spec);
        } catch (ApiException exception) {
            markFailedAfterLaunchFailure(runId, exception);
            runRegistry.markFailed(runId, exception.getMessage());
            throw exception;
        }
        runRegistry.attachProcess(runId, process);
        try {
            runRecordRepository.markRunning(runId);
        } catch (ApiException exception) {
            stopStartedProcess(process);
            runRegistry.markFailed(runId, exception.getMessage());
            throw exception;
        }
        runRegistry.markRunning(runId);
        processOutputPump.attach(runId, process);
        return new StartRunResponse(
                runId,
                request.sessionId(),
                request.source(),
                RunStatus.RUNNING,
                Instant.now()
        );
    }

    private void startVscodeCodexRun(
            String runId,
            StartRunRequest request,
            RemoteSessionDto session,
            ModelDto model
    ) {
        try {
            runRecordRepository.markRunning(runId);
            runRegistry.markRunning(runId);
            vscodeCodexRunBridge.start(runId, request, session, model);
        } catch (ApiException exception) {
            try {
                runRecordRepository.markFailed(runId, null, exception.getMessage());
            } catch (ApiException persistenceFailure) {
                exception.addSuppressed(persistenceFailure);
            }
            runRegistry.markFailed(runId, exception.getMessage());
            throw exception;
        }
    }

    public SseEmitter events(String runId) {
        runRegistry.statusOf(runId);
        try {
            return runEventBus.subscribe(runId);
        } catch (RuntimeException exception) {
            throw new ApiException(
                    ErrorCode.SSE_SUBSCRIBE_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Run event subscription failed",
                    runId
            );
        }
    }

    public StartRunResponse responseFor(String runId) {
        RunRegistry.RunSnapshot snapshot = runRegistry.snapshot(runId);
        return new StartRunResponse(
                runId,
                snapshot.request().sessionId(),
                snapshot.request().source(),
                snapshot.status(),
                snapshot.createdAt()
        );
    }

    public List<RunRecordDto> listRuns() {
        return runRegistry.snapshots().stream()
                .map(snapshot -> new RunRecordDto(
                        snapshot.runId(),
                        snapshot.request().sessionId(),
                        snapshot.request().source(),
                        snapshot.request().modelId(),
                        snapshot.request().mode(),
                        snapshot.status(),
                        snapshot.exitCode(),
                        snapshot.message(),
                        snapshot.createdAt(),
                        snapshot.processAttached()
                ))
                .toList();
    }

    public StopRunResponse stop(String runId) {
        if (vscodeCodexRunBridge.stop(runId)) {
            runRecordRepository.markStopped(runId);
            return new StopRunResponse(runId, RunStatus.STOPPED);
        }

        ManagedProcess managedProcess = runRegistry.getRequiredProcess(runId);
        runRegistry.markStopped(runId);
        Process process = managedProcess.process();
        if (process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(PROCESS_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ApiException(
                        ErrorCode.PROCESS_STOP_FAILED,
                        HttpStatus.FAILED_DEPENDENCY,
                        "Process stop interrupted",
                        runId
                );
            }
        }
        runRecordRepository.markStopped(runId);
        runEventBus.publish(
                runId,
                new RunEventDto(runId, "stopped", "Process stopped", Instant.now(), Map.of())
        );
        return new StopRunResponse(runId, RunStatus.STOPPED);
    }

    private void stopStartedProcess(ManagedProcess managedProcess) {
        Process process = managedProcess.process();
        if (process.isAlive()) {
            process.destroy();
        }
    }

    private void markFailedAfterLaunchFailure(String runId, ApiException launchFailure) {
        try {
            runRecordRepository.markFailed(runId);
        } catch (ApiException persistenceFailure) {
            launchFailure.addSuppressed(persistenceFailure);
        }
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
        validateCodexOptions(request);
        validateOpencodeOptions(request);
    }

    private void validateCodexOptions(StartRunRequest request) {
        if (request.codexOptions() == null) {
            return;
        }
        if (request.source() != AgentSource.CODEX) {
            throw validationFailed("codexOptions wrong source");
        }
        if (!isBlank(request.codexOptions().sandbox()) && !CODEX_SANDBOX_VALUES.contains(request.codexOptions().sandbox())) {
            throw validationFailed("codexOptions sandbox");
        }
        if ("danger-full-access".equals(request.codexOptions().sandbox()) && !request.confirmDangerous()) {
            throw new ApiException(
                    ErrorCode.DANGER_CONFIRM_REQUIRED,
                    HttpStatus.BAD_REQUEST,
                    "Dangerous Codex sandbox must be confirmed",
                    "codexOptions sandbox"
            );
        }
        if (!isBlank(request.codexOptions().approvalPolicy()) && !CODEX_APPROVAL_POLICIES.contains(request.codexOptions().approvalPolicy())) {
            throw validationFailed("codexOptions approval");
        }
        rejectDirectPath("codexOptions.profile", request.codexOptions().profile());
        rejectDirectPaths("codexOptions.imageAttachmentIds", request.codexOptions().imageAttachmentIds());
        rejectDirectPaths("codexOptions.addDirAttachmentIds", request.codexOptions().addDirAttachmentIds());
        rejectDirectPath("codexOptions.outputSchemaAttachmentId", request.codexOptions().outputSchemaAttachmentId());
        if (request.codexOptions().configOverrides() != null) {
            request.codexOptions().configOverrides().forEach(override -> {
                if (override == null || isBlank(override.key()) || isBlank(override.value())) {
                    throw validationFailed("codexOptions config");
                }
                if (!CODEX_REASONING_EFFORT_KEY.equals(override.key())) {
                    throw validationFailed("codexOptions config key");
                }
                if (!CODEX_REASONING_EFFORT_VALUES.contains(override.value())) {
                    throw validationFailed("codexOptions config value");
                }
            });
        }
    }

    private void validateOpencodeOptions(StartRunRequest request) {
        if (request.opencodeOptions() == null) {
            if (request.source() == AgentSource.OPENCODE && request.codexOptions() != null) {
                throw validationFailed("codexOptions wrong source");
            }
            return;
        }
        if (request.source() != AgentSource.OPENCODE) {
            throw validationFailed("opencodeOptions wrong source");
        }
        if (request.codexOptions() != null) {
            throw validationFailed("codexOptions wrong source");
        }
        if (Boolean.TRUE.equals(request.opencodeOptions().dangerouslySkipPermissions()) && !request.confirmDangerous()) {
            throw new ApiException(
                    ErrorCode.DANGER_CONFIRM_REQUIRED,
                    HttpStatus.BAD_REQUEST,
                    "Dangerous opencode permissions skip must be confirmed",
                    "opencodeOptions dangerously-skip-permissions"
            );
        }
        Integer replayLimit = request.opencodeOptions().replayLimit();
        if (replayLimit != null && (replayLimit < OPENCODE_REPLAY_LIMIT_MIN || replayLimit > OPENCODE_REPLAY_LIMIT_MAX)) {
            throw validationFailed("opencodeOptions replayLimit");
        }
        rejectDirectPath("opencodeOptions.agent", request.opencodeOptions().agent());
        rejectDirectPath("opencodeOptions.variant", request.opencodeOptions().variant());
        rejectDirectPaths("opencodeOptions.fileAttachmentIds", request.opencodeOptions().fileAttachmentIds());
    }

    private void rejectDirectPaths(String detail, List<String> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> rejectDirectPath(detail, value));
    }

    private void rejectDirectPath(String detail, String value) {
        if (!isBlank(value) && (value.startsWith("/") || value.contains("..") || value.contains("\\"))) {
            throw validationFailed(detail);
        }
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
