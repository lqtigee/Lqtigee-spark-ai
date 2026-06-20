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

        assertThat(messages).hasSize(12);
        assertThat(messages).extracting(SessionMessageDto::role).containsExactly(
                "user",
                "assistant",
                "assistant",
                "user",
                "assistant",
                "user",
                "assistant",
                "user",
                "assistant",
                "user",
                "assistant",
                "user"
        );
        assertThat(messages.subList(0, 3)).extracting(SessionMessageDto::text).containsExactly(
                "Open this session as chat",
                "Here is the real chat transcript.",
                "Line number id is allowed"
        );
        assertThat(messages.subList(0, 3)).extracting(SessionMessageDto::id).containsExactly("msg-user-1", "msg-assistant-1", "line-8");
        assertThat(messages)
                .extracting(SessionMessageDto::text)
                .doesNotContain("developer message must be excluded", "system message must be excluded");
    }

    @Test
    void readPageReturnsNewestTenMessagesByDefaultWhenCursorIsAbsent() {
        CodexTranscriptReader.CodexTranscriptPage page = reader.readPage(
                Path.of("src/test/resources/samples/codex-transcript-sample.jsonl"),
                0,
                null
        );

        assertThat(page.messages()).extracting(SessionMessageDto::id).containsExactly(
                "line-8",
                "msg-user-2",
                "msg-assistant-2",
                "msg-user-3",
                "msg-assistant-3",
                "msg-user-4",
                "msg-assistant-4",
                "msg-user-5",
                "msg-assistant-5",
                "msg-user-6"
        );
        assertThat(page.pageInfo().oldestCursor()).isEqualTo("line-8");
        assertThat(page.pageInfo().newestCursor()).isEqualTo("msg-user-6");
        assertThat(page.pageInfo().hasMoreBefore()).isTrue();
    }

    @Test
    void readPageReturnsOlderMessagesBeforeCursor() {
        CodexTranscriptReader.CodexTranscriptPage page = reader.readPage(
                Path.of("src/test/resources/samples/codex-transcript-sample.jsonl"),
                10,
                "line-8"
        );

        assertThat(page.messages()).extracting(SessionMessageDto::id).containsExactly("msg-user-1", "msg-assistant-1");
        assertThat(page.pageInfo().oldestCursor()).isEqualTo("msg-user-1");
        assertThat(page.pageInfo().newestCursor()).isEqualTo("msg-assistant-1");
        assertThat(page.pageInfo().hasMoreBefore()).isFalse();
    }

    @Test
    void readPageKeepsPageSortedOldestToNewest() {
        CodexTranscriptReader.CodexTranscriptPage page = reader.readPage(
                Path.of("src/test/resources/samples/codex-transcript-sample.jsonl"),
                3,
                "msg-user-4"
        );

        assertThat(page.messages()).extracting(SessionMessageDto::id).containsExactly(
                "msg-assistant-2",
                "msg-user-3",
                "msg-assistant-3"
        );
        assertThat(page.pageInfo().oldestCursor()).isEqualTo("msg-assistant-2");
        assertThat(page.pageInfo().newestCursor()).isEqualTo("msg-assistant-3");
        assertThat(page.pageInfo().hasMoreBefore()).isTrue();
    }

    @Test
    void readPageRejectsUnknownBeforeCursor() {
        assertThatThrownBy(() -> reader.readPage(
                Path.of("src/test/resources/samples/codex-transcript-sample.jsonl"),
                10,
                "missing-cursor"
        ))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
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
