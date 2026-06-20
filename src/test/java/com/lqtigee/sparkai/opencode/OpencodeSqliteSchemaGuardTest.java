package com.lqtigee.sparkai.opencode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpencodeSqliteSchemaGuardTest {

    @TempDir
    private Path tempDir;

    private final OpencodeSqliteSessionReader reader = new OpencodeSqliteSessionReader();

    @Test
    void validateSchemaPassesForRequiredSessionColumns() throws SQLException {
        Path database = tempDir.resolve("complete.db");
        try (Connection connection = open(database)) {
            createSessionTable(connection, true);

            assertThatNoException().isThrownBy(() -> reader.validateSchema(connection));
        }
    }

    @Test
    void validateSchemaFailsWhenRequiredColumnIsMissing() throws SQLException {
        Path database = tempDir.resolve("missing-model.db");
        try (Connection connection = open(database)) {
            createSessionTable(connection, false);

            assertThatThrownBy(() -> reader.validateSchema(connection))
                    .isInstanceOfSatisfying(ApiException.class, exception -> {
                        assertThat(exception.code()).isEqualTo(ErrorCode.OPENCODE_SESSION_SCHEMA_MISMATCH);
                        assertThat(exception.detail()).contains("model");
                    });
        }
    }

    private Connection open(Path database) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
    }

    private void createSessionTable(Connection connection, boolean includeModel) throws SQLException {
        String modelColumn = includeModel ? "model TEXT," : "";
        String sql = """
                CREATE TABLE session (
                    id TEXT PRIMARY KEY,
                    directory TEXT,
                    title TEXT,
                    %s
                    time_updated INTEGER,
                    time_archived INTEGER,
                    path TEXT,
                    agent TEXT
                )
                """.formatted(modelColumn);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
