package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.OpencodeRunOptionsDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpencodeCommandBuilderTest {

    private static final String SESSION_ID = "ses_01JYK3T8R2A8H3X9M6Q4N5P7Z1";
    private static final String WORKSPACE = "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai";
    private static final String PROMPT = "review state && keep this as one argument";

    private final OpencodeCommandBuilder builder = new OpencodeCommandBuilder();

    @Test
    void buildDoesNotAddDangerousFlagForAsk() {
        CommandSpec spec = builder.build(request(CommandMode.ASK, false), session(), model());

        assertBaseCommand(spec.command());
        assertThat(spec.command()).doesNotContain("--dangerously-skip-permissions", "--continue");
        assertPromptIsSingleArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildDoesNotAddDangerousFlagForEdit() {
        CommandSpec spec = builder.build(request(CommandMode.EDIT, false), session(), model());

        assertBaseCommand(spec.command());
        assertThat(spec.command()).doesNotContain("--dangerously-skip-permissions", "--continue");
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
    void buildAddsDangerousFlagForShellWithConfirmation() {
        CommandSpec spec = builder.build(request(CommandMode.SHELL, true), session(), model());

        assertBaseCommand(spec.command());
        assertThat(spec.command())
                .contains("--dangerously-skip-permissions")
                .doesNotContain("--continue");
        assertThat(spec.command().indexOf("--dangerously-skip-permissions"))
                .isLessThan(spec.command().indexOf(PROMPT));
        assertPromptIsSingleArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildMapsSupportedOpencodeRuntimeOptionsBeforePrompt() {
        CommandSpec spec = builder.build(request(
                CommandMode.ASK,
                false,
                new OpencodeRunOptionsDto(
                        "build",
                        true,
                        true,
                        "high",
                        true,
                        true,
                        10,
                        null,
                        null
                )
        ), session(), model());

        assertBaseCommand(spec.command());
        assertThat(spec.command())
                .containsSubsequence("--agent", "build")
                .contains("--fork", "--share")
                .containsSubsequence("--variant", "high")
                .contains("--thinking", "--replay")
                .containsSubsequence("--replay-limit", "10")
                .doesNotContain("--no-replay", "--continue", "--file");
        assertThat(spec.command().indexOf("--agent")).isLessThan(spec.command().indexOf(PROMPT));
        assertThat(spec.command().indexOf("--replay-limit")).isLessThan(spec.command().indexOf(PROMPT));
        assertPromptIsSingleArgument(spec.command());
        assertPromptIsLastArgument(spec.command());
        assertNoShellString(spec.command());
    }

    @Test
    void buildMapsReplayFalseToNoReplayWhenConfirmedByLocalHelp() {
        CommandSpec spec = builder.build(request(
                CommandMode.ASK,
                false,
                new OpencodeRunOptionsDto(
                        null,
                        false,
                        false,
                        null,
                        false,
                        false,
                        null,
                        null,
                        null
                )
        ), session(), model());

        assertThat(spec.command())
                .contains("--no-replay")
                .doesNotContain("--replay", "--fork", "--share", "--thinking");
        assertPromptIsSingleArgument(spec.command());
        assertPromptIsLastArgument(spec.command());
        assertNoShellString(spec.command());
    }

    private void assertBaseCommand(List<String> command) {
        assertThat(command)
                .containsSubsequence("opencode", "run")
                .containsSubsequence("--format", "json")
                .containsSubsequence("--model", "opencode/lqtigee")
                .containsSubsequence("--dir", WORKSPACE)
                .containsSubsequence("--session", SESSION_ID);
        assertThat(command.get(command.indexOf("--session") + 1)).isEqualTo(SESSION_ID);
    }

    private void assertPromptIsSingleArgument(List<String> command) {
        assertThat(command)
                .filteredOn(PROMPT::equals)
                .hasSize(1);
    }

    private void assertPromptIsLastArgument(List<String> command) {
        assertThat(command.getLast()).isEqualTo(PROMPT);
    }

    private void assertNoShellString(List<String> command) {
        assertThat(command).doesNotContain("sh", "bash", "-c");
    }

    private StartRunRequest request(CommandMode mode, boolean confirmDangerous) {
        return request(mode, confirmDangerous, null);
    }

    private StartRunRequest request(CommandMode mode, boolean confirmDangerous, OpencodeRunOptionsDto opencodeOptions) {
        return new StartRunRequest(
                SESSION_ID,
                AgentSource.OPENCODE,
                "openai/Lqtigee",
                mode,
                PROMPT,
                confirmDangerous,
                null,
                opencodeOptions
        );
    }

    private RemoteSessionDto session() {
        return new RemoteSessionDto(
                SESSION_ID,
                AgentSource.OPENCODE,
                "Lqtigee project",
                WORKSPACE,
                "openai/Lqtigee",
                SessionStatus.IDLE,
                Instant.parse("2026-06-20T00:00:00Z"),
                null,
                "/home/lqtiger/.local/share/opencode/opencode.db"
        );
    }

    private ModelDto model() {
        return new ModelDto(
                "openai/Lqtigee",
                "Lqtigee",
                "opencode/lqtigee",
                List.of(AgentSource.OPENCODE),
                true
        );
    }
}
