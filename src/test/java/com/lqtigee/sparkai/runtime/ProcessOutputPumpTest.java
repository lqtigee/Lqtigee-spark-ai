package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.dto.RunStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class ProcessOutputPumpTest {

    private final CapturingRunEventBus eventBus = new CapturingRunEventBus();
    private final ProcessLauncher processLauncher = new ProcessLauncher();
    private final ProcessOutputPump outputPump = new ProcessOutputPump(eventBus, Runnable::run);

    @Test
    void attachPublishesExactlyOneDoneEventWhenProcessExitsZero() {
        ManagedProcess process = processLauncher.start("run-done", command("/bin/true"));

        outputPump.attach("run-done", process);

        assertThat(eventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("done");
    }

    @Test
    void attachPublishesExactlyOneErrorEventWhenProcessExitsNonZero() {
        ManagedProcess process = processLauncher.start("run-error", command("/bin/false"));

        outputPump.attach("run-error", process);

        assertThat(eventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("error");
    }

    @Test
    void attachMarksRunFailedAndPublishesOnlyErrorWhenProcessExitsSeven() {
        RunRegistry runRegistry = new RunRegistry();
        String runId = runRegistry.create(request());
        runRegistry.markRunning(runId);
        ProcessOutputPump pumpWithRegistry = new ProcessOutputPump(eventBus, runRegistry, Runnable::run);
        ManagedProcess process = processLauncher.start(runId, command("/usr/bin/python3", "-c", "import sys; sys.exit(7)"));

        pumpWithRegistry.attach(runId, process);

        assertThat(runRegistry.statusOf(runId)).isEqualTo(RunStatus.FAILED);
        assertThat(eventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("error")
                .doesNotContain("done");
    }

    private CommandSpec command(String executable) {
        return command(executable, new String[0]);
    }

    private CommandSpec command(String executable, String... args) {
        List<String> command = new java.util.ArrayList<>();
        command.add(executable);
        command.addAll(List.of(args));
        return new CommandSpec(
                List.copyOf(command),
                Path.of(".").toAbsolutePath().normalize(),
                Map.of(),
                AgentSource.CODEX,
                "local-session",
                "local-model"
        );
    }

    private StartRunRequest request() {
        return new StartRunRequest(
                "local-session",
                AgentSource.CODEX,
                "local-model",
                CommandMode.ASK,
                "status",
                false
        );
    }

    private static class CapturingRunEventBus extends RunEventBus {

        private final List<RunEventDto> events = new CopyOnWriteArrayList<>();

        @Override
        public void publish(String runId, RunEventDto event) {
            events.add(event);
        }

        List<RunEventDto> events() {
            return events;
        }
    }
}
