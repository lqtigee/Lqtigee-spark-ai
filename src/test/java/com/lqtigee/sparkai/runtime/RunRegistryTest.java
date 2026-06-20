package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
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
}
