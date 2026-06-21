package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.service.AttachmentService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class CodexCommandBuilder {

    private static final Path STATIC_EVIDENCE_PATH = Path.of("doc/discovery/codex-resume-static-evidence.md");

    private final AttachmentService attachmentService;

    public CodexCommandBuilder(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    public CommandSpec build(StartRunRequest request, RemoteSessionDto session, ModelDto model) {
        requireStaticEvidence();

        List<String> command = new ArrayList<>();
        command.add("codex");
        command.add("-C");
        command.add(session.workspace());
        addPermissionArgs(command, request);
        command.add("exec");
        command.add("resume");
        command.add("--json");
        command.add("-m");
        command.add(model.commandModelName());
        addImageAttachments(command, request);
        command.add("--skip-git-repo-check");
        command.add(session.id());
        command.add(request.prompt());

        return new CommandSpec(
                List.copyOf(command),
                Path.of(session.workspace()),
                Map.of(),
                AgentSource.CODEX,
                session.id(),
                model.id()
        );
    }

    private void addImageAttachments(List<String> command, StartRunRequest request) {
        if (request.codexOptions() == null || request.codexOptions().imageAttachmentIds() == null) {
            return;
        }
        for (String attachmentId : request.codexOptions().imageAttachmentIds()) {
            AttachmentService.ResolvedAttachment attachment = attachmentService.requireImageAttachment(attachmentId);
            command.add("--image");
            command.add(attachment.path().toString());
        }
    }

    private void addPermissionArgs(List<String> command, StartRunRequest request) {
        CommandMode mode = request.mode();
        if (mode == CommandMode.ASK || mode == CommandMode.REVIEW) {
            command.add("-s");
            command.add("read-only");
            return;
        }
        if (mode == CommandMode.EDIT) {
            command.add("-s");
            command.add("workspace-write");
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
                throw new IllegalStateException("Codex resume static evidence is blocked");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Codex resume static evidence is unavailable", exception);
        }
    }
}
