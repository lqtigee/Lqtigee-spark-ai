package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class OpencodeCommandBuilder {

    private static final Path STATIC_EVIDENCE_PATH = Path.of("doc/discovery/opencode-session-static-evidence.md");

    public CommandSpec build(StartRunRequest request, RemoteSessionDto session, ModelDto model) {
        requireStaticEvidence();

        List<String> command = new ArrayList<>();
        command.add("opencode");
        command.add("run");
        command.add("--format");
        command.add("json");
        command.add("--model");
        command.add(model.commandModelName());
        command.add("--dir");
        command.add(session.workspace());
        command.add("--session");
        command.add(session.id());
        addPermissionArgs(command, request);
        command.add(request.prompt());

        return new CommandSpec(
                List.copyOf(command),
                Path.of(session.workspace()),
                Map.of(),
                AgentSource.OPENCODE,
                session.id(),
                model.id()
        );
    }

    private void addPermissionArgs(List<String> command, StartRunRequest request) {
        CommandMode mode = request.mode();
        if (mode == CommandMode.ASK || mode == CommandMode.REVIEW || mode == CommandMode.EDIT) {
            return;
        }
        if (mode == CommandMode.SHELL && request.confirmDangerous()) {
            command.add("--dangerously-skip-permissions");
            return;
        }
        throw new ApiException(
                ErrorCode.DANGER_CONFIRM_REQUIRED,
                HttpStatus.BAD_REQUEST,
                "Dangerous command mode requires explicit confirmation",
                "mode=SHELL"
        );
    }

    private void requireStaticEvidence() {
        try {
            String evidence = Files.readString(STATIC_EVIDENCE_PATH);
            if (evidence.contains("BLOCKED")) {
                throw new IllegalStateException("opencode session static evidence is blocked");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("opencode session static evidence is unavailable", exception);
        }
    }
}
