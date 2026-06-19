package com.lqtigee.sparkai.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;

public class CodexJsonlParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RemoteSessionDto parse(Path file) {
        String id = null;
        String workspace = null;
        String model = null;
        Instant updatedAt = null;

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                JsonNode record = readRecord(line);
                updatedAt = newestTimestamp(updatedAt, textValue(record.path("timestamp")));

                String type = textValue(record.path("type"));
                JsonNode payload = record.path("payload");
                if ("session_meta".equals(type)) {
                    id = firstPresent(id, textValue(payload.path("id")));
                    workspace = firstPresent(workspace, textValue(payload.path("cwd")));
                } else if ("turn_context".equals(type)) {
                    workspace = firstPresent(workspace, textValue(payload.path("cwd")));
                    model = firstPresent(model, textValue(payload.path("model")));
                }
            }

            if (updatedAt == null) {
                updatedAt = Files.getLastModifiedTime(file).toInstant();
            }

            requirePresent(id, "session_meta.payload.id");
            requirePresent(workspace, "session_meta.payload.cwd or turn_context.payload.cwd");
            requirePresent(model, "turn_context.payload.model");
            requirePresent(updatedAt, "timestamp or file mtime");

            return new RemoteSessionDto(
                    id,
                    AgentSource.CODEX,
                    "Codex " + shortId(id),
                    workspace,
                    model,
                    SessionStatus.UNKNOWN,
                    updatedAt,
                    "",
                    file.toAbsolutePath().normalize().toString()
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex JSONL parse failed",
                    exception.getMessage()
            );
        }
    }

    private JsonNode readRecord(String line) {
        try {
            return objectMapper.readTree(line);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_FORMAT_UNKNOWN,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Codex JSONL line is invalid",
                    exception.getMessage()
            );
        }
    }

    private Instant newestTimestamp(Instant current, String candidate) {
        if (candidate == null) {
            return current;
        }
        try {
            Instant parsed = Instant.parse(candidate);
            if (current == null || parsed.isAfter(current)) {
                return parsed;
            }
            return current;
        } catch (DateTimeParseException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_FORMAT_UNKNOWN,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Codex JSONL timestamp is invalid",
                    exception.getMessage()
            );
        }
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    private String firstPresent(String current, String candidate) {
        return current == null ? candidate : current;
    }

    private void requirePresent(Object value, String fieldName) {
        if (value == null) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_FIELD_MISSING,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Codex session field is missing",
                    fieldName
            );
        }
    }

    private String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(0, 8);
    }
}
