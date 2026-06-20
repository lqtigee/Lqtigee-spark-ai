package com.lqtigee.sparkai.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathGuardTest {

    @TempDir
    private Path tempDir;

    private final PathGuard pathGuard = new PathGuard();

    @Test
    void assertAllowedPassesWhenPathIsInsideRoot() throws IOException {
        Path root = tempDir.resolve("workspace");
        Path insidePath = root.resolve("project");
        Files.createDirectories(insidePath);

        assertThatCode(() -> pathGuard.assertAllowed(insidePath, List.of(root)))
                .doesNotThrowAnyException();
    }

    @Test
    void assertAllowedFailsWithWorkspaceNotAllowedWhenPathIsOutsideRoot() throws IOException {
        Path root = tempDir.resolve("workspace");
        Path outsidePath = tempDir.resolve("outside/project");
        Files.createDirectories(root);
        Files.createDirectories(outsidePath);

        assertThatThrownBy(() -> pathGuard.assertAllowed(outsidePath, List.of(root)))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.WORKSPACE_NOT_ALLOWED));
    }
}
