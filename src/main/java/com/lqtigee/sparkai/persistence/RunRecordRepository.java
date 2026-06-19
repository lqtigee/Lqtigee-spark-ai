package com.lqtigee.sparkai.persistence;

import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.http.HttpStatus;

public class RunRecordRepository {

    private final PostgresConnectionFactory connectionFactory;

    public RunRecordRepository(PostgresConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void saveStarted(String runId, String source, String sessionId, String modelId) {
        String sql = """
                INSERT INTO run_records (run_id, source, session_id, model_id, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, runId);
            statement.setString(2, source);
            statement.setString(3, sessionId);
            statement.setString(4, modelId);
            statement.setString(5, "STARTED");
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new ApiException(
                    ErrorCode.PROCESS_START_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Run record persistence failed",
                    exception.getMessage()
            );
        }
    }
}
