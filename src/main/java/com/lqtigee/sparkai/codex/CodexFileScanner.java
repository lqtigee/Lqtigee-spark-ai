package com.lqtigee.sparkai.codex;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;

public class CodexFileScanner {

    public List<Path> scan(Path codexHome) {
        if (codexHome == null || !Files.exists(codexHome)) {
            throw new ApiException(
                    ErrorCode.CODEX_HOME_NOT_FOUND,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex home not found",
                    codexHome == null ? "codexHome=null" : codexHome.toString()
            );
        }

        Path sessionsDirectory = codexHome.resolve("sessions");
        if (!Files.exists(sessionsDirectory)) {
            throw new ApiException(
                    ErrorCode.CODEX_HOME_NOT_FOUND,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex sessions directory not found",
                    sessionsDirectory.toString()
            );
        }

        try (Stream<Path> paths = Files.walk(sessionsDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .sorted(Comparator.comparingLong(this::lastModifiedMillis).reversed())
                    .map(path -> path.toAbsolutePath().normalize())
                    .toList();
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex session scan failed",
                    exception.getMessage()
            );
        }
    }

    private long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex session scan failed",
                    exception.getMessage()
            );
        }
    }
}
