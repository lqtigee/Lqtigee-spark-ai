package com.lqtigee.sparkai.codex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CodexJsonlParserTest {

    @TempDir
    private Path tempDir;

    @Test
    void parseReturnsRequiredFieldsFromSanitizedSample() {
        CodexJsonlParser parser = new CodexJsonlParser();
        Path sample = Path.of("src/test/resources/samples/codex-session-sample.jsonl");

        RemoteSessionDto session = parser.parse(sample);

        assertThat(session.id()).isEqualTo("<uuid>");
        assertThat(session.workspace()).isEqualTo("<path>");
        assertThat(session.model()).isEqualTo("<model>");
        assertThat(session.updatedAt()).isNotNull();
        assertThat(session.rawFile()).isEqualTo(sample.toAbsolutePath().normalize().toString());
        assertThat(session.source()).isEqualTo(AgentSource.CODEX);
        assertThat(session.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.title()).isEqualTo("Build the phone session chat view");
        assertThat(session.lastMessage()).isEqualTo("I will wire it to real transcript data.");
        assertThat(session.title()).doesNotContain("environment_context");
    }

    @Test
    void parseFailsWhenRequiredFieldIsMissing() throws IOException {
        CodexJsonlParser parser = new CodexJsonlParser();
        Path sessionFile = tempDir.resolve("missing-model.jsonl");
        Files.writeString(
                sessionFile,
                """
                {"type":"session_meta","timestamp":"2000-01-01T00:00:00Z","payload":{"id":"<uuid>","cwd":"<path>"}}
                {"type":"turn_context","timestamp":"2000-01-01T00:01:00Z","payload":{"cwd":"<path>"}}
                """
        );

        assertThatThrownBy(() -> parser.parse(sessionFile))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.CODEX_SESSION_FIELD_MISSING));
    }
}
