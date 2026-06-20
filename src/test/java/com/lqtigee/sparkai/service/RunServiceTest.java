package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CodexRunOptionsDto;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.OpencodeRunOptionsDto;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.runtime.CommandSpec;
import com.lqtigee.sparkai.runtime.ManagedProcess;
import com.lqtigee.sparkai.runtime.ProcessLauncher;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private Fixture fixture(int maxPromptChars) {
        CountingProcessLauncher launcher = new CountingProcessLauncher();
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
                remoteProperties
        );
        return new Fixture(service, launcher);
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

    private record Fixture(RunService service, CountingProcessLauncher launcher) {
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
}
