package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.config.DatabaseProperties;
import com.lqtigee.sparkai.config.ModelProperties;
import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CodexRunOptionsDto;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.OpencodeRunOptionsDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.dto.RunStatus;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.dto.StopRunResponse;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.persistence.PostgresConnectionFactory;
import com.lqtigee.sparkai.persistence.RunRecordRepository;
import com.lqtigee.sparkai.runtime.CodexCommandBuilder;
import com.lqtigee.sparkai.runtime.CommandSpec;
import com.lqtigee.sparkai.runtime.ManagedProcess;
import com.lqtigee.sparkai.runtime.OpencodeCommandBuilder;
import com.lqtigee.sparkai.runtime.ProcessLauncher;
import com.lqtigee.sparkai.runtime.ProcessOutputPump;
import com.lqtigee.sparkai.runtime.RunEventBus;
import com.lqtigee.sparkai.runtime.RunRegistry;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RunServiceTest {

    @Test
    void startRejectsBlankPromptBeforeLaunchingProcess() {
        Fixture fixture = fixture(8);

        assertThatThrownBy(() -> fixture.service().start(request("session-id", AgentSource.CODEX, "gpt-5.5", " ")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.PROMPT_EMPTY));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsOverLimitPromptBeforeLaunchingProcess() {
        Fixture fixture = fixture(8);

        assertThatThrownBy(() -> fixture.service().start(request("session-id", AgentSource.CODEX, "gpt-5.5", "123456789")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.PROMPT_TOO_LONG));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsMissingSourceBeforeLaunchingProcess() {
        Fixture fixture = fixture(8);

        assertThatThrownBy(() -> fixture.service().start(request("session-id", null, "gpt-5.5", "status")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsMissingSessionIdBeforeLaunchingProcess() {
        Fixture fixture = fixture(8);

        assertThatThrownBy(() -> fixture.service().start(request(" ", AgentSource.CODEX, "gpt-5.5", "status")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsCodexOptionsForWrongSourceBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithCodexOptions(
                AgentSource.OPENCODE,
                codexOptions(null, null, List.of("att_image_01"), List.of("att_dir_01"), null)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(exception.detail()).contains("codexOptions wrong source");
                });
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsUnsupportedCodexSandboxBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithCodexOptions(
                AgentSource.CODEX,
                codexOptions("unsafe", null, List.of("att_image_01"), List.of("att_dir_01"), null)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsUnsupportedCodexApprovalBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithCodexOptions(
                AgentSource.CODEX,
                codexOptions("workspace-write", "always", List.of("att_image_01"), List.of("att_dir_01"), null)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRequiresDangerConfirmationForDangerFullAccessSandboxBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithCodexOptions(
                AgentSource.CODEX,
                codexOptions("danger-full-access", "on-request", List.of("att_image_01"), List.of("att_dir_01"), null)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.DANGER_CONFIRM_REQUIRED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsDirectFrontendPathsBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithCodexOptions(
                AgentSource.CODEX,
                codexOptions("workspace-write", "on-request", List.of("/tmp/image.png"), List.of("att_dir_01"), null)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsOpencodeOptionsForWrongSourceBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithOpencodeOptions(
                AgentSource.CODEX,
                opencodeOptions(10, List.of("att_file_01"), false)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(exception.detail()).contains("opencodeOptions wrong source");
                });
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsCodexOptionsForOpencodeSourceBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithCodexOptions(
                AgentSource.OPENCODE,
                codexOptions(null, null, List.of("att_image_01"), List.of("att_dir_01"), null)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRequiresDangerConfirmationForOpencodePermissionSkipBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithOpencodeOptions(
                AgentSource.OPENCODE,
                opencodeOptions(10, List.of("att_file_01"), true)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.DANGER_CONFIRM_REQUIRED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsOutOfRangeOpencodeReplayLimitBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithOpencodeOptions(
                AgentSource.OPENCODE,
                opencodeOptions(0, List.of("att_file_01"), false)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startRejectsDirectOpencodeFilePathsBeforeLaunchingProcess() {
        Fixture fixture = fixture(64);

        assertThatThrownBy(() -> fixture.service().start(requestWithOpencodeOptions(
                AgentSource.OPENCODE,
                opencodeOptions(10, List.of("/tmp/context.txt"), false)
        )))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void startPersistsStartedAndRunningBeforeAttachingOutputPump() {
        RuntimeFixture fixture = runtimeFixture();

        StartRunResponse response = fixture.service().start(request("session-id", AgentSource.CODEX, "gpt-5.5", "status"));

        assertThat(response.status()).isEqualTo(RunStatus.RUNNING);
        assertThat(fixture.runRecordRepository().calls()).containsExactly(
                "saveStarted:CODEX:session-id:gpt-5.5",
                "markRunning:" + response.runId()
        );
        assertThat(fixture.outputPump().attachedRunIds()).containsExactly(response.runId());
        assertThat(fixture.runRegistry().statusOf(response.runId())).isEqualTo(RunStatus.RUNNING);
    }

    @Test
    void startDoesNotLaunchProcessWhenSaveStartedFails() {
        RuntimeFixture fixture = runtimeFixture();
        fixture.runRecordRepository().failSaveStarted();

        assertThatThrownBy(() -> fixture.service().start(request("session-id", AgentSource.CODEX, "gpt-5.5", "status")))
                .isInstanceOf(ApiException.class);

        assertThat(fixture.launcher().calls()).isZero();
        assertThat(fixture.outputPump().attachedRunIds()).isEmpty();
        assertThat(fixture.runRecordRepository().calls()).containsExactly("saveStarted:CODEX:session-id:gpt-5.5");
    }

    @Test
    void startMarksFailedAndRethrowsWhenProcessLaunchFails() {
        RuntimeFixture fixture = runtimeFixture();
        fixture.launcher().failStart();

        assertThatThrownBy(() -> fixture.service().start(request("session-id", AgentSource.CODEX, "gpt-5.5", "status")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.PROCESS_START_FAILED));

        assertThat(fixture.runRecordRepository().calls()).containsExactly(
                "saveStarted:CODEX:session-id:gpt-5.5",
                "markFailed:" + fixture.launcher().lastRunId()
        );
        assertThat(fixture.outputPump().attachedRunIds()).isEmpty();
        assertThat(fixture.runRegistry().statusOf(fixture.launcher().lastRunId())).isEqualTo(RunStatus.FAILED);
    }

    @Test
    void startDestroysProcessAndDoesNotAttachPumpWhenMarkRunningFails() {
        RuntimeFixture fixture = runtimeFixture();
        fixture.runRecordRepository().failMarkRunning();

        assertThatThrownBy(() -> fixture.service().start(request("session-id", AgentSource.CODEX, "gpt-5.5", "status")))
                .isInstanceOf(ApiException.class);

        assertThat(fixture.launcher().lastProcess().destroyed()).isTrue();
        assertThat(fixture.outputPump().attachedRunIds()).isEmpty();
        assertThat(fixture.runRegistry().statusOf(fixture.launcher().lastRunId())).isEqualTo(RunStatus.FAILED);
        assertThat(fixture.runRecordRepository().calls()).containsExactly(
                "saveStarted:CODEX:session-id:gpt-5.5",
                "markRunning:" + fixture.launcher().lastRunId()
        );
    }

    @Test
    void stopPersistsStoppedBeforeRegistryAndEventSuccess() {
        RuntimeFixture fixture = runtimeFixture();
        StartRunResponse response = fixture.service().start(request("session-id", AgentSource.CODEX, "gpt-5.5", "status"));

        StopRunResponse stopResponse = fixture.service().stop(response.runId());

        assertThat(stopResponse.status()).isEqualTo(RunStatus.STOPPED);
        assertThat(fixture.runRecordRepository().calls()).contains(
                "markStopped:" + response.runId()
        );
        assertThat(fixture.runRegistry().statusOf(response.runId())).isEqualTo(RunStatus.STOPPED);
        assertThat(fixture.eventBus().events()).extracting(RunEventDto::type).containsExactly("stopped");
    }

    @Test
    void stopDoesNotPublishSuccessWhenMarkStoppedFails() {
        RuntimeFixture fixture = runtimeFixture();
        StartRunResponse response = fixture.service().start(request("session-id", AgentSource.CODEX, "gpt-5.5", "status"));
        fixture.runRecordRepository().failMarkStopped();

        assertThatThrownBy(() -> fixture.service().stop(response.runId()))
                .isInstanceOf(ApiException.class);

        assertThat(fixture.runRegistry().statusOf(response.runId())).isEqualTo(RunStatus.RUNNING);
        assertThat(fixture.eventBus().events()).isEmpty();
    }

    private Fixture fixture(int maxPromptChars) {
        CountingProcessLauncher launcher = new CountingProcessLauncher();
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        RemoteProperties remoteProperties = new RemoteProperties();
        remoteProperties.setMaxPromptChars(maxPromptChars);
        RunService service = new RunService(
                null,
                null,
                null,
                null,
                launcher,
                null,
                null,
                null,
                remoteProperties,
                runRecordRepository
        );
        return new Fixture(service, launcher, runRecordRepository);
    }

    private RuntimeFixture runtimeFixture() {
        RemoteProperties remoteProperties = new RemoteProperties();
        remoteProperties.setMaxPromptChars(64);
        RunRegistry runRegistry = new RunRegistry();
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        ControllableProcessLauncher launcher = new ControllableProcessLauncher();
        RecordingProcessOutputPump outputPump = new RecordingProcessOutputPump();
        CapturingRunEventBus eventBus = new CapturingRunEventBus();
        RunService service = new RunService(
                new FixedSessionService(),
                new FixedModelService(),
                new FixedCodexCommandBuilder(),
                new FixedOpencodeCommandBuilder(),
                launcher,
                outputPump,
                eventBus,
                runRegistry,
                remoteProperties,
                runRecordRepository
        );
        return new RuntimeFixture(service, launcher, outputPump, eventBus, runRegistry, runRecordRepository);
    }

    private StartRunRequest request(String sessionId, AgentSource source, String modelId, String prompt) {
        return new StartRunRequest(
                sessionId,
                source,
                modelId,
                CommandMode.ASK,
                prompt,
                false
        );
    }

    private StartRunRequest requestWithCodexOptions(AgentSource source, CodexRunOptionsDto codexOptions) {
        return new StartRunRequest(
                "session-id",
                source,
                source == AgentSource.CODEX ? "gpt-5.5" : "openai/Lqtigee",
                CommandMode.ASK,
                "status",
                false,
                codexOptions
        );
    }

    private StartRunRequest requestWithOpencodeOptions(AgentSource source, OpencodeRunOptionsDto opencodeOptions) {
        return new StartRunRequest(
                "session-id",
                source,
                source == AgentSource.CODEX ? "gpt-5.5" : "openai/Lqtigee",
                CommandMode.ASK,
                "status",
                false,
                null,
                opencodeOptions
        );
    }

    private CodexRunOptionsDto codexOptions(
            String sandbox,
            String approval,
            List<String> imageAttachmentIds,
            List<String> addDirAttachmentIds,
            String outputSchemaAttachmentId
    ) {
        return new CodexRunOptionsDto(
                imageAttachmentIds,
                "work",
                sandbox,
                approval,
                true,
                addDirAttachmentIds,
                List.of(new CodexRunOptionsDto.ConfigOverrideDto("model_reasoning_effort", "high")),
                outputSchemaAttachmentId
        );
    }

    private OpencodeRunOptionsDto opencodeOptions(int replayLimit, List<String> fileAttachmentIds, boolean dangerouslySkipPermissions) {
        return new OpencodeRunOptionsDto(
                "build",
                false,
                false,
                "high",
                true,
                true,
                replayLimit,
                fileAttachmentIds,
                dangerouslySkipPermissions
        );
    }

    private record Fixture(
            RunService service,
            CountingProcessLauncher launcher,
            RecordingRunRecordRepository runRecordRepository
    ) {
    }

    private record RuntimeFixture(
            RunService service,
            ControllableProcessLauncher launcher,
            RecordingProcessOutputPump outputPump,
            CapturingRunEventBus eventBus,
            RunRegistry runRegistry,
            RecordingRunRecordRepository runRecordRepository
    ) {
    }

    private static class CountingProcessLauncher extends ProcessLauncher {

        private int calls;

        @Override
        public ManagedProcess start(String runId, CommandSpec spec) {
            calls++;
            throw new AssertionError("Process launcher must not be called for invalid requests");
        }

        private int calls() {
            return calls;
        }
    }

    private static class ControllableProcessLauncher extends ProcessLauncher {

        private int calls;
        private String lastRunId;
        private TestProcess lastProcess;
        private boolean failStart;

        @Override
        public ManagedProcess start(String runId, CommandSpec spec) {
            calls++;
            lastRunId = runId;
            if (failStart) {
                throw new ApiException(
                        ErrorCode.PROCESS_START_FAILED,
                        HttpStatus.FAILED_DEPENDENCY,
                        "Process start failed",
                        "boom"
                );
            }
            lastProcess = new TestProcess();
            return new ManagedProcess(runId, lastProcess, Instant.parse("2026-06-20T00:00:00Z"), spec);
        }

        void failStart() {
            failStart = true;
        }

        int calls() {
            return calls;
        }

        String lastRunId() {
            return lastRunId;
        }

        TestProcess lastProcess() {
            return lastProcess;
        }
    }

    private static class RecordingProcessOutputPump extends ProcessOutputPump {

        private final List<String> attachedRunIds = new ArrayList<>();

        RecordingProcessOutputPump() {
            super(new RunEventBus(), new RunRegistry());
        }

        @Override
        public void attach(String runId, ManagedProcess process) {
            attachedRunIds.add(runId);
        }

        List<String> attachedRunIds() {
            return attachedRunIds;
        }
    }

    private static class CapturingRunEventBus extends RunEventBus {

        private final List<RunEventDto> events = new ArrayList<>();

        @Override
        public void publish(String runId, RunEventDto event) {
            events.add(event);
        }

        List<RunEventDto> events() {
            return events;
        }
    }

    private static class FixedSessionService extends SessionService {

        FixedSessionService() {
            super(null, null);
        }

        @Override
        public RemoteSessionDto getRequiredSession(AgentSource source, String id) {
            return new RemoteSessionDto(
                    id,
                    source,
                    "Session " + id,
                    "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
                    "gpt-5.5",
                    SessionStatus.IDLE,
                    Instant.parse("2026-06-20T00:00:00Z"),
                    "",
                    "/home/lqtiger/.codex/sessions/" + id + ".jsonl"
            );
        }
    }

    private static class FixedModelService extends ModelService {

        FixedModelService() {
            super(modelProperties());
        }

        @Override
        public ModelDto getRequiredModel(String id) {
            return model();
        }

        @Override
        public void validateModelForSource(String id, AgentSource source) {
        }

        private static ModelProperties modelProperties() {
            ModelProperties properties = new ModelProperties();
            ModelProperties.Entry entry = new ModelProperties.Entry();
            entry.setId("gpt-5.5");
            entry.setLabel("GPT-5.5");
            entry.setCommandModelName("gpt-5.5");
            entry.setSources(List.of(AgentSource.CODEX));
            entry.setEnabled(true);
            properties.setEntries(List.of(entry));
            return properties;
        }

        private static ModelDto model() {
            return new ModelDto(
                    "gpt-5.5",
                    "GPT-5.5",
                    "gpt-5.5",
                    List.of(AgentSource.CODEX),
                    true
            );
        }
    }

    private static class FixedCodexCommandBuilder extends CodexCommandBuilder {

        FixedCodexCommandBuilder() {
            super(null);
        }

        @Override
        public CommandSpec build(StartRunRequest request, RemoteSessionDto session, ModelDto model) {
            return commandSpec(request, model);
        }
    }

    private static class FixedOpencodeCommandBuilder extends OpencodeCommandBuilder {

        FixedOpencodeCommandBuilder() {
            super(null);
        }

        @Override
        public CommandSpec build(StartRunRequest request, RemoteSessionDto session, ModelDto model) {
            return commandSpec(request, model);
        }
    }

    private static CommandSpec commandSpec(StartRunRequest request, ModelDto model) {
        return new CommandSpec(
                List.of("/bin/true"),
                Path.of(".").toAbsolutePath().normalize(),
                Map.of(),
                request.source(),
                request.sessionId(),
                model.id()
        );
    }

    private static class TestProcess extends Process {

        private boolean destroyed;

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }

        @Override
        public Process destroyForcibly() {
            destroyed = true;
            return this;
        }

        @Override
        public boolean isAlive() {
            return !destroyed;
        }

        boolean destroyed() {
            return destroyed;
        }
    }

    private static class RecordingRunRecordRepository extends RunRecordRepository {

        private final List<String> calls = new ArrayList<>();
        private ApiException saveStartedFailure;
        private ApiException markRunningFailure;
        private ApiException markStoppedFailure;

        RecordingRunRecordRepository() {
            super(new NeverOpenConnectionFactory());
        }

        @Override
        public void saveStarted(String runId, String source, String sessionId, String modelId) {
            calls.add("saveStarted:" + source + ":" + sessionId + ":" + modelId);
            if (saveStartedFailure != null) {
                throw saveStartedFailure;
            }
        }

        @Override
        public void markRunning(String runId) {
            calls.add("markRunning:" + runId);
            if (markRunningFailure != null) {
                throw markRunningFailure;
            }
        }

        @Override
        public void markStopped(String runId) {
            calls.add("markStopped:" + runId);
            if (markStoppedFailure != null) {
                throw markStoppedFailure;
            }
        }

        @Override
        public void markFailed(String runId) {
            calls.add("markFailed:" + runId);
        }

        List<String> calls() {
            return calls;
        }

        void failSaveStarted() {
            saveStartedFailure = persistenceFailure("save failed");
        }

        void failMarkRunning() {
            markRunningFailure = persistenceFailure("running failed");
        }

        void failMarkStopped() {
            markStoppedFailure = persistenceFailure("stop failed");
        }
    }

    private static class NeverOpenConnectionFactory extends PostgresConnectionFactory {

        NeverOpenConnectionFactory() {
            super(new DatabaseProperties());
        }

        @Override
        public Connection open() {
            throw new AssertionError("RunServiceTest must not open PostgreSQL");
        }
    }

    private static ApiException persistenceFailure(String detail) {
        return new ApiException(
                ErrorCode.PROCESS_START_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                "Run record persistence failed",
                detail
        );
    }
}
