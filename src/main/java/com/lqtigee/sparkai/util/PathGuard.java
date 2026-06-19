package com.lqtigee.sparkai.util;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Path;
import java.util.List;
import org.springframework.http.HttpStatus;

public class PathGuard {

    public void assertAllowed(Path path, List<Path> allowedRoots) {
        Path normalizedPath = normalize(path);
        if (normalizedPath == null || allowedRoots == null || allowedRoots.isEmpty()) {
            throw workspaceNotAllowed(path);
        }

        boolean allowed = allowedRoots.stream()
                .map(this::normalize)
                .filter(root -> root != null)
                .anyMatch(normalizedPath::startsWith);

        if (!allowed) {
            throw workspaceNotAllowed(path);
        }
    }

    private Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    private ApiException workspaceNotAllowed(Path path) {
        return new ApiException(
                ErrorCode.WORKSPACE_NOT_ALLOWED,
                HttpStatus.FORBIDDEN,
                "Workspace path is not allowed",
                path == null ? "path=null" : path.toString()
        );
    }
}
