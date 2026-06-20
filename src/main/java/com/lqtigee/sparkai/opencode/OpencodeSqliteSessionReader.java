package com.lqtigee.sparkai.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
            // Row queries are implemented by later parser tickets.
        } catch (SQLException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode SQLite database open failed",
                    exception.getMessage()
            );
        }

        throw new UnsupportedOperationException("Opencode SQLite session reading is not implemented yet");
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

    String extractModel(String modelJson) {
        if (modelJson == null || modelJson.isBlank()) {
            throw missingModelField("session.model");
        }

        JsonNode model;
        try {
            model = objectMapper.readTree(modelJson);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_FORMAT_UNKNOWN,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Opencode session model JSON is invalid",
                    exception.getMessage()
            );
        }

        String id = textValue(model.path("id"));
        String providerId = textValue(model.path("providerID"));
        if (id == null) {
            throw missingModelField("session.model.id");
        }
        if (providerId == null) {
            return id;
        }
        return providerId + "/" + id;
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
        String value = node.asText();
        return value.isBlank() ? null : value;
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
