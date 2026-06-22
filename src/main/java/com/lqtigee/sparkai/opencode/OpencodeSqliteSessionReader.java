package com.lqtigee.sparkai.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.springframework.http.HttpStatus;

public class OpencodeSqliteSessionReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> REQUIRED_SESSION_COLUMNS = Set.of(
            "id",
            "directory",
            "title",
            "model",
            "time_updated",
            "time_archived",
            "path",
            "agent"
    );

    private static final String SESSION_QUERY = """
            SELECT id, directory, title, model, time_updated, time_archived, path, agent
            FROM session
            ORDER BY time_updated DESC
            """;

    private static final String METADATA_MODEL_QUERY = """
            SELECT
                json_extract(data, '$.model.providerID') AS provider_id,
                json_extract(data, '$.model.id') AS model_id,
                json_extract(data, '$.model.modelID') AS model_id_alt
            FROM message
            WHERE session_id = ?
            UNION ALL
            SELECT
                json_extract(data, '$.model.providerID') AS provider_id,
                json_extract(data, '$.model.id') AS model_id,
                json_extract(data, '$.model.modelID') AS model_id_alt
            FROM event
            WHERE json_extract(data, '$.sessionID') = ?
            UNION ALL
            SELECT
                json_extract(data, '$.model.providerID') AS provider_id,
                json_extract(data, '$.model.id') AS model_id,
                json_extract(data, '$.model.modelID') AS model_id_alt
            FROM session_message
            WHERE session_id = ?
            """;

    private static final String LATEST_ASSISTANT_MESSAGE_QUERY = """
            SELECT
                json_extract(data, '$.time.completed') AS completed_at,
                json_extract(data, '$.finish') AS finish
            FROM message
            WHERE session_id = ?
              AND json_extract(data, '$.role') = 'assistant'
            ORDER BY time_created DESC, id DESC
            LIMIT 1
            """;

    public List<RemoteSessionDto> readSessions(Path databasePath) {
        if (databasePath == null || !Files.exists(databasePath)) {
            throw new ApiException(
                    ErrorCode.OPENCODE_CONFIG_NOT_FOUND,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode SQLite database not found",
                    databasePath == null ? "databasePath=null" : databasePath.toString()
            );
        }
        if (!Files.isReadable(databasePath)) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode SQLite database is not readable",
                    databasePath.toString()
            );
        }

        try (Connection connection = openReadOnly(databasePath)) {
            validateSchema(connection);
            return querySessions(connection, databasePath);
        } catch (SQLException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode SQLite database open failed",
                    exception.getMessage()
            );
        }
    }

    void validateSchema(Connection connection) {
        Set<String> actualColumns = new LinkedHashSet<>();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA table_info(session)")) {
            while (resultSet.next()) {
                actualColumns.add(resultSet.getString("name"));
            }
        } catch (SQLException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode SQLite session schema read failed",
                    exception.getMessage()
            );
        }

        List<String> missingColumns = REQUIRED_SESSION_COLUMNS.stream()
                .filter(column -> !actualColumns.contains(column))
                .sorted()
                .toList();
        if (!missingColumns.isEmpty()) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCHEMA_MISMATCH,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Opencode SQLite session schema is missing required columns",
                    String.join(",", missingColumns)
            );
        }
    }

    private List<RemoteSessionDto> querySessions(Connection connection, Path databasePath) throws SQLException {
        List<RemoteSessionDto> sessions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SESSION_QUERY);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                RemoteSessionDto session = toDto(connection, resultSet, databasePath);
                if (session != null) {
                    sessions.add(session);
                }
            }
        }
        return List.copyOf(sessions);
    }

    private RemoteSessionDto toDto(Connection connection, ResultSet row, Path databasePath) throws SQLException {
        String id = requiredText(row, "id");
        String workspace = requiredText(row, "directory");
        String title = requiredText(row, "title");
        String model = extractModel(connection, id, requiredText(row, "model"));
        if (model == null) {
            return null;
        }
        long updatedAtMillis = requiredLong(row, "time_updated");
        Object archivedAt = row.getObject("time_archived");

        return new RemoteSessionDto(
                id,
                AgentSource.OPENCODE,
                title,
                workspace,
                model,
                statusFor(connection, id, archivedAt),
                Instant.ofEpochMilli(updatedAtMillis),
                "",
                databasePath.toAbsolutePath().normalize().toString()
        );
    }

    String extractModel(String modelJson) {
        JsonNode model = parseModelJson(modelJson);
        String id = textValue(model.path("id"));
        if (id == null) {
            throw missingModelField("session.model.id");
        }
        return formatModel(model, id);
    }

    private String extractModel(Connection connection, String sessionId, String modelJson) throws SQLException {
        JsonNode model = parseModelJson(modelJson);
        String id = textValue(model.path("id"));
        if (id != null) {
            return formatModel(model, id);
        }
        if (isEmptyText(model.path("id"))) {
            return recoverMetadataModel(connection, sessionId);
        }
        throw missingModelField("session.model.id");
    }

    private JsonNode parseModelJson(String modelJson) {
        if (modelJson == null || modelJson.isBlank()) {
            throw missingModelField("session.model");
        }

        try {
            return objectMapper.readTree(modelJson);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_FORMAT_UNKNOWN,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Opencode session model JSON is invalid",
                    exception.getMessage()
            );
        }
    }

    private String recoverMetadataModel(Connection connection, String sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(METADATA_MODEL_QUERY)) {
            statement.setString(1, sessionId);
            statement.setString(2, sessionId);
            statement.setString(3, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String id = firstPresent(
                            textValue(resultSet.getString("model_id")),
                            textValue(resultSet.getString("model_id_alt"))
                    );
                    if (id != null) {
                        return formatModel(textValue(resultSet.getString("provider_id")), id);
                    }
                }
            }
        }
        return null;
    }

    private SessionStatus statusFor(Connection connection, String sessionId, Object archivedAt) throws SQLException {
        if (archivedAt != null) {
            return SessionStatus.IDLE;
        }
        return hasIncompleteLatestAssistantMessage(connection, sessionId) ? SessionStatus.RUNNING : SessionStatus.ACTIVE;
    }

    private boolean hasIncompleteLatestAssistantMessage(Connection connection, String sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LATEST_ASSISTANT_MESSAGE_QUERY)) {
            statement.setString(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
                Object completedAt = resultSet.getObject("completed_at");
                String finish = textValue(resultSet.getString("finish"));
                return completedAt == null && finish == null;
            }
        }
    }

    private String formatModel(JsonNode model, String id) {
        String providerId = textValue(model.path("providerID"));
        return formatModel(providerId, id);
    }

    private String formatModel(String providerId, String id) {
        if (providerId == null) {
            return id;
        }
        return providerId + "/" + id;
    }

    private String requiredText(ResultSet row, String column) throws SQLException {
        String value = row.getString(column);
        if (value == null || value.isBlank()) {
            throw missingSessionField("session." + column);
        }
        return value;
    }

    private long requiredLong(ResultSet row, String column) throws SQLException {
        long value = row.getLong(column);
        if (row.wasNull()) {
            throw missingSessionField("session." + column);
        }
        return value;
    }

    private Connection openReadOnly(Path databasePath) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("open_mode", "1");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath(), properties);
        connection.setReadOnly(true);
        return connection;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
            return null;
        }
        return textValue(node.asText());
    }

    private String textValue(String value) {
        if (value == null) {
            return null;
        }
        return value.isBlank() ? null : value;
    }

    private boolean isEmptyText(JsonNode node) {
        return node != null && node.isTextual() && node.asText().isBlank();
    }

    private String firstPresent(String first, String second) {
        return first == null ? second : first;
    }

    private ApiException missingSessionField(String detail) {
        return new ApiException(
                ErrorCode.OPENCODE_SESSION_FIELD_MISSING,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Opencode session field is missing",
                detail
        );
    }

    private ApiException missingModelField(String detail) {
        return new ApiException(
                ErrorCode.OPENCODE_SESSION_FIELD_MISSING,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Opencode session model field is missing",
                detail
        );
    }
}
