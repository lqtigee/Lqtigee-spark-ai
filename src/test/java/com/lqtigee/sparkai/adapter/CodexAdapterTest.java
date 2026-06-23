package com.lqtigee.sparkai.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.codex.CodexFileScanner;
import com.lqtigee.sparkai.codex.CodexJsonlParser;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexAdapterTest {

    @TempDir
    private Path tempDir;

    @Test
    void discoverSessionsParsesScannerOutputInOrder() throws IOException {
        Path first = writeCodexJsonl("first.jsonl", "session-first", "/workspace/first", "gpt-5-codex");
        Path second = writeCodexJsonl("second.jsonl", "session-second", "/workspace/second", "gpt-5");
        CodexAdapter adapter = new CodexAdapter(
                new FixedScanner(List.of(first, second)),
                new CodexJsonlParser()
        );

        var sessions = adapter.discoverSessions();

        assertThat(sessions).hasSize(2);
        assertThat(sessions)
                .extracting(session -> session.id())
                .containsExactly("session-first", "session-second");
        assertThat(sessions)
                .extracting(session -> session.source())
                .containsOnly(AgentSource.CODEX);
        assertThat(sessions)
                .extracting(session -> session.model())
                .containsExactly("gpt-5-codex", "gpt-5");
    }

    @Test
    void discoverSessionsPropagatesParserFailure() throws IOException {
        Path invalid = tempDir.resolve("invalid.jsonl");
        Files.writeString(invalid, """
                {"timestamp":"2026-06-20T00:00:00Z","type":"session_meta","payload":{"id":"broken","cwd":"/workspace/broken"}}
                """);
        CodexAdapter adapter = new CodexAdapter(
                new FixedScanner(List.of(invalid)),
                new CodexJsonlParser()
        );

        assertThatThrownBy(adapter::discoverSessions)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.CODEX_SESSION_FIELD_MISSING));
    }

    @Test
    void discoverSessionsByIdsUsesScannerIdFilter() throws IOException {
        Path first = writeCodexJsonl("first.jsonl", "session-first", "/workspace/first", "gpt-5-codex");
        CodexAdapter adapter = new CodexAdapter(
                new FixedScanner(List.of(first)),
                new CodexJsonlParser()
        );

        var sessions = adapter.discoverSessionsByIds(Set.of("session-first"));

        assertThat(sessions)
                .extracting(session -> session.id())
                .containsExactly("session-first");
    }

    private Path writeCodexJsonl(String fileName, String id, String workspace, String model) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, """
                {"timestamp":"2026-06-20T00:00:00Z","type":"session_meta","payload":{"id":"%s","cwd":"%s"}}
                {"timestamp":"2026-06-20T00:01:00Z","type":"turn_context","payload":{"cwd":"%s","model":"%s"}}
                """.formatted(id, workspace, workspace, model));
        return file;
    }

    private static class FixedScanner extends CodexFileScanner {

        private final List<Path> paths;

        private FixedScanner(List<Path> paths) {
            this.paths = paths;
        }

        @Override
        public List<Path> scan(Path codexHome) {
            return paths;
        }

        @Override
        public List<Path> findBySessionIds(Path codexHome, Set<String> sessionIds) {
            return paths;
        }
    }
}
