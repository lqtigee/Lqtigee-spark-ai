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
            throw persistenceFailed(exception.getMessage());
        }
    }

    public void markRunning(String runId) {
        updateStatus(runId, "RUNNING", false);
    }

    public void markExited(String runId) {
        updateStatus(runId, "EXITED", true);
    }

    public void markStopped(String runId) {
        updateStatus(runId, "STOPPED", true);
    }

    public void markFailed(String runId) {
        updateStatus(runId, "FAILED", true);
    }

    private void updateStatus(String runId, String status, boolean terminal) {
        String sql = terminal
                ? "UPDATE run_records SET status = ?, ended_at = NOW() WHERE run_id = ?"
                : "UPDATE run_records SET status = ? WHERE run_id = ?";
        try (Connection connection = connectionFactory.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, runId);
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw persistenceFailed("run_id=" + runId);
            }
        } catch (SQLException exception) {
            throw persistenceFailed(exception.getMessage());
        }
    }

    private ApiException persistenceFailed(String detail) {
        return new ApiException(
                ErrorCode.PROCESS_START_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                "Run record persistence failed",
                detail
        );
    }
}
