package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RunRegistryTest {

    private final RunRegistry runRegistry = new RunRegistry();

    @Test
    void markRunningFailsAfterRunIsStopped() {
        String runId = runRegistry.create(request());
        runRegistry.markStopped(runId);

        assertThatThrownBy(() -> runRegistry.markRunning(runId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.RUN_ALREADY_FINISHED));
    }

    @Test
    void attachAndRetrieveProcessFailsAfterRunIsStopped() throws IOException {
        String runId = runRegistry.create(request());
        ManagedProcess process = new ManagedProcess(
                runId,
                new ProcessBuilder("/bin/true").start(),
                Instant.parse("2026-06-20T00:00:00Z"),
                commandSpec()
        );

        runRegistry.attachProcess(runId, process);

        assertThat(runRegistry.getRequiredProcess(runId)).isSameAs(process);

        runRegistry.markStopped(runId);

        assertThatThrownBy(() -> runRegistry.getRequiredProcess(runId))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.RUN_ALREADY_FINISHED));
    }

    private StartRunRequest request() {
        return new StartRunRequest(
                "session-id",
                AgentSource.CODEX,
                "gpt-5.5",
                CommandMode.ASK,
                "status",
                false
        );
    }

    private CommandSpec commandSpec() {
        return new CommandSpec(
                List.of("/bin/true"),
                null,
                Map.of(),
                AgentSource.CODEX,
                "session-id",
                "gpt-5.5"
        );
    }
}
