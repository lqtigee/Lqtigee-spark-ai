package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.adapter.CodexAdapter;
import com.lqtigee.sparkai.adapter.OpencodeAdapter;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionRefreshRequest;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SessionServiceTest {

    @Test
    void listAllSessionsFailsWhenCodexSucceedsAndOpencodeFails() {
        SessionService service = new SessionService(
                new EmptyCodexAdapter(),
                new FailingOpencodeAdapter()
        );

        assertThatThrownBy(service::listAllSessions)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.OPENCODE_SESSION_SCAN_FAILED));
    }

    @Test
    void getRequiredSessionReturnsExactIdForRequestedSource() {
        RemoteSessionDto codexSession = session("codex-session", AgentSource.CODEX);
        RemoteSessionDto opencodeSession = session("opencode-session", AgentSource.OPENCODE);
        SessionService service = new SessionService(
                new FixedCodexAdapter(List.of(codexSession)),
                new FixedOpencodeAdapter(List.of(opencodeSession))
        );

        RemoteSessionDto result = service.getRequiredSession(AgentSource.OPENCODE, "opencode-session");

        assertThat(result).isSameAs(opencodeSession);
    }

    @Test
    void getRequiredSessionUsesExactIdLookupInsteadOfFullDiscovery() {
        RemoteSessionDto opencodeSession = session("opencode-session", AgentSource.OPENCODE);
        CountingOpencodeAdapter opencodeAdapter = new CountingOpencodeAdapter(List.of(opencodeSession));
        SessionService service = new SessionService(
                new FixedCodexAdapter(List.of()),
                opencodeAdapter
        );

        RemoteSessionDto result = service.getRequiredSession(AgentSource.OPENCODE, "opencode-session");

        assertThat(result).isSameAs(opencodeSession);
        assertThat(opencodeAdapter.discoverSessionsCalls).isZero();
        assertThat(opencodeAdapter.discoverSessionsByIdsCalls).isEqualTo(1);
    }

    @Test
    void getRequiredSessionThrowsWhenIdIsMissingForRequestedSource() {
        RemoteSessionDto codexSession = session("shared-id", AgentSource.CODEX);
        SessionService service = new SessionService(
                new FixedCodexAdapter(List.of(codexSession)),
                new FixedOpencodeAdapter(List.of())
        );

        assertThatThrownBy(() -> service.getRequiredSession(AgentSource.OPENCODE, "shared-id"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.SESSION_NOT_FOUND));
    }

    @Test
    void refreshSessionsReturnsOnlyRequestedExistingRefs() {
        RemoteSessionDto codexRequested = session("codex-requested", AgentSource.CODEX);
        RemoteSessionDto codexUnrequested = session("codex-unrequested", AgentSource.CODEX);
        RemoteSessionDto opencodeRequested = session("opencode-requested", AgentSource.OPENCODE);
        SessionService service = new SessionService(
                new FixedCodexAdapter(List.of(codexRequested, codexUnrequested)),
                new FixedOpencodeAdapter(List.of(opencodeRequested))
        );

        List<RemoteSessionDto> result = service.refreshSessions(new SessionRefreshRequest(List.of(
                new SessionRefreshRequest.SessionRefDto(AgentSource.CODEX, "codex-requested"),
                new SessionRefreshRequest.SessionRefDto(AgentSource.OPENCODE, "opencode-requested"),
                new SessionRefreshRequest.SessionRefDto(AgentSource.CODEX, "missing")
        )));

        assertThat(result).containsExactly(codexRequested, opencodeRequested);
    }

    @Test
    void refreshSessionsUsesExactIdLookupInsteadOfFullDiscovery() {
        RemoteSessionDto codexRequested = session("codex-requested", AgentSource.CODEX);
        RemoteSessionDto opencodeRequested = session("opencode-requested", AgentSource.OPENCODE);
        CountingCodexAdapter codexAdapter = new CountingCodexAdapter(List.of(codexRequested));
        CountingOpencodeAdapter opencodeAdapter = new CountingOpencodeAdapter(List.of(opencodeRequested));
        SessionService service = new SessionService(codexAdapter, opencodeAdapter);

        List<RemoteSessionDto> result = service.refreshSessions(new SessionRefreshRequest(List.of(
                new SessionRefreshRequest.SessionRefDto(AgentSource.CODEX, "codex-requested"),
                new SessionRefreshRequest.SessionRefDto(AgentSource.OPENCODE, "opencode-requested")
        )));

        assertThat(result).containsExactly(codexRequested, opencodeRequested);
        assertThat(codexAdapter.discoverSessionsCalls).isZero();
        assertThat(opencodeAdapter.discoverSessionsCalls).isZero();
        assertThat(codexAdapter.discoverSessionsByIdsCalls).isEqualTo(1);
        assertThat(opencodeAdapter.discoverSessionsByIdsCalls).isEqualTo(1);
    }

    @Test
    void refreshSessionsRejectsBlankId() {
        SessionService service = new SessionService(
                new EmptyCodexAdapter(),
                new FixedOpencodeAdapter(List.of())
        );

        assertThatThrownBy(() -> service.refreshSessions(new SessionRefreshRequest(List.of(
                new SessionRefreshRequest.SessionRefDto(AgentSource.CODEX, " ")
        ))))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    private static RemoteSessionDto session(String id, AgentSource source) {
        return new RemoteSessionDto(
                id,
                source,
                "Title " + id,
                "/workspace/" + id,
                "model-" + id,
                SessionStatus.UNKNOWN,
                Instant.parse("2026-06-20T00:00:00Z"),
                "",
                "/sessions/" + id
        );
    }

    private static class EmptyCodexAdapter extends CodexAdapter {

        @Override
        public List<RemoteSessionDto> discoverSessions() {
            return List.of();
        }
    }

    private static class FailingOpencodeAdapter extends OpencodeAdapter {

        @Override
        public List<RemoteSessionDto> discoverSessions() {
            throw new IllegalStateException("opencode discovery failed");
        }
    }

    private static class FixedCodexAdapter extends CodexAdapter {

        private final List<RemoteSessionDto> sessions;

        private FixedCodexAdapter(List<RemoteSessionDto> sessions) {
            this.sessions = sessions;
        }

        @Override
        public List<RemoteSessionDto> discoverSessions() {
            return sessions;
        }

        @Override
        public List<RemoteSessionDto> discoverSessionsByIds(Set<String> ids) {
            return sessions.stream()
                    .filter(session -> ids.contains(session.id()))
                    .toList();
        }
    }

    private static class FixedOpencodeAdapter extends OpencodeAdapter {

        private final List<RemoteSessionDto> sessions;

        private FixedOpencodeAdapter(List<RemoteSessionDto> sessions) {
            this.sessions = sessions;
        }

        @Override
        public List<RemoteSessionDto> discoverSessions() {
            return sessions;
        }

        @Override
        public List<RemoteSessionDto> discoverSessionsByIds(Set<String> ids) {
            return sessions.stream()
                    .filter(session -> ids.contains(session.id()))
                    .toList();
        }
    }

    private static class CountingCodexAdapter extends FixedCodexAdapter {

        private int discoverSessionsCalls;
        private int discoverSessionsByIdsCalls;

        private CountingCodexAdapter(List<RemoteSessionDto> sessions) {
            super(sessions);
        }

        @Override
        public List<RemoteSessionDto> discoverSessions() {
            discoverSessionsCalls++;
            return super.discoverSessions();
        }

        @Override
        public List<RemoteSessionDto> discoverSessionsByIds(Set<String> ids) {
            discoverSessionsByIdsCalls++;
            return super.discoverSessionsByIds(ids);
        }
    }

    private static class CountingOpencodeAdapter extends FixedOpencodeAdapter {

        private int discoverSessionsCalls;
        private int discoverSessionsByIdsCalls;

        private CountingOpencodeAdapter(List<RemoteSessionDto> sessions) {
            super(sessions);
        }

        @Override
        public List<RemoteSessionDto> discoverSessions() {
            discoverSessionsCalls++;
            return super.discoverSessions();
        }

        @Override
        public List<RemoteSessionDto> discoverSessionsByIds(Set<String> ids) {
            discoverSessionsByIdsCalls++;
            return super.discoverSessionsByIds(ids);
        }
    }
}
