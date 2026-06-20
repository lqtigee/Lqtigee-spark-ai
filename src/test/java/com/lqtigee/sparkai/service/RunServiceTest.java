package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.runtime.CommandSpec;
import com.lqtigee.sparkai.runtime.ManagedProcess;
import com.lqtigee.sparkai.runtime.ProcessLauncher;
import org.junit.jupiter.api.Test;

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
