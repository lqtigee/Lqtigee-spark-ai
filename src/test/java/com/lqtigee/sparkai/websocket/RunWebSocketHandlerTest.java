package com.lqtigee.sparkai.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lqtigee.sparkai.config.SecurityProperties;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.dto.RunStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.dto.StartRunResponse;
import com.lqtigee.sparkai.runtime.RunEventBus;
import com.lqtigee.sparkai.service.RunService;
import java.net.URI;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

class RunWebSocketHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void startMessageStartsRunAndStreamsEventsOverSameSocket() throws Exception {
        RunService runService = org.mockito.Mockito.mock(RunService.class);
        RunEventBus runEventBus = new RunEventBus();
        SecurityProperties securityProperties = securityProperties();
        RunWebSocketHandler handler = new RunWebSocketHandler(objectMapper, runService, runEventBus, securityProperties);
        CapturingWebSocketSession session = new CapturingWebSocketSession("ws://localhost/ws/runs?token=test-token");
        when(runService.start(any(StartRunRequest.class))).thenReturn(new StartRunResponse(
                "run-1",
                "session-1",
                AgentSource.CODEX,
                RunStatus.RUNNING,
                Instant.parse("2026-06-20T00:00:00Z")
        ));

        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, new TextMessage("""
                {
                  "type": "run.start",
                  "request": {
                    "sessionId": "session-1",
                    "source": "CODEX",
                    "modelId": "gpt-5.5",
                    "mode": "ASK",
                    "prompt": "hello",
                    "confirmDangerous": false
                  }
                }
                """));
        runEventBus.publish("run-1", new RunEventDto("run-1", "stdout", "line-1", Instant.parse("2026-06-20T00:00:01Z"), Map.of()));

        assertThat(session.textMessages())
                .anySatisfy(message -> assertThat(message).contains("\"type\":\"connection.ready\""))
                .anySatisfy(message -> assertThat(message).contains("\"type\":\"run.started\"").contains("\"runId\":\"run-1\""))
                .anySatisfy(message -> assertThat(message).contains("\"type\":\"run.event\"").contains("\"message\":\"line-1\""));
        ArgumentCaptor<StartRunRequest> requestCaptor = ArgumentCaptor.forClass(StartRunRequest.class);
        verify(runService).start(requestCaptor.capture());
        assertThat(requestCaptor.getValue().sessionId()).isEqualTo("session-1");
        assertThat(requestCaptor.getValue().prompt()).isEqualTo("hello");
    }

    @Test
    void invalidTokenClosesSocketWithPolicyViolation() throws Exception {
        RunService runService = org.mockito.Mockito.mock(RunService.class);
        RunWebSocketHandler handler = new RunWebSocketHandler(objectMapper, runService, new RunEventBus(), securityProperties());
        CapturingWebSocketSession session = new CapturingWebSocketSession("ws://localhost/ws/runs?token=wrong");

        handler.afterConnectionEstablished(session);

        assertThat(session.closeStatus()).isEqualTo(CloseStatus.POLICY_VIOLATION);
        assertThat(session.textMessages()).anySatisfy(message -> assertThat(message).contains("\"type\":\"run.error\""));
    }

    private SecurityProperties securityProperties() {
        SecurityProperties properties = new SecurityProperties();
        properties.setApiToken("test-token");
        return properties;
    }

    private static class CapturingWebSocketSession implements WebSocketSession {

        private final URI uri;
        private final List<String> textMessages = new ArrayList<>();
        private boolean open = true;
        private CloseStatus closeStatus;

        CapturingWebSocketSession(String uri) {
            this.uri = URI.create(uri);
        }

        @Override
        public String getId() {
            return "session-1";
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return new WebSocketHttpHeaders();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Map.of();
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 8192;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 8192;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return List.of();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) {
            if (message instanceof TextMessage textMessage) {
                textMessages.add(textMessage.getPayload());
            }
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public void close(CloseStatus status) {
            closeStatus = status;
            open = false;
        }

        List<String> textMessages() {
            return textMessages;
        }

        CloseStatus closeStatus() {
            return closeStatus;
        }
    }
}
