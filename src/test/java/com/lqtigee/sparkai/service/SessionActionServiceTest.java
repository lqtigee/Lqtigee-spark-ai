package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionActionRequest;
import com.lqtigee.sparkai.dto.SessionActionResponse;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.runtime.CodexSessionActionCommandBuilder;
import com.lqtigee.sparkai.runtime.CommandSpec;
import com.lqtigee.sparkai.runtime.ManagedProcess;
import com.lqtigee.sparkai.runtime.OpencodeSessionActionCommandBuilder;
import com.lqtigee.sparkai.runtime.ProcessLauncher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SessionActionServiceTest {

    private static final String CODEX_SESSION_ID = "019ee090-24e8-7ac1-bd1c-8e4d6788fbf1";
    private static final String OPENCODE_SESSION_ID = "ses_121488be4ffeSI5wIkwYvHniqr";
    private static final Instant STARTED_AT = Instant.parse("2026-06-20T00:00:00Z");

    @Test
    void startsCodexArchiveActionFromVerifiedSessionAndCapability() {
        Fixture fixture = fixture();

        SessionActionResponse response = fixture.service().startAction(
                AgentSource.CODEX,
                CODEX_SESSION_ID,
                new SessionActionRequest("archive", false)
        );

        assertThat(response.actionId()).startsWith("act_");
        assertThat(response.source()).isEqualTo(AgentSource.CODEX);
        assertThat(response.sessionId()).isEqualTo(CODEX_SESSION_ID);
        assertThat(response.action()).isEqualTo("archive");
        assertThat(response.status()).isEqualTo("STARTED");
        assertThat(response.startedAt()).isEqualTo(STARTED_AT);
        assertThat(fixture.sessionService().lookups()).containsExactly("CODEX:" + CODEX_SESSION_ID);
        assertThat(fixture.launcher().lastCommand()).containsExactly("codex", "archive", CODEX_SESSION_ID);
        assertThat(fixture.launcher().lastProcess().inputClosed()).isTrue();
        assertThat(fixture.launcher().lastProcess().waited()).isTrue();
    }

    @Test
    void startsOpencodeExportActionFromVerifiedSessionAndCapability() {
        Fixture fixture = fixture();

        SessionActionResponse response = fixture.service().startAction(
                AgentSource.OPENCODE,
                OPENCODE_SESSION_ID,
                new SessionActionRequest("export", false)
        );

        assertThat(response.source()).isEqualTo(AgentSource.OPENCODE);
        assertThat(response.sessionId()).isEqualTo(OPENCODE_SESSION_ID);
        assertThat(response.action()).isEqualTo("export");
        assertThat(response.status()).isEqualTo("STARTED");
        assertThat(fixture.launcher().lastCommand()).containsExactly("opencode", "export", OPENCODE_SESSION_ID);
        assertThat(fixture.launcher().lastProcess().inputClosed()).isTrue();
    }

    @Test
    void rejectsBlankActionBeforeLaunchingProcess() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service().startAction(
                AgentSource.CODEX,
                CODEX_SESSION_ID,
                new SessionActionRequest(" ", false)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));

        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void rejectsActionNotEnabledByCapabilitiesBeforeLaunchingProcess() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service().startAction(
                AgentSource.OPENCODE,
                OPENCODE_SESSION_ID,
                new SessionActionRequest("fork", false)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));

        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void verifiesSelectedSessionBeforeLaunchingProcess() {
        Fixture fixture = fixture();
        fixture.sessionService().failLookup();

        assertThatThrownBy(() -> fixture.service().startAction(
                AgentSource.CODEX,
                CODEX_SESSION_ID,
                new SessionActionRequest("archive", false)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo(ErrorCode.SESSION_NOT_FOUND));

        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void destructiveCodexDeleteRequiresConfirmation() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service().startAction(
                AgentSource.CODEX,
                CODEX_SESSION_ID,
                new SessionActionRequest("delete", false)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo(ErrorCode.DANGER_CONFIRM_REQUIRED));

        assertThat(fixture.launcher().calls()).isZero();
    }

    @Test
    void destructiveOpencodeDeleteRequiresConfirmation() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service().startAction(
                AgentSource.OPENCODE,
                OPENCODE_SESSION_ID,
                new SessionActionRequest("delete", false)
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo(ErrorCode.DANGER_CONFIRM_REQUIRED));

        assertThat(fixture.launcher().calls()).isZero();
    }

    private Fixture fixture() {
        FixedSessionService sessionService = new FixedSessionService();
        RecordingProcessLauncher launcher = new RecordingProcessLauncher();
        SessionActionService service = new SessionActionService(
                sessionService,
                new CapabilityService(),
                new CodexSessionActionCommandBuilder(),
                new OpencodeSessionActionCommandBuilder(),
                launcher,
                Runnable::run
        );
        return new Fixture(service, sessionService, launcher);
    }

    private record Fixture(
            SessionActionService service,
            FixedSessionService sessionService,
            RecordingProcessLauncher launcher
    ) {
    }

    private static class FixedSessionService extends SessionService {

        private final List<String> lookups = new java.util.ArrayList<>();
        private boolean failLookup;

        FixedSessionService() {
            super(null, null);
        }

        @Override
        public RemoteSessionDto getRequiredSession(AgentSource source, String id) {
            lookups.add(source.name() + ":" + id);
            if (failLookup) {
                throw new ApiException(
                        ErrorCode.SESSION_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Session not found",
                        id
                );
            }
            return new RemoteSessionDto(
                    id,
                    source,
                    "session",
                    "/home/lqtiger",
                    source == AgentSource.CODEX ? "gpt-5.5" : "openai/Lqtigee",
                    SessionStatus.IDLE,
                    STARTED_AT,
                    "",
                    ""
            );
        }

        List<String> lookups() {
            return lookups;
        }

        void failLookup() {
            failLookup = true;
        }
    }

    private static class RecordingProcessLauncher extends ProcessLauncher {

        private int calls;
        private List<String> lastCommand = List.of();
        private TestProcess lastProcess;

        @Override
        public ManagedProcess start(String runId, CommandSpec spec) {
            calls++;
            lastCommand = spec.command();
            lastProcess = new TestProcess();
            return new ManagedProcess(runId, lastProcess, STARTED_AT, spec);
        }

        int calls() {
            return calls;
        }

        List<String> lastCommand() {
            return lastCommand;
        }

        TestProcess lastProcess() {
            return lastProcess;
        }
    }

    private static class TestProcess extends Process {

        private final TrackingOutputStream input = new TrackingOutputStream();
        private boolean waited;

        @Override
        public OutputStream getOutputStream() {
            return input;
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
            waited = true;
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
        }

        boolean inputClosed() {
            return input.closed();
        }

        boolean waited() {
            return waited;
        }
    }

    private static class TrackingOutputStream extends OutputStream {

        private boolean closed;

        @Override
        public void write(int b) {
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }

        boolean closed() {
            return closed;
        }
    }
}
