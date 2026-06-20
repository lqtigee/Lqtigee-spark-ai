package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.lqtigee.sparkai.dto.AgentSource;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessLauncherTest {

    private final ProcessLauncher processLauncher = new ProcessLauncher();

    @Test
    void startRunsDeterministicLocalProcessWithoutShellString() throws Exception {
        CommandSpec spec = new CommandSpec(
                List.of("/bin/echo", "lqtigee-process-launcher"),
                Path.of(".").toAbsolutePath().normalize(),
                Map.of("LQTIGEE_PROCESS_LAUNCHER_TEST", "1"),
                AgentSource.CODEX,
                "local-session",
                "local-model"
        );

        ManagedProcess managedProcess = processLauncher.start("run-local-echo", spec);

        assertThat(managedProcess.process().waitFor()).isZero();
        assertThat(managedProcess.commandSpec().command()).doesNotContain("sh", "bash", "-c");
        assertThat(managedProcess.commandSpec().command()).doesNotContain("codex", "opencode");
    }
}
