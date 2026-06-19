package com.lqtigee.sparkai.codex;

import static org.assertj.core.api.Assertions.assertThat;

import com.lqtigee.sparkai.dto.SessionFileFormat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexFormatDetectorTest {

    @TempDir
    private Path tempDir;

    @Test
    void detectReturnsJsonWhenFileStartsWithJsonObject() throws IOException {
        CodexFormatDetector detector = new CodexFormatDetector();
        Path sessionFile = tempDir.resolve("session.jsonl");
        Files.writeString(sessionFile, "{\"id\":\"session-1\"}");

        assertThat(detector.detect(sessionFile)).isEqualTo(SessionFileFormat.JSON);
    }

    @Test
    void detectReturnsJsonlWhenFileContainsJsonObjectLines() throws IOException {
        CodexFormatDetector detector = new CodexFormatDetector();
        Path sessionFile = tempDir.resolve("session.jsonl");
        Files.writeString(sessionFile, "{\"id\":\"session-1\"}\n{\"id\":\"session-2\"}\n");

        assertThat(detector.detect(sessionFile)).isEqualTo(SessionFileFormat.JSONL);
    }
}
