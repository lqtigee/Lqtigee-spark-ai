package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RunEventDto;
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

    private CommandSpec command(String executable) {
        return new CommandSpec(
                List.of(executable),
                Path.of(".").toAbsolutePath().normalize(),
                Map.of(),
                AgentSource.CODEX,
                "local-session",
                "local-model"
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
