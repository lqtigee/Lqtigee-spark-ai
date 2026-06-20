package com.lqtigee.sparkai.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqtigee.sparkai.dto.SessionMessageDto;
import com.lqtigee.sparkai.dto.SessionTranscriptDto;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class OpencodeSqliteTranscriptReader {

    private static final int DEFAULT_PAGE_LIMIT = 10;

    private static final String MESSAGE_QUERY = """
            SELECT
                message.id AS message_id,
                message.time_created AS message_time,
                message.data AS message_data,
                part.id AS part_id,
                part.time_created AS part_time,
                part.data AS part_data
            FROM message
            JOIN part ON part.message_id = message.id
            WHERE message.session_id = ?
              AND part.session_id = ?
            ORDER BY message.time_created ASC, part.time_created ASC, part.id ASC
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<SessionMessageDto> readMessages(Path databasePath, String sessionId) {
        return readAllMessages(databasePath, sessionId);
    }

    public OpencodeTranscriptPage readPage(Path databasePath, String sessionId, int limit, String beforeCursor) {
        List<SessionMessageDto> messages = readAllMessages(databasePath, sessionId);
        List<SessionMessageDto> pageMessages = pageMessages(messages, normalizedLimit(limit), beforeCursor);
        boolean hasMoreBefore = hasMoreBefore(messages, pageMessages);
        return new OpencodeTranscriptPage(
                pageMessages,
                SessionTranscriptDto.TranscriptPageInfoDto.fromMessages(pageMessages, hasMoreBefore)
        );
    }

    private List<SessionMessageDto> readAllMessages(Path databasePath, String sessionId) {
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
            return queryMessages(connection, sessionId);
        } catch (SQLException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Opencode transcript read failed",
                    exception.getMessage()
            );
        }
    }

    private List<SessionMessageDto> pageMessages(List<SessionMessageDto> messages, int limit, String beforeCursor) {
        int endExclusive = messages.size();
        if (beforeCursor != null && !beforeCursor.isBlank()) {
            endExclusive = indexOfCursor(messages, beforeCursor);
        }

        int startInclusive = Math.max(0, endExclusive - limit);
        return List.copyOf(messages.subList(startInclusive, endExclusive));
    }

    private boolean hasMoreBefore(List<SessionMessageDto> allMessages, List<SessionMessageDto> pageMessages) {
        if (pageMessages.isEmpty()) {
            return false;
        }
        return indexOfCursor(allMessages, pageMessages.getFirst().id()) > 0;
    }

    private int normalizedLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_PAGE_LIMIT;
        }
        return limit;
    }

    private int indexOfCursor(List<SessionMessageDto> messages, String beforeCursor) {
        for (int index = 0; index < messages.size(); index++) {
            if (beforeCursor.equals(messages.get(index).id())) {
                return index;
            }
        }
        throw new ApiException(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "Opencode transcript cursor was not found",
                "beforeCursor"
        );
    }

    private List<SessionMessageDto> queryMessages(Connection connection, String sessionId) throws SQLException {
        List<SessionMessageDto> messages = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(MESSAGE_QUERY)) {
            statement.setString(1, sessionId);
            statement.setString(2, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SessionMessageDto message = toMessage(resultSet);
                    if (message != null) {
                        messages.add(message);
                    }
                }
            }
        }
        return List.copyOf(messages);
    }

    private SessionMessageDto toMessage(ResultSet row) throws SQLException {
        JsonNode messageData = readJson(row.getString("message_data"));
        String role = textValue(messageData.path("role"));
        if (!"user".equals(role) && !"assistant".equals(role)) {
            return null;
        }

        JsonNode partData = readJson(row.getString("part_data"));
        if (!"text".equals(textValue(partData.path("type")))) {
            return null;
        }

        String text = normalizeText(textValue(partData.path("text")));
        if (text == null) {
            return null;
        }

        long timestampMillis = row.getLong("part_time");
        if (row.wasNull()) {
            timestampMillis = row.getLong("message_time");
        }

        return new SessionMessageDto(
                row.getString("part_id"),
                role,
                text,
                Instant.ofEpochMilli(timestampMillis)
        );
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.OPENCODE_SESSION_FORMAT_UNKNOWN,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Opencode transcript JSON is invalid",
                    exception.getMessage()
            );
        }
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

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record OpencodeTranscriptPage(
            List<SessionMessageDto> messages,
            SessionTranscriptDto.TranscriptPageInfoDto pageInfo
    ) {
    }
}
