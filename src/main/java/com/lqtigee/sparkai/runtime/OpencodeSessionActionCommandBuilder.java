package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class OpencodeSessionActionCommandBuilder {

    public CommandSpec delete(String sessionId, boolean confirmDestructive) {
        requireDestructiveConfirmation(confirmDestructive);
        return build("session", "delete", sessionId);
    }

    public CommandSpec export(String sessionId) {
        return build("export", sessionId);
    }

    private CommandSpec build(String action, String sessionId) {
        requireSessionId(sessionId);

        return new CommandSpec(
                List.of("opencode", action, sessionId),
                Path.of("."),
                Map.of(),
                AgentSource.OPENCODE,
                sessionId,
                ""
        );
    }

    private CommandSpec build(String group, String action, String sessionId) {
        requireSessionId(sessionId);

        return new CommandSpec(
                List.of("opencode", group, action, sessionId),
                Path.of("."),
                Map.of(),
                AgentSource.OPENCODE,
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
