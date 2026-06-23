package com.lqtigee.sparkai.codex;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;

public class CodexFileScanner {

    public List<Path> scan(Path codexHome) {
        Path sessionsDirectory = requireSessionsDirectory(codexHome);

        try (Stream<Path> paths = Files.walk(sessionsDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .sorted(Comparator.comparingLong(this::lastModifiedMillis).reversed())
                    .map(path -> path.toAbsolutePath().normalize())
                    .toList();
        } catch (IOException exception) {
            throw scanFailed(exception);
        }
    }

    public List<Path> findBySessionIds(Path codexHome, Set<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        Path sessionsDirectory = requireSessionsDirectory(codexHome);

        try (Stream<Path> paths = Files.walk(sessionsDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .filter(path -> fileNameMatchesAnySessionId(path, sessionIds))
                    .sorted(Comparator.comparingLong(this::lastModifiedMillis).reversed())
                    .map(path -> path.toAbsolutePath().normalize())
                    .toList();
        } catch (IOException exception) {
            throw scanFailed(exception);
        }
    }

    private Path requireSessionsDirectory(Path codexHome) {
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
        return sessionsDirectory;
    }

    private boolean fileNameMatchesAnySessionId(Path path, Set<String> sessionIds) {
        String fileName = path.getFileName().toString();
        return sessionIds.stream().anyMatch(sessionId ->
                fileName.equals(sessionId + ".jsonl") || fileName.endsWith("-" + sessionId + ".jsonl")
        );
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

    private ApiException scanFailed(IOException exception) {
        return new ApiException(
                ErrorCode.CODEX_SESSION_SCAN_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                "Codex session scan failed",
                exception.getMessage()
        );
    }
}
