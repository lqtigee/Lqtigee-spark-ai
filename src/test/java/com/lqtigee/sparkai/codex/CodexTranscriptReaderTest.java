package com.lqtigee.sparkai.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.SessionMessageDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexTranscriptReaderTest {

    @TempDir
    private Path tempDir;

    private final CodexTranscriptReader reader = new CodexTranscriptReader();

    @Test
    void readMessagesReturnsOnlyVisibleUserAndAssistantText() {
        List<SessionMessageDto> messages = reader.readMessages(Path.of("src/test/resources/samples/codex-transcript-sample.jsonl"));

        assertThat(messages).extracting(SessionMessageDto::role).containsExactly("user", "assistant", "assistant");
        assertThat(messages).extracting(SessionMessageDto::text).containsExactly(
                "Open this session as chat",
                "Here is the real chat transcript.",
                "Line number id is allowed"
        );
        assertThat(messages).extracting(SessionMessageDto::id).containsExactly("msg-user-1", "msg-assistant-1", "line-8");
        assertThat(messages)
                .extracting(SessionMessageDto::text)
                .doesNotContain("developer message must be excluded", "system message must be excluded");
    }

    @Test
    void readMessagesRejectsInvalidTimestamp() throws IOException {
        Path sessionFile = tempDir.resolve("invalid-timestamp.jsonl");
        Files.writeString(
                sessionFile,
                """
                {"type":"response_item","timestamp":"not-time","payload":{"type":"message","role":"user","content":[{"text":"hello"}]}}
                """
        );

        assertThatThrownBy(() -> reader.readMessages(sessionFile))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.CODEX_SESSION_FORMAT_UNKNOWN));
    }
}
