package com.lqtigee.sparkai.opencode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.SessionMessageDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpencodeSqliteTranscriptReaderTest {

    @TempDir
    private Path tempDir;

    private final OpencodeSqliteTranscriptReader reader = new OpencodeSqliteTranscriptReader();

    @Test
    @DisplayName("type=text role=user role=assistant only")
    void readMessagesReturnsOnlyVisibleTextPartsForUserAndAssistant() throws SQLException {
        Path database = tempDir.resolve("opencode.db");
        try (Connection connection = open(database)) {
            createTables(connection);
            insertMessage(connection, "msg-user", "ses_1", 1000L, "{\"role\":\"user\"}");
            insertPart(connection, "part-user-text", "msg-user", "ses_1", 1100L, "{\"type\":\"text\",\"text\":\"Open this session as chat\"}");
            insertMessage(connection, "msg-assistant", "ses_1", 2000L, "{\"role\":\"assistant\"}");
            insertPart(connection, "part-assistant-tool", "msg-assistant", "ses_1", 2100L, "{\"type\":\"tool\",\"tool\":\"bash\"}");
            insertPart(connection, "part-assistant-text", "msg-assistant", "ses_1", 2200L, "{\"type\":\"text\",\"text\":\"Here is the real opencode transcript.\"}");
            insertMessage(connection, "msg-system", "ses_1", 3000L, "{\"role\":\"system\"}");
            insertPart(connection, "part-system-text", "msg-system", "ses_1", 3100L, "{\"type\":\"text\",\"text\":\"system message\"}");
            insertMessage(connection, "msg-other-session", "ses_other", 4000L, "{\"role\":\"user\"}");
            insertPart(connection, "part-other-session", "msg-other-session", "ses_other", 4100L, "{\"type\":\"text\",\"text\":\"other session\"}");
        }

        List<SessionMessageDto> messages = reader.readMessages(database, "ses_1");

        assertThat(messages).extracting(SessionMessageDto::role).containsExactly("user", "assistant");
        assertThat(messages).extracting(SessionMessageDto::text).containsExactly(
                "Open this session as chat",
                "Here is the real opencode transcript."
        );
        assertThat(messages).extracting(SessionMessageDto::id).containsExactly("part-user-text", "part-assistant-text");
    }

    @Test
    void readMessagesRejectsInvalidJson() throws SQLException {
        Path database = tempDir.resolve("invalid-json.db");
        try (Connection connection = open(database)) {
            createTables(connection);
            insertMessage(connection, "msg-user", "ses_1", 1000L, "{\"role\":\"user\"}");
            insertPart(connection, "part-invalid", "msg-user", "ses_1", 1100L, "{bad json");
        }

        assertThatThrownBy(() -> reader.readMessages(database, "ses_1"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.OPENCODE_SESSION_FORMAT_UNKNOWN));
    }

    @Test
    void readPageReturnsNewestTenMessagesByDefaultWhenCursorIsAbsent() throws SQLException {
        Path database = tempDir.resolve("paged-default.db");
        try (Connection connection = open(database)) {
            createTables(connection);
            insertVisibleMessages(connection, "ses_1", 12);
        }

        OpencodeSqliteTranscriptReader.OpencodeTranscriptPage page = reader.readPage(database, "ses_1", 0, null);

        assertThat(page.messages()).extracting(SessionMessageDto::id).containsExactly(
                "part-03",
                "part-04",
                "part-05",
                "part-06",
                "part-07",
                "part-08",
                "part-09",
                "part-10",
                "part-11",
                "part-12"
        );
        assertThat(page.pageInfo().oldestCursor()).isEqualTo("part-03");
        assertThat(page.pageInfo().newestCursor()).isEqualTo("part-12");
        assertThat(page.pageInfo().hasMoreBefore()).isTrue();
    }

    @Test
    void readPageReturnsOlderMessagesBeforeCursor() throws SQLException {
        Path database = tempDir.resolve("paged-before.db");
        try (Connection connection = open(database)) {
            createTables(connection);
            insertVisibleMessages(connection, "ses_1", 12);
        }

        OpencodeSqliteTranscriptReader.OpencodeTranscriptPage page = reader.readPage(database, "ses_1", 10, "part-03");

        assertThat(page.messages()).extracting(SessionMessageDto::id).containsExactly("part-01", "part-02");
        assertThat(page.pageInfo().oldestCursor()).isEqualTo("part-01");
        assertThat(page.pageInfo().newestCursor()).isEqualTo("part-02");
        assertThat(page.pageInfo().hasMoreBefore()).isFalse();
    }

    @Test
    void readPageKeepsPageSortedOldestToNewest() throws SQLException {
        Path database = tempDir.resolve("paged-sorted.db");
        try (Connection connection = open(database)) {
            createTables(connection);
            insertVisibleMessages(connection, "ses_1", 12);
        }

        OpencodeSqliteTranscriptReader.OpencodeTranscriptPage page = reader.readPage(database, "ses_1", 3, "part-07");

        assertThat(page.messages()).extracting(SessionMessageDto::id).containsExactly("part-04", "part-05", "part-06");
        assertThat(page.pageInfo().oldestCursor()).isEqualTo("part-04");
        assertThat(page.pageInfo().newestCursor()).isEqualTo("part-06");
        assertThat(page.pageInfo().hasMoreBefore()).isTrue();
    }

    @Test
    void readPageRejectsUnknownBeforeCursor() throws SQLException {
        Path database = tempDir.resolve("paged-missing-cursor.db");
        try (Connection connection = open(database)) {
            createTables(connection);
            insertVisibleMessages(connection, "ses_1", 2);
        }

        assertThatThrownBy(() -> reader.readPage(database, "ses_1", 10, "missing-cursor"))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    private Connection open(Path database) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
    }

    private void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE message (
                        id TEXT PRIMARY KEY,
                        session_id TEXT,
                        time_created INTEGER,
                        data TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE part (
                        id TEXT PRIMARY KEY,
                        message_id TEXT,
                        session_id TEXT,
                        time_created INTEGER,
                        data TEXT
                    )
                    """);
        }
    }

    private void insertMessage(Connection connection, String id, String sessionId, long timeCreated, String data) throws SQLException {
        String sql = """
                INSERT INTO message (id, session_id, time_created, data)
                VALUES ('%s', '%s', %d, '%s')
                """.formatted(id, sessionId, timeCreated, escape(data));
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void insertPart(Connection connection, String id, String messageId, String sessionId, long timeCreated, String data) throws SQLException {
        String sql = """
                INSERT INTO part (id, message_id, session_id, time_created, data)
                VALUES ('%s', '%s', '%s', %d, '%s')
                """.formatted(id, messageId, sessionId, timeCreated, escape(data));
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void insertVisibleMessages(Connection connection, String sessionId, int count) throws SQLException {
        for (int index = 1; index <= count; index++) {
            String paddedIndex = "%02d".formatted(index);
            String role = index % 2 == 0 ? "assistant" : "user";
            insertMessage(
                    connection,
                    "msg-" + paddedIndex,
                    sessionId,
                    index * 1000L,
                    "{\"role\":\"%s\"}".formatted(role)
            );
            insertPart(
                    connection,
                    "part-" + paddedIndex,
                    "msg-" + paddedIndex,
                    sessionId,
                    index * 1000L + 100L,
                    "{\"type\":\"text\",\"text\":\"Visible message %s\"}".formatted(paddedIndex)
            );
        }
    }

    private String escape(String value) {
        return value.replace("'", "''");
    }
}
