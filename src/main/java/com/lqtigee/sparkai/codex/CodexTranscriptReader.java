package com.lqtigee.sparkai.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqtigee.sparkai.dto.SessionMessageDto;
import com.lqtigee.sparkai.dto.SessionTranscriptDto;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CodexTranscriptReader {

    private static final int DEFAULT_PAGE_LIMIT = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<SessionMessageDto> readMessages(Path jsonlFile) {
        return readAllMessages(jsonlFile);
    }

    public CodexTranscriptPage readPage(Path jsonlFile, int limit, String beforeCursor) {
        List<SessionMessageDto> messages = readAllMessages(jsonlFile);
        List<SessionMessageDto> pageMessages = pageMessages(messages, normalizedLimit(limit), beforeCursor);
        boolean hasMoreBefore = hasMoreBefore(messages, pageMessages);
        return new CodexTranscriptPage(
                pageMessages,
                SessionTranscriptDto.TranscriptPageInfoDto.fromMessages(pageMessages, hasMoreBefore)
        );
    }

    private List<SessionMessageDto> readAllMessages(Path jsonlFile) {
        if (jsonlFile == null || !Files.exists(jsonlFile)) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex transcript file not found",
                    jsonlFile == null ? "jsonlFile=null" : jsonlFile.toString()
            );
        }
        if (!Files.isReadable(jsonlFile)) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex transcript file is not readable",
                    jsonlFile.toString()
            );
        }

        List<SessionMessageDto> messages = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(jsonlFile)) {
            String line;
            long lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                JsonNode record = readRecord(line);
                SessionMessageDto message = toMessage(record, lineNumber);
                if (message != null) {
                    messages.add(message);
                }
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_SCAN_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "Codex transcript read failed",
                    exception.getMessage()
            );
        }

        return List.copyOf(messages);
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
                "Codex transcript cursor was not found",
                "beforeCursor"
        );
    }

    private JsonNode readRecord(String line) {
        try {
            return objectMapper.readTree(line);
        } catch (IOException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_FORMAT_UNKNOWN,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Codex transcript JSONL line is invalid",
                    exception.getMessage()
            );
        }
    }

    private SessionMessageDto toMessage(JsonNode record, long lineNumber) {
        if (!"response_item".equals(textValue(record.path("type")))) {
            return null;
        }

        JsonNode payload = record.path("payload");
        if (!"message".equals(textValue(payload.path("type")))) {
            return null;
        }

        String role = textValue(payload.path("role"));
        if (!"user".equals(role) && !"assistant".equals(role)) {
            return null;
        }

        String text = visibleText(payload.path("content"));
        if (text == null) {
            return null;
        }

        return new SessionMessageDto(
                firstPresent(textValue(payload.path("id")), "line-" + lineNumber),
                role,
                text,
                timestamp(record)
        );
    }

    private Instant timestamp(JsonNode record) {
        String value = textValue(record.path("timestamp"));
        if (value == null) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_FORMAT_UNKNOWN,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Codex transcript message timestamp is missing",
                    "timestamp"
            );
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ApiException(
                    ErrorCode.CODEX_SESSION_FORMAT_UNKNOWN,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Codex transcript message timestamp is invalid",
                    exception.getMessage()
            );
        }
    }

    private String visibleText(JsonNode content) {
        if (!content.isArray()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode item : content) {
            String itemText = textValue(item.path("text"));
            if (itemText != null) {
                if (!text.isEmpty()) {
                    text.append("\n");
                }
                text.append(itemText);
            }
        }

        return normalizeText(text.toString());
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    private String normalizeText(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstPresent(String first, String second) {
        return first == null ? second : first;
    }

    public record CodexTranscriptPage(
            List<SessionMessageDto> messages,
            SessionTranscriptDto.TranscriptPageInfoDto pageInfo
    ) {
    }
}
