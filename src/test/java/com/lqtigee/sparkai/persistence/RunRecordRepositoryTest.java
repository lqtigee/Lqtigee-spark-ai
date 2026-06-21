package com.lqtigee.sparkai.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lqtigee.sparkai.config.DatabaseProperties;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RunRecordRepositoryTest {

    @Test
    void saveStartedInsertsStartedStatusWithoutEndedAt() throws Exception {
        JdbcFixture fixture = fixture();

        fixture.repository().saveStarted("run-1", "CODEX", "session-1", "gpt-5.5");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(fixture.connection()).prepareStatement(sql.capture());
        verify(fixture.connection()).prepareStatement(contains("INSERT INTO run_records"));
        assertThat(sql.getValue()).doesNotContain("ended_at");
        verify(fixture.statement()).setString(1, "run-1");
        verify(fixture.statement()).setString(2, "CODEX");
        verify(fixture.statement()).setString(3, "session-1");
        verify(fixture.statement()).setString(4, "gpt-5.5");
        verify(fixture.statement()).setString(5, "STARTED");
        verify(fixture.statement()).executeUpdate();
    }

    @Test
    void markRunningUpdatesStatusWithoutEndedAt() throws Exception {
        JdbcFixture fixture = fixture();

        fixture.repository().markRunning("run-1");

        verify(fixture.connection()).prepareStatement("UPDATE run_records SET status = ? WHERE run_id = ?");
        verify(fixture.statement()).setString(1, "RUNNING");
        verify(fixture.statement()).setString(2, "run-1");
        verify(fixture.statement()).executeUpdate();
    }

    @Test
    void markExitedUpdatesStatusAndEndedAt() throws Exception {
        JdbcFixture fixture = fixture();

        fixture.repository().markExited("run-1");

        verify(fixture.connection()).prepareStatement("UPDATE run_records SET status = ?, ended_at = NOW() WHERE run_id = ?");
        verify(fixture.statement()).setString(1, "EXITED");
        verify(fixture.statement()).setString(2, "run-1");
        verify(fixture.statement()).executeUpdate();
    }

    @Test
    void markStoppedUpdatesStatusAndEndedAt() throws Exception {
        JdbcFixture fixture = fixture();

        fixture.repository().markStopped("run-1");

        verify(fixture.connection()).prepareStatement("UPDATE run_records SET status = ?, ended_at = NOW() WHERE run_id = ?");
        verify(fixture.statement()).setString(1, "STOPPED");
        verify(fixture.statement()).setString(2, "run-1");
        verify(fixture.statement()).executeUpdate();
    }

    @Test
    void markFailedUpdatesStatusAndEndedAt() throws Exception {
        JdbcFixture fixture = fixture();

        fixture.repository().markFailed("run-1");

        verify(fixture.connection()).prepareStatement("UPDATE run_records SET status = ?, ended_at = NOW() WHERE run_id = ?");
        verify(fixture.statement()).setString(1, "FAILED");
        verify(fixture.statement()).setString(2, "run-1");
        verify(fixture.statement()).executeUpdate();
    }

    @Test
    void updateThrowsApiExceptionWhenNoRowsChange() throws Exception {
        JdbcFixture fixture = fixture(0);

        assertThatThrownBy(() -> fixture.repository().markRunning("missing-run"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.code()).isEqualTo(ErrorCode.PROCESS_START_FAILED);
                    assertThat(apiException.detail()).isEqualTo("run_id=missing-run");
                });
    }

    @Test
    void sqlFailurePreservesOriginalDetail() throws Exception {
        JdbcFixture fixture = fixture(new SQLException("write failed"));

        assertThatThrownBy(() -> fixture.repository().markFailed("run-1"))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.code()).isEqualTo(ErrorCode.PROCESS_START_FAILED);
                    assertThat(apiException.detail()).isEqualTo("write failed");
                });
    }

    @Test
    void projectRunRecordSchemasUseEndedAtColumn() throws Exception {
        for (Path schema : runRecordSchemas()) {
            String sql = Files.readString(schema);

            assertThat(sql).contains("ended_at");
            assertThat(sql).doesNotContain("finished_at");
            assertThat(sql).doesNotContain("prompt");
            assertThat(sql).doesNotContain("transcript");
        }
    }

    private JdbcFixture fixture() throws Exception {
        return fixture(1);
    }

    private List<Path> runRecordSchemas() {
        return List.of(
                Path.of("src/main/resources/db/migration/V001_run_records.sql"),
                Path.of("src/test/resources/db/run-record-schema.sql"),
                Path.of("src/main/resources/db/postgres/001_init.sql")
        );
    }

    private JdbcFixture fixture(int updatedRows) throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(updatedRows);
        RunRecordRepository repository = new RunRecordRepository(new StubConnectionFactory(connection));
        return new JdbcFixture(repository, connection, statement);
    }

    private JdbcFixture fixture(SQLException failure) throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(failure);
        RunRecordRepository repository = new RunRecordRepository(new StubConnectionFactory(connection));
        return new JdbcFixture(repository, connection, statement);
    }

    private record JdbcFixture(
            RunRecordRepository repository,
            Connection connection,
            PreparedStatement statement
    ) {
    }

    private static class StubConnectionFactory extends PostgresConnectionFactory {

        private final Connection connection;

        StubConnectionFactory(Connection connection) {
            super(new DatabaseProperties());
            this.connection = connection;
        }

        @Override
        public Connection open() {
            return connection;
        }
    }
}
