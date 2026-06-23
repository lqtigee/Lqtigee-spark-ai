package com.lqtigee.sparkai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.adapter.CodexAdapter;
import com.lqtigee.sparkai.adapter.OpencodeAdapter;
import com.lqtigee.sparkai.codex.CodexTranscriptReader;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionMessageDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.dto.SessionTranscriptDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.opencode.OpencodeSqliteTranscriptReader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SessionTranscriptServiceTest {

    @Test
    void getTranscriptReadsCodexMessagesFromSelectedSessionRawFile() {
        RemoteSessionDto codexSession = session("codex-session", AgentSource.CODEX, "/sessions/codex.jsonl");
        FixedCodexTranscriptReader codexReader = new FixedCodexTranscriptReader(List.of(message("msg-1", "user")));
        FixedOpencodeTranscriptReader opencodeReader = new FixedOpencodeTranscriptReader(List.of());
        SessionTranscriptService service = service(List.of(codexSession), List.of(), codexReader, opencodeReader);

        SessionTranscriptDto transcript = service.getTranscript(AgentSource.CODEX, "codex-session");

        assertThat(transcript.session()).isEqualTo(codexSession);
        assertThat(transcript.messages()).extracting(SessionMessageDto::id).containsExactly("msg-1");
        assertThat(codexReader.path).isEqualTo(Path.of("/sessions/codex.jsonl"));
        assertThat(opencodeReader.called).isFalse();
    }

    @Test
    void getTranscriptPassesLimitAndBeforeCursorToCodexReader() {
        RemoteSessionDto codexSession = session("codex-session", AgentSource.CODEX, "/sessions/codex.jsonl");
        FixedCodexTranscriptReader codexReader = new FixedCodexTranscriptReader(List.of(message("msg-1", "user")));
        FixedOpencodeTranscriptReader opencodeReader = new FixedOpencodeTranscriptReader(List.of());
        SessionTranscriptService service = service(List.of(codexSession), List.of(), codexReader, opencodeReader);

        SessionTranscriptDto transcript = service.getTranscript(AgentSource.CODEX, "codex-session", 10, "cursor-1");

        assertThat(transcript.session()).isEqualTo(codexSession);
        assertThat(transcript.pageInfo().oldestCursor()).isEqualTo("msg-1");
        assertThat(codexReader.path).isEqualTo(Path.of("/sessions/codex.jsonl"));
        assertThat(codexReader.limit).isEqualTo(10);
        assertThat(codexReader.beforeCursor).isEqualTo("cursor-1");
        assertThat(opencodeReader.called).isFalse();
    }

    @Test
    void getTranscriptReadsOpencodeMessagesFromSelectedSessionDatabasePathAndId() {
        RemoteSessionDto opencodeSession = session("opencode-session", AgentSource.OPENCODE, "/sessions/opencode.db");
        FixedCodexTranscriptReader codexReader = new FixedCodexTranscriptReader(List.of());
        FixedOpencodeTranscriptReader opencodeReader = new FixedOpencodeTranscriptReader(List.of(message("msg-2", "assistant")));
        SessionTranscriptService service = service(List.of(), List.of(opencodeSession), codexReader, opencodeReader);

        SessionTranscriptDto transcript = service.getTranscript(AgentSource.OPENCODE, "opencode-session");

        assertThat(transcript.session()).isEqualTo(opencodeSession);
        assertThat(transcript.messages()).extracting(SessionMessageDto::id).containsExactly("msg-2");
        assertThat(opencodeReader.path).isEqualTo(Path.of("/sessions/opencode.db"));
        assertThat(opencodeReader.sessionId).isEqualTo("opencode-session");
        assertThat(codexReader.called).isFalse();
    }

    @Test
    void getTranscriptPassesLimitAndBeforeCursorToOpencodeReader() {
        RemoteSessionDto opencodeSession = session("opencode-session", AgentSource.OPENCODE, "/sessions/opencode.db");
        FixedCodexTranscriptReader codexReader = new FixedCodexTranscriptReader(List.of());
        FixedOpencodeTranscriptReader opencodeReader = new FixedOpencodeTranscriptReader(List.of(message("msg-2", "assistant")));
        SessionTranscriptService service = service(List.of(), List.of(opencodeSession), codexReader, opencodeReader);

        SessionTranscriptDto transcript = service.getTranscript(AgentSource.OPENCODE, "opencode-session", 7, "cursor-2");

        assertThat(transcript.session()).isEqualTo(opencodeSession);
        assertThat(transcript.pageInfo().oldestCursor()).isEqualTo("msg-2");
        assertThat(opencodeReader.path).isEqualTo(Path.of("/sessions/opencode.db"));
        assertThat(opencodeReader.sessionId).isEqualTo("opencode-session");
        assertThat(opencodeReader.limit).isEqualTo(7);
        assertThat(opencodeReader.beforeCursor).isEqualTo("cursor-2");
        assertThat(codexReader.called).isFalse();
    }

    @Test
    void getTranscriptFailsWhenSessionIsMissing() {
        SessionTranscriptService service = service(List.of(), List.of(), new FixedCodexTranscriptReader(List.of()), new FixedOpencodeTranscriptReader(List.of()));

        assertThatThrownBy(() -> service.getTranscript(AgentSource.CODEX, "missing"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.SESSION_NOT_FOUND));
    }

    @Test
    void getTranscriptRejectsLimitLessThanOne() {
        RemoteSessionDto codexSession = session("codex-session", AgentSource.CODEX, "/sessions/codex.jsonl");
        SessionTranscriptService service = service(
                List.of(codexSession),
                List.of(),
                new FixedCodexTranscriptReader(List.of()),
                new FixedOpencodeTranscriptReader(List.of())
        );

        assertThatThrownBy(() -> service.getTranscript(AgentSource.CODEX, "codex-session", 0, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void getTranscriptRejectsLimitAboveMaximum() {
        RemoteSessionDto codexSession = session("codex-session", AgentSource.CODEX, "/sessions/codex.jsonl");
        SessionTranscriptService service = service(
                List.of(codexSession),
                List.of(),
                new FixedCodexTranscriptReader(List.of()),
                new FixedOpencodeTranscriptReader(List.of())
        );

        assertThatThrownBy(() -> service.getTranscript(AgentSource.CODEX, "codex-session", 101, null))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    private SessionTranscriptService service(
            List<RemoteSessionDto> codexSessions,
            List<RemoteSessionDto> opencodeSessions,
            CodexTranscriptReader codexReader,
            OpencodeSqliteTranscriptReader opencodeReader
    ) {
        return new SessionTranscriptService(
                new SessionService(new FixedCodexAdapter(codexSessions), new FixedOpencodeAdapter(opencodeSessions)),
                codexReader,
                opencodeReader
        );
    }

    private static RemoteSessionDto session(String id, AgentSource source, String rawFile) {
        return new RemoteSessionDto(
                id,
                source,
                "Title " + id,
                "/workspace/" + id,
                "model-" + id,
                SessionStatus.UNKNOWN,
                Instant.parse("2026-06-20T00:00:00Z"),
                "Last " + id,
                rawFile
        );
    }

    private static SessionMessageDto message(String id, String role) {
        return new SessionMessageDto(id, role, "message " + id, Instant.parse("2026-06-20T00:00:00Z"));
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

    private static class FixedCodexTranscriptReader extends CodexTranscriptReader {

        private final List<SessionMessageDto> messages;
        private boolean called;
        private Path path;
        private int limit;
        private String beforeCursor;

        private FixedCodexTranscriptReader(List<SessionMessageDto> messages) {
            this.messages = messages;
        }

        @Override
        public List<SessionMessageDto> readMessages(Path jsonlFile) {
            called = true;
            path = jsonlFile;
            return messages;
        }

        @Override
        public CodexTranscriptPage readPage(Path jsonlFile, int selectedLimit, String selectedBeforeCursor) {
            called = true;
            path = jsonlFile;
            limit = selectedLimit;
            beforeCursor = selectedBeforeCursor;
            return new CodexTranscriptPage(
                    messages,
                    SessionTranscriptDto.TranscriptPageInfoDto.fromMessages(messages, false)
            );
        }
    }

    private static class FixedOpencodeTranscriptReader extends OpencodeSqliteTranscriptReader {

        private final List<SessionMessageDto> messages;
        private boolean called;
        private Path path;
        private String sessionId;
        private int limit;
        private String beforeCursor;

        private FixedOpencodeTranscriptReader(List<SessionMessageDto> messages) {
            this.messages = messages;
        }

        @Override
        public List<SessionMessageDto> readMessages(Path databasePath, String selectedSessionId) {
            called = true;
            path = databasePath;
            sessionId = selectedSessionId;
            return messages;
        }

        @Override
        public OpencodeTranscriptPage readPage(Path databasePath, String selectedSessionId, int selectedLimit, String selectedBeforeCursor) {
            called = true;
            path = databasePath;
            sessionId = selectedSessionId;
            limit = selectedLimit;
            beforeCursor = selectedBeforeCursor;
            return new OpencodeTranscriptPage(
                    messages,
                    SessionTranscriptDto.TranscriptPageInfoDto.fromMessages(messages, false)
            );
        }
    }
}
