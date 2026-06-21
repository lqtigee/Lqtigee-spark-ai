package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpencodeSessionActionCommandBuilderTest {

    private static final String SESSION_ID = "ses_121488be4ffeSI5wIkwYvHniqr";

    private final OpencodeSessionActionCommandBuilder builder = new OpencodeSessionActionCommandBuilder();

    @Test
    void deleteBuildsArgumentArrayWhenConfirmed() {
        CommandSpec spec = builder.delete(SESSION_ID, true);

        assertThat(spec.command()).containsExactly("opencode", "session", "delete", SESSION_ID);
        assertThat(spec.source()).isEqualTo(AgentSource.OPENCODE);
        assertThat(spec.sessionId()).isEqualTo(SESSION_ID);
        assertNoShellString(spec.command());
    }

    @Test
    void deleteRequiresConfirmation() {
        assertThatThrownBy(() -> builder.delete(SESSION_ID, false))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.DANGER_CONFIRM_REQUIRED));
    }

    @Test
    void deleteRejectsBlankSessionIdWhenConfirmed() {
        assertThatThrownBy(() -> builder.delete("", true))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    private void assertNoShellString(List<String> command) {
        assertThat(command).doesNotContain("sh", "bash", "-c");
    }
}
