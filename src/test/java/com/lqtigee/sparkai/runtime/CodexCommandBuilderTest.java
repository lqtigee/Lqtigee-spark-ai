package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodexCommandBuilderTest {

    private static final String SESSION_ID = "018f6e54-7b1c-7000-8000-000000000001";
    private static final String WORKSPACE = "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai";
    private static final String PROMPT = "explain status && keep this as one argument";

    private final CodexCommandBuilder builder = new CodexCommandBuilder();

    @Test
    void buildAddsReadOnlySandboxForAsk() {
        CommandSpec spec = builder.build(request(CommandMode.ASK, false), session(), model());

        assertThat(spec.command())
                .containsSubsequence("-s", "read-only")
                .contains("--json", "--skip-git-repo-check");
        assertCommandOrder(spec.command());
        assertPromptIsSingleArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildAddsWorkspaceWriteSandboxForEdit() {
        CommandSpec spec = builder.build(request(CommandMode.EDIT, false), session(), model());

        assertThat(spec.command())
                .containsSubsequence("-s", "workspace-write")
                .doesNotContain("--dangerously-bypass-approvals-and-sandbox");
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
        assertThat(command).doesNotContain("sh", "bash", "-c");
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
}
