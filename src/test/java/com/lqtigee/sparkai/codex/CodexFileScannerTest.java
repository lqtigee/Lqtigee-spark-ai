package com.lqtigee.sparkai.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexFileScannerTest {

    @TempDir
    private Path tempDir;

    @Test
    void scanFailsWhenCodexHomeDoesNotExist() {
        CodexFileScanner scanner = new CodexFileScanner();
        Path missingCodexHome = tempDir.resolve("missing-codex-home");

        assertThatThrownBy(() -> scanner.scan(missingCodexHome))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.CODEX_HOME_NOT_FOUND));
    }

    @Test
    void scanReturnsOnlyJsonlFilesUnderSessions() throws IOException {
        CodexFileScanner scanner = new CodexFileScanner();
        Path codexHome = tempDir.resolve("codex-home");
        Path sessions = Files.createDirectories(codexHome.resolve("sessions"));
        Path nestedSessions = Files.createDirectories(sessions.resolve("2026/06/20"));
        Path expected = Files.createFile(nestedSessions.resolve("session.jsonl"));

        Files.createFile(codexHome.resolve("outside.jsonl"));
        Files.createFile(sessions.resolve("session.log"));
        Files.createFile(sessions.resolve("session.txt"));
        Files.createFile(sessions.resolve("session.md"));
        Files.createFile(sessions.resolve("session.db"));
        Files.createFile(sessions.resolve("session.json"));

        assertThat(scanner.scan(codexHome))
                .containsExactly(expected.toAbsolutePath().normalize());
    }

    @Test
    void findBySessionIdsReturnsOnlyMatchingJsonlFiles() throws IOException {
        CodexFileScanner scanner = new CodexFileScanner();
        Path codexHome = tempDir.resolve("codex-home");
        Path sessions = Files.createDirectories(codexHome.resolve("sessions/2026/06/24"));
        Path expected = Files.createFile(sessions.resolve("rollout-2026-06-24T00-00-00-session-target.jsonl"));
        Files.createFile(sessions.resolve("rollout-2026-06-24T00-00-00-session-other.jsonl"));

        assertThat(scanner.findBySessionIds(codexHome, Set.of("session-target")))
                .containsExactly(expected.toAbsolutePath().normalize());
    }
}
