package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class CodexSessionActionCommandBuilder {

    public CommandSpec archive(String sessionId) {
        return build("archive", sessionId);
    }

    public CommandSpec unarchive(String sessionId) {
        return build("unarchive", sessionId);
    }

    public CommandSpec delete(String sessionId, boolean confirmDestructive) {
        requireDestructiveConfirmation(confirmDestructive);
        return build("delete", sessionId);
    }

    private CommandSpec build(String action, String sessionId) {
        requireSessionId(sessionId);

        return new CommandSpec(
                List.of("codex", action, sessionId),
                Path.of("."),
                Map.of(),
                AgentSource.CODEX,
                sessionId,
                ""
        );
    }

    private void requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "Session id is required",
                    "sessionId"
            );
        }
    }

    private void requireDestructiveConfirmation(boolean confirmDestructive) {
        if (!confirmDestructive) {
            throw new ApiException(
                    ErrorCode.DANGER_CONFIRM_REQUIRED,
                    HttpStatus.BAD_REQUEST,
                    "Destructive session action requires explicit confirmation",
                    "confirmDestructive"
            );
        }
    }
}
