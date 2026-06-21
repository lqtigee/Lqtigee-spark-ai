package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.lqtigee.sparkai.config.DatabaseProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.dto.RunStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.persistence.PostgresConnectionFactory;
import com.lqtigee.sparkai.persistence.RunRecordRepository;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProcessOutputPumpTest {

    private final CapturingRunEventBus eventBus = new CapturingRunEventBus();
    private final ProcessLauncher processLauncher = new ProcessLauncher();

    @Test
    void attachPublishesExactlyOneDoneEventWhenProcessExitsZero() {
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        ProcessOutputPump pump = new ProcessOutputPump(eventBus, null, runRecordRepository, Runnable::run);
        ManagedProcess process = processLauncher.start("run-done", command("/bin/true"));

        pump.attach("run-done", process);

        assertThat(runRecordRepository.calls()).containsExactly("markExited:run-done");
        assertThat(eventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("done");
    }

    @Test
    void attachPublishesExactlyOneErrorEventWhenProcessExitsNonZero() {
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        ProcessOutputPump pump = new ProcessOutputPump(eventBus, null, runRecordRepository, Runnable::run);
        ManagedProcess process = processLauncher.start("run-error", command("/bin/false"));

        pump.attach("run-error", process);

        assertThat(runRecordRepository.calls()).containsExactly("markFailed:run-error");
        assertThat(eventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("error");
    }

    @Test
    void attachPublishesStdoutAndStderrLineEventsBeforeTerminalEvent() {
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        ProcessOutputPump pump = new ProcessOutputPump(eventBus, null, runRecordRepository, Runnable::run);
        ManagedProcess process = processLauncher.start(
                "run-stdio",
                command("/usr/bin/python3", "-c", "import sys; print('out-one'); print('err-one', file=sys.stderr)")
        );

        pump.attach("run-stdio", process);

        List<RunEventDto> events = eventBus.events();
        assertThat(events).hasSize(3);
        assertThat(events.subList(0, 2))
                .extracting(RunEventDto::type)
                .containsExactlyInAnyOrder("stdout", "stderr");
        assertThat(events.get(2).type()).isEqualTo("done");
        assertThat(events)
                .filteredOn(event -> event.type().equals("stdout"))
                .extracting(RunEventDto::message)
                .containsExactly("out-one");
        assertThat(events)
                .filteredOn(event -> event.type().equals("stderr"))
                .extracting(RunEventDto::message)
                .containsExactly("err-one");
        assertThat(runRecordRepository.calls()).containsExactly("markExited:run-stdio");
    }

    @Test
    void attachMarksRunFailedAndPublishesOnlyErrorWhenProcessExitsSeven() {
        RunRegistry runRegistry = new RunRegistry();
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        String runId = runRegistry.create(request());
        runRegistry.markRunning(runId);
        ProcessOutputPump pumpWithRegistry = new ProcessOutputPump(eventBus, runRegistry, runRecordRepository, Runnable::run);
        ManagedProcess process = processLauncher.start(runId, command("/usr/bin/python3", "-c", "import sys; sys.exit(7)"));

        pumpWithRegistry.attach(runId, process);

        assertThat(runRecordRepository.calls()).containsExactly("markFailed:" + runId);
        assertThat(runRegistry.statusOf(runId)).isEqualTo(RunStatus.FAILED);
        assertThat(eventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("error")
                .doesNotContain("done");
    }

    @Test
    void productionConstructorDoesNotBlockCallerWhileProcessRuns() throws Exception {
        CapturingRunEventBus asyncEventBus = new CapturingRunEventBus();
        RunRegistry runRegistry = new RunRegistry();
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        String runId = runRegistry.create(request());
        runRegistry.markRunning(runId);
        ProcessOutputPump asyncPump = new ProcessOutputPump(asyncEventBus, runRegistry, runRecordRepository);
        ManagedProcess process = processLauncher.start(runId, command("/bin/sleep", "1"));

        long startedAt = System.nanoTime();
        asyncPump.attach(runId, process);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertThat(elapsedMillis).isLessThan(500L);
        assertThat(process.process().waitFor(3, TimeUnit.SECONDS)).isTrue();
        asyncEventBus.awaitEvents(1);
        assertThat(runRecordRepository.calls()).containsExactly("markExited:" + runId);
        assertThat(runRegistry.statusOf(runId)).isEqualTo(RunStatus.EXITED);
        assertThat(asyncEventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("done");
    }

    @Test
    void exitedPersistenceFailurePublishesOnlyErrorAndMarksRegistryFailed() {
        RunRegistry runRegistry = new RunRegistry();
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        runRecordRepository.failMarkExited();
        String runId = runRegistry.create(request());
        runRegistry.markRunning(runId);
        ProcessOutputPump pump = new ProcessOutputPump(eventBus, runRegistry, runRecordRepository, Runnable::run);
        ManagedProcess process = processLauncher.start(runId, command("/bin/true"));

        pump.attach(runId, process);

        assertThat(runRecordRepository.calls()).containsExactly("markExited:" + runId);
        assertThat(runRegistry.statusOf(runId)).isEqualTo(RunStatus.FAILED);
        assertThat(eventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("error")
                .doesNotContain("done");
    }

    @Test
    void failedPersistenceFailureStillPublishesExactlyOneError() {
        RunRegistry runRegistry = new RunRegistry();
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        runRecordRepository.failMarkFailed();
        String runId = runRegistry.create(request());
        runRegistry.markRunning(runId);
        ProcessOutputPump pump = new ProcessOutputPump(eventBus, runRegistry, runRecordRepository, Runnable::run);
        ManagedProcess process = processLauncher.start(runId, command("/bin/false"));

        pump.attach(runId, process);

        assertThat(runRecordRepository.calls()).containsExactly("markFailed:" + runId);
        assertThat(runRegistry.statusOf(runId)).isEqualTo(RunStatus.FAILED);
        assertThat(eventBus.events())
                .extracting(RunEventDto::type)
                .containsExactly("error");
    }

    @Test
    void attachSkipsTerminalPersistenceAndEventWhenRunIsAlreadyStopped() {
        RunRegistry runRegistry = new RunRegistry();
        RecordingRunRecordRepository runRecordRepository = new RecordingRunRecordRepository();
        String runId = runRegistry.create(request());
        runRegistry.markRunning(runId);
        runRegistry.markStopped(runId);
        ProcessOutputPump pump = new ProcessOutputPump(eventBus, runRegistry, runRecordRepository, Runnable::run);
        ManagedProcess process = processLauncher.start(runId, command("/bin/true"));

        pump.attach(runId, process);

        assertThat(runRecordRepository.calls()).isEmpty();
        assertThat(runRegistry.statusOf(runId)).isEqualTo(RunStatus.STOPPED);
        assertThat(eventBus.events()).isEmpty();
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

        void awaitEvents(int count) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (events.size() < count && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
        }

        List<RunEventDto> events() {
            return events;
        }
    }

    private static class RecordingRunRecordRepository extends RunRecordRepository {

        private final List<String> calls = new ArrayList<>();
        private ApiException markExitedFailure;
        private ApiException markFailedFailure;

        RecordingRunRecordRepository() {
            super(new NeverOpenConnectionFactory());
        }

        @Override
        public void markExited(String runId) {
            calls.add("markExited:" + runId);
            if (markExitedFailure != null) {
                throw markExitedFailure;
            }
        }

        @Override
        public void markFailed(String runId) {
            calls.add("markFailed:" + runId);
            if (markFailedFailure != null) {
                throw markFailedFailure;
            }
        }

        List<String> calls() {
            return calls;
        }

        void failMarkExited() {
            markExitedFailure = persistenceFailure("exit persistence failed");
        }

        void failMarkFailed() {
            markFailedFailure = persistenceFailure("failed persistence failed");
        }
    }

    private static class NeverOpenConnectionFactory extends PostgresConnectionFactory {

        NeverOpenConnectionFactory() {
            super(new DatabaseProperties());
        }

        @Override
        public Connection open() {
            throw new AssertionError("ProcessOutputPumpTest must not open PostgreSQL");
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
