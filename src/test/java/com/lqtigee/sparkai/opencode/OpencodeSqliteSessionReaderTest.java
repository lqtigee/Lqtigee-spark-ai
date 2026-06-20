package com.lqtigee.sparkai.opencode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpencodeSqliteSessionReaderTest {

    @TempDir
    private Path tempDir;

    private final OpencodeSqliteSessionReader reader = new OpencodeSqliteSessionReader();

    @Test
    void readSessionsMapsSanitizedSessionRow() throws SQLException {
        Path database = tempDir.resolve("opencode.db");
        try (Connection connection = open(database)) {
            createSessionTable(connection);
            insertSession(
                    connection,
                    "ses_01JYK3T8R2A8H3X9M6Q4N5P7Z1",
                    "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
                    "Lqtigee project",
                    """
                    {"id":"Lqtigee","providerID":"openai","variant":"default"}
                    """,
                    1781887279066L,
                    null
            );
        }

        List<RemoteSessionDto> sessions = reader.readSessions(database);

        assertThat(sessions).hasSize(1);
        RemoteSessionDto session = sessions.getFirst();
        assertThat(session.id()).isEqualTo("ses_01JYK3T8R2A8H3X9M6Q4N5P7Z1");
        assertThat(session.source()).isEqualTo(AgentSource.OPENCODE);
        assertThat(session.title()).isEqualTo("Lqtigee project");
        assertThat(session.workspace()).isEqualTo("/home/lqtiger/GIT_HUB/Lqtigee-spark-ai");
        assertThat(session.model()).isEqualTo("openai/Lqtigee");
        assertThat(session.status()).isEqualTo(SessionStatus.UNKNOWN);
        assertThat(session.updatedAt()).isEqualTo(Instant.ofEpochMilli(1781887279066L));
        assertThat(session.lastMessage()).isEmpty();
        assertThat(session.rawFile()).isEqualTo(database.toAbsolutePath().normalize().toString());
    }

    @Test
    void readSessionsFailsWhenRequiredSessionFieldIsMissing() throws SQLException {
        Path database = tempDir.resolve("missing-field.db");
        try (Connection connection = open(database)) {
            createSessionTable(connection);
            insertSession(
                    connection,
                    "ses_01JYK3T8R2A8H3X9M6Q4N5P7Z2",
                    "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
                    "Missing model",
                    null,
                    1781887279066L,
                    null
            );
        }

        assertThatThrownBy(() -> reader.readSessions(database))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(ErrorCode.OPENCODE_SESSION_FIELD_MISSING);
                    assertThat(exception.detail()).contains("session.model");
                });
    }

    @Test
    void readSessionsExcludesEmptyModelIdWithoutRecoverableMetadata() throws SQLException {
        Path database = tempDir.resolve("empty-model-id.db");
        try (Connection connection = open(database)) {
            createSessionTable(connection);
            createMetadataTables(connection);
            insertSession(
                    connection,
                    "ses_empty_model",
                    "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
                    "Non-runnable session",
                    """
                    {"id":"","providerID":"Lqtigee","variant":"default"}
                    """,
                    1781887279066L,
                    null
            );
            insertMessageMetadata(
                    connection,
                    "msg_empty_model",
                    "ses_empty_model",
                    """
                    {"model":{"providerID":"Lqtigee","modelID":""}}
                    """
            );
        }

        List<RemoteSessionDto> sessions = reader.readSessions(database);

        assertThat(sessions).isEmpty();
    }

    @Test
    void readSessionsDoesNotUseProviderIdAloneAsModel() throws SQLException {
        Path database = tempDir.resolve("provider-only.db");
        try (Connection connection = open(database)) {
            createSessionTable(connection);
            createMetadataTables(connection);
            insertSession(
                    connection,
                    "ses_provider_only",
                    "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
                    "Provider only session",
                    """
                    {"id":"","providerID":"gpt-5.5","variant":"high"}
                    """,
                    1781887279066L,
                    null
            );
            insertEventMetadata(
                    connection,
                    "event_provider_only",
                    "ses_provider_only",
                    """
                    {"sessionID":"ses_provider_only","model":{"providerID":"gpt-5.5"}}
                    """
            );
        }

        List<RemoteSessionDto> sessions = reader.readSessions(database);

        assertThat(sessions)
                .extracting(RemoteSessionDto::model)
                .doesNotContain("gpt-5.5");
    }

    @Test
    void readSessionsRecoversEmptyModelIdFromMetadataModelId() throws SQLException {
        Path database = tempDir.resolve("metadata-model-id.db");
        try (Connection connection = open(database)) {
            createSessionTable(connection);
            createMetadataTables(connection);
            insertSession(
                    connection,
                    "ses_metadata_model",
                    "/home/lqtiger/GIT_HUB/Lqtigee-spark-ai",
                    "Metadata model session",
                    """
                    {"id":"","providerID":"openai","variant":"default"}
                    """,
                    1781887279066L,
                    null
            );
            insertSessionMessageMetadata(
                    connection,
                    "session_message_metadata_model",
                    "ses_metadata_model",
                    """
                    {"sessionID":"ses_metadata_model","model":{"providerID":"openai","modelID":"Lqtigee"}}
                    """
            );
        }

        List<RemoteSessionDto> sessions = reader.readSessions(database);

        assertThat(sessions)
                .extracting(RemoteSessionDto::model)
                .containsExactly("openai/Lqtigee");
    }

    private Connection open(Path database) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
    }

    private void createSessionTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE session (
                    id TEXT PRIMARY KEY,
                    directory TEXT,
                    title TEXT,
                    model TEXT,
                    time_updated INTEGER,
                    time_archived INTEGER,
                    path TEXT,
                    agent TEXT
                )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void createMetadataTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE message (
                        id TEXT PRIMARY KEY,
                        session_id TEXT,
                        data TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE event (
                        id TEXT PRIMARY KEY,
                        data TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE session_message (
                        id TEXT PRIMARY KEY,
                        session_id TEXT,
                        data TEXT
                    )
                    """);
        }
    }

    private void insertSession(
            Connection connection,
            String id,
            String directory,
            String title,
            String model,
            long timeUpdated,
            Long timeArchived
    ) throws SQLException {
        String archivedSql = timeArchived == null ? "NULL" : timeArchived.toString();
        String modelSql = model == null ? "NULL" : "'" + model.trim().replace("'", "''") + "'";
        String sql = """
                INSERT INTO session (id, directory, title, model, time_updated, time_archived, path, agent)
                VALUES ('%s', '%s', '%s', %s, %d, %s, '%s', '%s')
                """.formatted(
                id,
                directory,
                title,
                modelSql,
                timeUpdated,
                archivedSql,
                directory,
                "build"
        );
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void insertMessageMetadata(
            Connection connection,
            String id,
            String sessionId,
            String data
    ) throws SQLException {
        String sql = """
                INSERT INTO message (id, session_id, data)
                VALUES ('%s', '%s', '%s')
                """.formatted(id, sessionId, data.trim().replace("'", "''"));
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void insertEventMetadata(
            Connection connection,
            String id,
            String sessionId,
            String data
    ) throws SQLException {
        String sql = """
                INSERT INTO event (id, data)
                VALUES ('%s', '%s')
                """.formatted(id, data.trim().replace("'", "''"));
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void insertSessionMessageMetadata(
            Connection connection,
            String id,
            String sessionId,
            String data
    ) throws SQLException {
        String sql = """
                INSERT INTO session_message (id, session_id, data)
                VALUES ('%s', '%s', '%s')
                """.formatted(id, sessionId, data.trim().replace("'", "''"));
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
