package com.lqtigee.sparkai.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqtigee.sparkai.config.SecurityProperties;
import com.lqtigee.sparkai.dto.ApiErrorDto;
import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.runtime.RunEventBus;
import com.lqtigee.sparkai.service.RunService;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RunWebSocketHandler extends TextWebSocketHandler {

    private static final String CLIENT_MESSAGE_START = "run.start";
    private static final String SERVER_MESSAGE_STARTED = "run.started";
    private static final String SERVER_MESSAGE_EVENT = "run.event";
    private static final String SERVER_MESSAGE_ERROR = "run.error";
    private static final String SERVER_MESSAGE_ACK = "connection.ready";

    private final ObjectMapper objectMapper;
    private final RunService runService;
    private final RunEventBus runEventBus;
    private final SecurityProperties securityProperties;
    private final Map<String, RunEventBus.RunEventSubscription> subscriptions = new ConcurrentHashMap<>();

    public RunWebSocketHandler(
            ObjectMapper objectMapper,
            RunService runService,
            RunEventBus runEventBus,
            SecurityProperties securityProperties
    ) {
        this.objectMapper = objectMapper;
        this.runService = runService;
        this.runEventBus = runEventBus;
        this.securityProperties = securityProperties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!isAuthorized(session.getUri())) {
            sendError(session, ErrorCode.AUTH_TOKEN_INVALID, "Authorization bearer token is invalid", null);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sendEnvelope(session, SERVER_MESSAGE_ACK, Map.of("connectedAt", Instant.now()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode envelope = objectMapper.readTree(message.getPayload());
        String type = text(envelope.path("type"));
        if (!CLIENT_MESSAGE_START.equals(type)) {
            sendError(session, ErrorCode.VALIDATION_FAILED, "Unsupported WebSocket message type", type);
            return;
        }
        StartRunRequest request = objectMapper.treeToValue(envelope.path("request"), StartRunRequest.class);
        startRun(session, request);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        RunEventBus.RunEventSubscription subscription = subscriptions.remove(session.getId());
        if (subscription != null) {
            subscription.close();
        }
    }

    private void startRun(WebSocketSession session, StartRunRequest request) throws IOException {
        try {
            StartRunResponse response = runService.start(request);
            sendEnvelope(session, SERVER_MESSAGE_STARTED, response);
            RunEventBus.RunEventSubscription previousSubscription = subscriptions.remove(session.getId());
            if (previousSubscription != null) {
                previousSubscription.close();
            }
            subscriptions.put(
                    session.getId(),
                    runEventBus.subscribeReplay(response.runId(), event -> sendRunEvent(session, event))
            );
        } catch (ApiException exception) {
            sendError(session, exception.code(), exception.getMessage(), exception.detail());
        } catch (RuntimeException exception) {
            sendError(session, ErrorCode.INTERNAL_ERROR, "Internal server error", null);
        }
    }

    private void sendRunEvent(WebSocketSession session, RunEventDto event) {
        try {
            if (session.isOpen()) {
                sendEnvelope(session, SERVER_MESSAGE_EVENT, event);
            }
        } catch (IOException exception) {
            RunEventBus.RunEventSubscription subscription = subscriptions.remove(session.getId());
            if (subscription != null) {
                subscription.close();
            }
        }
    }

    private boolean isAuthorized(URI uri) {
        securityProperties.validate();
        String token = queryParam(uri, "token");
        return token != null && token.equals(securityProperties.getApiToken());
    }

    private String queryParam(URI uri, String key) {
        if (uri == null || uri.getRawQuery() == null) {
            return null;
        }
        for (String part : uri.getRawQuery().split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && key.equals(urlDecode(pair[0]))) {
                return urlDecode(pair[1]);
            }
        }
        return null;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendError(WebSocketSession session, ErrorCode code, String message, String detail) throws IOException {
        ApiErrorDto error = new ApiErrorDto(code, message, detail, Instant.now(), "/ws/runs");
        sendEnvelope(session, SERVER_MESSAGE_ERROR, error);
    }

    private void sendEnvelope(WebSocketSession session, String type, Object payload) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "payload", payload
            ))));
        }
    }

    private String text(JsonNode node) {
        return node != null && node.isTextual() && !node.asText().isBlank() ? node.asText() : null;
    }
}
