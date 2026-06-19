package com.lqtigee.sparkai.opencode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpencodeFileScannerTest {

    @TempDir
    private Path tempDir;

    @Test
    void scanFailsWhenAllRootsAreMissing() {
        OpencodeFileScanner scanner = new OpencodeFileScanner();
        List<Path> missingRoots = List.of(
                tempDir.resolve("missing-config"),
                tempDir.resolve("missing-share")
        );

        assertThatThrownBy(() -> scanner.scan(missingRoots))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.OPENCODE_CONFIG_NOT_FOUND));
    }

    @Test
    void scanReturnsOnlyOpencodeDatabaseFiles() throws IOException {
        OpencodeFileScanner scanner = new OpencodeFileScanner();
        Path configRoot = Files.createDirectories(tempDir.resolve("config"));
        Path shareRoot = Files.createDirectories(tempDir.resolve("share"));
        Path stateRoot = Files.createDirectories(tempDir.resolve("state"));
        Path expected = Files.createFile(shareRoot.resolve("opencode.db"));

        Files.createFile(configRoot.resolve("opencode.json"));
        Files.createFile(shareRoot.resolve("session.json"));
        Files.createFile(shareRoot.resolve("session.jsonl"));
        Files.createFile(shareRoot.resolve("opencode.log"));
        Files.createFile(shareRoot.resolve("session.txt"));
        Files.createFile(shareRoot.resolve("opencode.sqlite"));
        Files.createFile(stateRoot.resolve("prompt-history.jsonl"));

        assertThat(scanner.scan(List.of(configRoot, shareRoot, stateRoot)))
                .containsExactly(expected.toAbsolutePath().normalize());
    }
}
