package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodexSessionActionCommandBuilderTest {

    private static final String SESSION_ID = "018f6e54-7b1c-7000-8000-000000000001";

    private final CodexSessionActionCommandBuilder builder = new CodexSessionActionCommandBuilder();

    @Test
    void archiveBuildsArgumentArray() {
        CommandSpec spec = builder.archive(SESSION_ID);

        assertThat(spec.command()).containsExactly("codex", "archive", SESSION_ID);
        assertThat(spec.source()).isEqualTo(AgentSource.CODEX);
        assertThat(spec.sessionId()).isEqualTo(SESSION_ID);
        assertNoShellString(spec.command());
    }

    @Test
    void unarchiveBuildsArgumentArray() {
        CommandSpec spec = builder.unarchive(SESSION_ID);

        assertThat(spec.command()).containsExactly("codex", "unarchive", SESSION_ID);
        assertThat(spec.source()).isEqualTo(AgentSource.CODEX);
        assertThat(spec.sessionId()).isEqualTo(SESSION_ID);
        assertNoShellString(spec.command());
    }

    @Test
    void archiveRejectsBlankSessionId() {
        assertThatThrownBy(() -> builder.archive(" "))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void unarchiveRejectsBlankSessionId() {
        assertThatThrownBy(() -> builder.unarchive(""))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    private void assertNoShellString(List<String> command) {
        assertThat(command).doesNotContain("sh", "bash", "-c");
    }
}
