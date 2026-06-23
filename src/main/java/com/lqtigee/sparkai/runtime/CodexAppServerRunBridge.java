package com.lqtigee.sparkai.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lqtigee.sparkai.dto.CodexRunOptionsDto;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.runtime.CodexAppServerClient.CodexAppServerNotificationListener;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CodexAppServerRunBridge {

    private final ObjectMapper objectMapper;
    private final CodexAppServerClient client;
    private final RunEventBus runEventBus;
    private final RunRegistry runRegistry;
    private final Map<String, ActiveCodexTurn> activeTurns = new ConcurrentHashMap<>();
    private final Map<String, CodexRunNotificationListener> activeListeners = new ConcurrentHashMap<>();

    public CodexAppServerRunBridge(
            ObjectMapper objectMapper,
            CodexAppServerClient client,
            RunEventBus runEventBus,
            RunRegistry runRegistry
    ) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.runEventBus = runEventBus;
        this.runRegistry = runRegistry;
    }

    public void start(String runId, StartRunRequest request, RemoteSessionDto session, ModelDto model) {
        CodexRunNotificationListener listener = new CodexRunNotificationListener(runId, request.sessionId());
        activeListeners.put(runId, listener);
        client.addNotificationListener(listener);
        try {
            resumeThread(request, session, model);
            JsonNode turnStartResult = client.request("turn/start", turnStartParams(request, session, model));
            String turnId = textValue(turnStartResult.path("turn").path("id"));
            if (turnId != null) {
                activeTurns.put(runId, new ActiveCodexTurn(request.sessionId(), turnId));
            }
            runEventBus.publish(runId, event(runId, "status", "Codex app-server turn started", Map.of(
                    "threadId", request.sessionId(),
                    "turnId", turnId == null ? "" : turnId,
                    "transport", "codex-app-server"
            )));
        } catch (ApiException exception) {
            removeListener(runId, listener);
            runEventBus.publish(runId, event(runId, "error", exception.getMessage(), Map.of(
                    "detail", exception.detail() == null ? "" : exception.detail(),
                    "transport", "codex-app-server"
            )));
            throw exception;
        }
    }

    public boolean stop(String runId) {
        ActiveCodexTurn activeTurn = activeTurns.remove(runId);
        if (activeTurn == null) {
            return false;
        }
        CodexRunNotificationListener listener = activeListeners.remove(runId);
        if (listener != null) {
            client.removeNotificationListener(listener);
        }
        ObjectNode params = objectMapper.createObjectNode();
        params.put("threadId", activeTurn.threadId());
        params.put("turnId", activeTurn.turnId());
        client.request("turn/interrupt", params);
        runRegistry.markStopped(runId);
        runEventBus.publish(runId, event(runId, "stopped", "Codex turn interrupted", Map.of(
                "threadId", activeTurn.threadId(),
                "turnId", activeTurn.turnId(),
                "transport", "codex-app-server"
        )));
        return true;
    }

    private void removeListener(String runId, CodexRunNotificationListener listener) {
        activeListeners.remove(runId, listener);
        client.removeNotificationListener(listener);
    }

    private void resumeThread(StartRunRequest request, RemoteSessionDto session, ModelDto model) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("threadId", request.sessionId());
        params.put("cwd", session.workspace());
        params.put("model", model.commandModelName());
        params.put("sandbox", sandboxMode(request));
        String approvalPolicy = approvalPolicy(request);
        if (approvalPolicy != null) {
            params.put("approvalPolicy", approvalPolicy);
        }
        client.request("thread/resume", params);
    }

    private ObjectNode turnStartParams(StartRunRequest request, RemoteSessionDto session, ModelDto model) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("threadId", request.sessionId());
        params.put("cwd", session.workspace());
        params.put("model", model.commandModelName());
        params.set("input", input(request));
        params.set("sandboxPolicy", sandboxPolicy(request));

        String approvalPolicy = approvalPolicy(request);
        if (approvalPolicy != null) {
            params.put("approvalPolicy", approvalPolicy);
        }
        String effort = reasoningEffort(request);
        if (effort != null) {
            params.put("effort", effort);
        }
        return params;
    }

    private ArrayNode input(StartRunRequest request) {
        ArrayNode input = objectMapper.createArrayNode();
        ObjectNode text = objectMapper.createObjectNode();
        text.put("type", "text");
        text.put("text", request.prompt());
        input.add(text);
        return input;
    }

    private String sandboxMode(StartRunRequest request) {
        if (request.codexOptions() != null && nonBlank(request.codexOptions().sandbox())) {
            return request.codexOptions().sandbox();
        }
        if (request.mode() == CommandMode.EDIT) {
            return "workspace-write";
        }
        if (request.mode() == CommandMode.SHELL) {
            return "danger-full-access";
        }
        return "read-only";
    }

    private ObjectNode sandboxPolicy(StartRunRequest request) {
        ObjectNode policy = objectMapper.createObjectNode();
        String mode = sandboxMode(request);
        if ("workspace-write".equals(mode)) {
            policy.put("type", "workspaceWrite");
        } else if ("danger-full-access".equals(mode)) {
            policy.put("type", "dangerFullAccess");
        } else {
            policy.put("type", "readOnly");
        }
        return policy;
    }

    private String approvalPolicy(StartRunRequest request) {
        if (request.codexOptions() != null && nonBlank(request.codexOptions().approvalPolicy())) {
            return request.codexOptions().approvalPolicy();
        }
        return null;
    }

    private String reasoningEffort(StartRunRequest request) {
        CodexRunOptionsDto options = request.codexOptions();
        if (options == null || options.configOverrides() == null) {
            return null;
        }
        return options.configOverrides().stream()
                .filter(override -> "model_reasoning_effort".equals(override.key()))
                .map(CodexRunOptionsDto.ConfigOverrideDto::value)
                .filter(this::nonBlank)
                .findFirst()
                .orElse(null);
    }

    private RunEventDto event(String runId, String type, String message, Map<String, Object> data) {
        return new RunEventDto(runId, type, message, Instant.now(), data);
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    private class CodexRunNotificationListener implements CodexAppServerNotificationListener {

        private final String runId;
        private final String threadId;

        private CodexRunNotificationListener(String runId, String threadId) {
            this.runId = runId;
            this.threadId = threadId;
        }

        @Override
        public void onNotification(String method, JsonNode params) {
            String notificationThreadId = textValue(params.path("threadId"));
            if (notificationThreadId != null && !threadId.equals(notificationThreadId)) {
                return;
            }
            switch (method) {
                case "thread/status/changed" -> publishStatus(params);
                case "turn/started" -> publishTurnStarted(params);
                case "item/agentMessage/delta" -> publishDelta("assistant", params);
                case "commandExecution/outputDelta" -> publishDelta("tool", params);
                case "turn/plan/updated" -> publishPlan(params);
                case "thread/tokenUsage/updated" -> publishTokenUsage(params);
                case "error" -> publishError(params);
                case "turn/completed" -> publishCompleted(params);
                default -> {
                }
            }
        }

        private void publishStatus(JsonNode params) {
            String statusType = textValue(params.path("status").path("type"));
            runEventBus.publish(runId, event(runId, "status", "Codex thread status: " + firstPresent(statusType, "unknown"), Map.of(
                    "method", "thread/status/changed"
            )));
        }

        private void publishTurnStarted(JsonNode params) {
            String turnId = textValue(params.path("turn").path("id"));
            runEventBus.publish(runId, event(runId, "status", "Codex turn started", Map.of(
                    "turnId", firstPresent(turnId, "")
            )));
        }

        private void publishDelta(String type, JsonNode params) {
            String delta = textValue(params.path("delta"));
            if (delta == null) {
                return;
            }
            runEventBus.publish(runId, event(runId, type, delta, Map.of(
                    "streaming", true
            )));
        }

        private void publishPlan(JsonNode params) {
            runEventBus.publish(runId, event(runId, "tool", params.path("plan").toString(), Map.of(
                    "method", "turn/plan/updated"
            )));
        }

        private void publishTokenUsage(JsonNode params) {
            Map<String, Object> data = new HashMap<>();
            data.put("method", "thread/tokenUsage/updated");
            data.put("tokenUsage", params.path("tokenUsage").toString());
            runEventBus.publish(runId, event(runId, "status", "Codex token usage updated", data));
        }

        private void publishError(JsonNode params) {
            String message = firstPresent(textValue(params.path("error").path("message")), "Codex turn failed");
            activeTurns.remove(runId);
            try {
                runRegistry.markFailed(runId, message);
            } catch (ApiException ignored) {
            }
            runEventBus.publish(runId, event(runId, "error", message, Map.of(
                    "codexErrorInfo", params.path("error").path("codexErrorInfo").toString(),
                    "additionalDetails", firstPresent(textValue(params.path("error").path("additionalDetails")), "")
            )));
            removeListener(runId, this);
        }

        private void publishCompleted(JsonNode params) {
            activeTurns.remove(runId);
            try {
                runRegistry.markExited(runId, 0);
            } catch (ApiException ignored) {
            }
            runEventBus.publish(runId, event(runId, "done", "Codex turn completed", Map.of(
                    "turn", params.path("turn").toString()
            )));
            removeListener(runId, this);
        }

        private String firstPresent(String value, String fallback) {
            return value == null ? fallback : value;
        }
    }

    private record ActiveCodexTurn(String threadId, String turnId) {
    }
}
