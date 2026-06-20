package com.lqtigee.sparkai.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionTranscriptDtoTest {

    @Test
    void constructorAddsPageInfoFromMessageIds() {
        SessionTranscriptDto transcript = new SessionTranscriptDto(
                session(),
                List.of(
                        message("line-3"),
                        message("line-5")
                )
        );

        assertThat(transcript.pageInfo().oldestCursor()).isEqualTo("line-3");
        assertThat(transcript.pageInfo().newestCursor()).isEqualTo("line-5");
        assertThat(transcript.pageInfo().hasMoreBefore()).isFalse();
    }

    @Test
    void emptyMessagesUseNullCursors() {
        SessionTranscriptDto transcript = new SessionTranscriptDto(
                session(),
                List.of(),
                new SessionTranscriptDto.TranscriptPageInfoDto(null, null, false)
        );

        assertThat(transcript.pageInfo().oldestCursor()).isNull();
        assertThat(transcript.pageInfo().newestCursor()).isNull();
        assertThat(transcript.pageInfo().hasMoreBefore()).isFalse();
    }

    @Test
    void explicitPageInfoPreservesHasMoreBefore() {
        SessionTranscriptDto transcript = new SessionTranscriptDto(
                session(),
                List.of(message("line-8"), message("line-12")),
                new SessionTranscriptDto.TranscriptPageInfoDto("line-8", "line-12", true)
        );

        assertThat(transcript.pageInfo().oldestCursor()).isEqualTo("line-8");
        assertThat(transcript.pageInfo().newestCursor()).isEqualTo("line-12");
        assertThat(transcript.pageInfo().hasMoreBefore()).isTrue();
    }

    private static RemoteSessionDto session() {
        return new RemoteSessionDto(
                "session-id",
                AgentSource.CODEX,
                "Session title",
                "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
                "gpt-5.5",
                SessionStatus.UNKNOWN,
                Instant.parse("2026-06-20T00:00:00Z"),
                "",
                "/home/lqtiger/.codex/sessions/session.jsonl"
        );
    }

    private static SessionMessageDto message(String id) {
        return new SessionMessageDto(
                id,
                "user",
                "message " + id,
                Instant.parse("2026-06-20T00:00:00Z")
        );
    }
}
