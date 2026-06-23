package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.config.RemoteProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.AttachmentDto;
import com.lqtigee.sparkai.dto.CodexRunOptionsDto;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.service.AttachmentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CodexCommandBuilderTest {

    private static final String SESSION_ID = "018f6e54-7b1c-7000-8000-000000000001";
    private static final String WORKSPACE = "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai";
    private static final String PROMPT = "explain status && keep this as one argument";
    private static final String MISSING_ATTACHMENT_ID = "att_00000000000000000000000000000002";

    private final Path testRoot = Path.of("/home/lqtiger/.lqtigee-spark-ai/test-codex-attachments-" + UUID.randomUUID())
            .toAbsolutePath()
            .normalize();
    private final AttachmentService attachmentService = new AttachmentService(testProperties(testRoot.resolve("attachments")));
    private final CodexCommandBuilder builder = new CodexCommandBuilder(attachmentService);

    @AfterEach
    void removeTestRoot() throws IOException {
        if (!Files.exists(testRoot)) {
            return;
        }
        try (var paths = Files.walk(testRoot)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException("Failed to delete test attachment path", exception);
                        }
                    });
        }
    }

    @Test
    void buildUsesReadOnlySandboxForAsk() {
        CommandSpec spec = builder.build(request(CommandMode.ASK, false), session(), model());

        assertThat(spec.command())
                .contains("--json", "--skip-git-repo-check")
                .containsSubsequence("-m", "gpt-5.5")
                .containsSubsequence("-s", "read-only", "exec", "resume");
        assertCommandOrder(spec.command());
        assertPromptIsSingleArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildUsesReadOnlySandboxForReview() {
        CommandSpec spec = builder.build(request(CommandMode.REVIEW, false), session(), model());

        assertThat(spec.command())
                .contains("--json", "--skip-git-repo-check")
                .containsSubsequence("-m", "gpt-5.5")
                .containsSubsequence("-s", "read-only", "exec", "resume");
        assertCommandOrder(spec.command());
        assertPromptIsSingleArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildUsesWorkspaceWriteSandboxForEdit() {
        CommandSpec spec = builder.build(request(CommandMode.EDIT, false), session(), model());

        assertThat(spec.command())
                .contains("--json", "--skip-git-repo-check")
                .containsSubsequence("-m", "gpt-5.5")
                .containsSubsequence("-s", "workspace-write", "exec", "resume");
        assertCommandOrder(spec.command());
        assertPromptIsSingleArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildFailsWithDangerConfirmRequiredForShellWithoutConfirmation() {
        assertThatThrownBy(() -> builder.build(request(CommandMode.SHELL, false), session(), model()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.DANGER_CONFIRM_REQUIRED));
    }

    @Test
    void buildFailsForShellEvenWithConfirmationUntilSupportedPathExists() {
        assertThatThrownBy(() -> builder.build(request(CommandMode.SHELL, true), session(), model()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.DANGER_CONFIRM_REQUIRED));
    }

    @Test
    void buildMapsImageAttachmentIdsToImageArgsBeforeSessionId() {
        AttachmentDto image = uploadAttachment("image.png", "image/png");
        Path imagePath = testRoot.resolve("attachments").resolve(image.id());

        CommandSpec spec = builder.build(requestWithImages(List.of(image.id())), session(), model());

        assertThat(spec.command())
                .containsSubsequence("--image", imagePath.toString(), "--skip-git-repo-check");
        assertThat(spec.command().indexOf("--image")).isLessThan(spec.command().indexOf(SESSION_ID));
        assertPromptIsSingleArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildMapsCodexConfigOverridesBeforeExecResume() {
        CommandSpec spec = builder.build(requestWithReasoningEffort("xhigh"), session(), model());

        assertThat(spec.command())
                .containsSubsequence("-s", "read-only", "-c", "model_reasoning_effort=\"xhigh\"", "exec", "resume");
        assertThat(spec.command().indexOf("-c")).isLessThan(spec.command().indexOf("exec"));
        assertPromptIsSingleArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildRejectsMissingImageAttachmentId() {
        assertThatThrownBy(() -> builder.build(requestWithImages(List.of(MISSING_ATTACHMENT_ID)), session(), model()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.ATTACHMENT_NOT_FOUND));
    }

    @Test
    void buildRejectsRawImageAttachmentPath() {
        assertThatThrownBy(() -> builder.build(requestWithImages(List.of("/tmp/image.png")), session(), model()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.ATTACHMENT_NOT_FOUND));
    }

    @Test
    void buildRejectsNonImageAttachmentId() {
        AttachmentDto text = uploadAttachment("context.txt", "text/plain");

        assertThatThrownBy(() -> builder.build(requestWithImages(List.of(text.id())), session(), model()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.ATTACHMENT_CONTENT_TYPE_FORBIDDEN));
    }

    private void assertCommandOrder(List<String> command) {
        assertThat(command.indexOf("-C")).isLessThan(command.indexOf("exec"));
        assertThat(command.indexOf("resume")).isGreaterThan(command.indexOf("exec"));
        assertThat(command.indexOf(SESSION_ID)).isLessThan(command.indexOf(PROMPT));
    }

    private void assertPromptIsSingleArgument(List<String> command) {
        assertThat(command)
                .filteredOn(PROMPT::equals)
                .hasSize(1);
    }

    private void assertNoShellString(List<String> command) {
        assertThat(command).doesNotContain("sh", "bash");
    }

    private StartRunRequest request(CommandMode mode, boolean confirmDangerous) {
        return new StartRunRequest(
                SESSION_ID,
                AgentSource.CODEX,
                "gpt-5.5",
                mode,
                PROMPT,
                confirmDangerous
        );
    }

    private StartRunRequest requestWithImages(List<String> attachmentIds) {
        return new StartRunRequest(
                SESSION_ID,
                AgentSource.CODEX,
                "gpt-5.5",
                CommandMode.ASK,
                PROMPT,
                false,
                new CodexRunOptionsDto(
                        attachmentIds,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                null
        );
    }

    private StartRunRequest requestWithReasoningEffort(String effort) {
        return new StartRunRequest(
                SESSION_ID,
                AgentSource.CODEX,
                "gpt-5.5",
                CommandMode.ASK,
                PROMPT,
                false,
                new CodexRunOptionsDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(new CodexRunOptionsDto.ConfigOverrideDto("model_reasoning_effort", effort)),
                        null
                ),
                null
        );
    }

    private AttachmentDto uploadAttachment(String filename, String contentType) {
        return attachmentService.upload(new MockMultipartFile(
                "file",
                filename,
                contentType,
                "content".getBytes()
        ));
    }

    private RemoteSessionDto session() {
        return new RemoteSessionDto(
                SESSION_ID,
                AgentSource.CODEX,
                "Lqtigee project",
                WORKSPACE,
                "gpt-5.5",
                SessionStatus.IDLE,
                Instant.parse("2026-06-20T00:00:00Z"),
                null,
                "/home/lqtiger/.codex/sessions/sample.jsonl"
        );
    }

    private ModelDto model() {
        return new ModelDto(
                "gpt-5.5",
                "GPT-5.5",
                "gpt-5.5",
                List.of(AgentSource.CODEX),
                true
        );
    }

    private static RemoteProperties testProperties(Path attachmentRoot) {
        RemoteProperties properties = new RemoteProperties();
        properties.setMaxPromptChars(8000);
        properties.setAttachmentRoot(attachmentRoot.toString());
        return properties;
    }
}
