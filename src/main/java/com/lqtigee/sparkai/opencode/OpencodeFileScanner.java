package com.lqtigee.sparkai.opencode;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;

public class OpencodeFileScanner {

    public List<Path> scan(List<Path> roots) {
        if (roots == null || roots.stream().noneMatch(path -> path != null && Files.exists(path))) {
            throw new ApiException(
                    ErrorCode.OPENCODE_CONFIG_NOT_FOUND,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode roots not found",
                    roots == null ? "roots=null" : roots.toString()
            );
        }

        try {
            return roots.stream()
                    .filter(path -> path != null && Files.exists(path))
                    .flatMap(this::walkRoot)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("opencode.db"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(path -> path.toAbsolutePath().normalize())
                    .toList();
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode session scan failed",
                    exception.getMessage()
            );
        }
    }

    private Stream<Path> walkRoot(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode session scan failed",
                    exception.getMessage()
            );
        }
    }
}
