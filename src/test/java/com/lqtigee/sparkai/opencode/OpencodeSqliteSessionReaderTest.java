package com.lqtigee.sparkai.opencode;

import static org.assertj.core.api.Assertions.assertThat;

import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionStatus;
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
        String sql = """
                INSERT INTO session (id, directory, title, model, time_updated, time_archived, path, agent)
                VALUES ('%s', '%s', '%s', '%s', %d, %s, '%s', '%s')
                """.formatted(
                id,
                directory,
                title,
                model.trim().replace("'", "''"),
                timeUpdated,
                archivedSql,
                directory,
                "build"
        );
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
