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
import com.lqtigee.sparkai.error.ErrorCode;
import com.lqtigee.sparkai.runtime.VscodeCodexSessionTracker.VscodeCodexSessionState;
import com.lqtigee.sparkai.runtime.VscodeCodexSessionTracker.VscodeCodexSessionSubscription;
import com.lqtigee.sparkai.runtime.VscodeIpcClient.VscodeIpcResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;

public class VscodeCodexRunBridge {

    private static final Duration VSCODE_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final VscodeIpcClient ipcClient;
    private final VscodeCodexSessionTracker sessionTracker;
    private final RunEventBus runEventBus;
    private final RunRegistry runRegistry;
    private final ConcurrentMap<String, ActiveVscodeCodexRun> activeRuns = new ConcurrentHashMap<>();

    public VscodeCodexRunBridge(
            ObjectMapper objectMapper,
            VscodeIpcClient ipcClient,
            VscodeCodexSessionTracker sessionTracker,
            RunEventBus runEventBus,
            RunRegistry runRegistry
    ) {
        this.objectMapper = objectMapper;
        this.ipcClient = ipcClient;
        this.sessionTracker = sessionTracker;
        this.runEventBus = runEventBus;
        this.runRegistry = runRegistry;
    }

    public void start(String runId, StartRunRequest request, RemoteSessionDto session, ModelDto model) {
        VscodeCodexSessionState sessionState = requireOwnerSession(request.sessionId());
        VscodeCodexSessionSubscription subscription = sessionTracker.subscribe(
                request.sessionId(),
                frame -> publishBroadcast(runId, frame)
        );
        activeRuns.put(runId, new ActiveVscodeCodexRun(request.sessionId(), subscription));
        try {
            VscodeIpcResponse response = sessionState.running()
                    ? steerRunningTurn(request, sessionState)
                    : startNewTurn(request, session, model, sessionState);
            runEventBus.publish(runId, event(runId, "status", "VSCode Codex request accepted", Map.of(
                    "conversationId", request.sessionId(),
                    "handledByClientId", nullToEmpty(response.handledByClientId()),
                    "transport", "vscode-ipc"
            )));
        } catch (ApiException exception) {
            cleanupRun(runId);
            runEventBus.publish(runId, event(runId, "error", exception.getMessage(), Map.of(
                    "detail", nullToEmpty(exception.detail()),
                    "transport", "vscode-ipc"
            )));
            throw exception;
        }
    }

    public boolean stop(String runId) {
        ActiveVscodeCodexRun activeRun = activeRuns.remove(runId);
        if (activeRun == null) {
            return false;
        }
        activeRun.subscription().close();
        ObjectNode params = objectMapper.createObjectNode();
        params.put("conversationId", activeRun.conversationId());
        try {
            VscodeIpcResponse response = ipcClient.request("thread-follower-interrupt-turn", params, VSCODE_REQUEST_TIMEOUT);
            runRegistry.markStopped(runId);
            runEventBus.publish(runId, event(runId, "stopped", "VSCode Codex turn interrupted", Map.of(
                    "conversationId", activeRun.conversationId(),
                    "handledByClientId", nullToEmpty(response.handledByClientId()),
                    "transport", "vscode-ipc"
            )));
            return true;
        } catch (ApiException exception) {
            runRegistry.markFailed(runId, exception.getMessage());
            runEventBus.publish(runId, event(runId, "error", exception.getMessage(), Map.of(
                    "conversationId", activeRun.conversationId(),
                    "detail", nullToEmpty(exception.detail()),
                    "transport", "vscode-ipc"
            )));
            throw exception;
        }
    }

    private VscodeCodexSessionState requireOwnerSession(String conversationId) {
        try {
            ObjectNode params = objectMapper.createObjectNode();
            params.put("conversationId", conversationId);
            ipcClient.request("thread-follower-load-complete-history", params, Duration.ofSeconds(10));
            return waitForTrackedSession(conversationId).orElseThrow(() -> new ApiException(
                    ErrorCode.VSCODE_CODEX_SESSION_NOT_OPEN,
                    HttpStatus.FAILED_DEPENDENCY,
                    "VSCode Codex session state was not broadcast",
                    conversationId
            ));
        } catch (ApiException exception) {
            throw new ApiException(
                    ErrorCode.VSCODE_CODEX_SESSION_NOT_OPEN,
                    HttpStatus.FAILED_DEPENDENCY,
                    "VSCode Codex session is not open as owner",
                    conversationId + ": " + exception.detail()
            );
        }
    }

    private Optional<VscodeCodexSessionState> waitForTrackedSession(String conversationId) {
        Instant deadline = Instant.now().plusMillis(1500L);
        Optional<VscodeCodexSessionState> sessionState = sessionTracker.find(conversationId);
        while (sessionState.isEmpty() && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
            sessionState = sessionTracker.find(conversationId);
        }
        return sessionState;
    }

    private VscodeIpcResponse startNewTurn(
            StartRunRequest request,
            RemoteSessionDto session,
            ModelDto model,
            VscodeCodexSessionState sessionState
    ) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("conversationId", request.sessionId());
        params.set("turnStartParams", turnStartParams(request, session, model, sessionState));
        return ipcClient.request("thread-follower-start-turn", params, VSCODE_REQUEST_TIMEOUT);
    }

    private VscodeIpcResponse steerRunningTurn(StartRunRequest request, VscodeCodexSessionState sessionState) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("conversationId", request.sessionId());
        params.put("clientUserMessageId", UUID.randomUUID().toString());
        params.set("input", input(request));
        params.putNull("serviceTier");
        params.set("attachments", objectMapper.createArrayNode());
        params.set("restoreMessage", restoreMessage(request, sessionState));
        return ipcClient.request("thread-follower-steer-turn", params, VSCODE_REQUEST_TIMEOUT);
    }

    private ObjectNode turnStartParams(
            StartRunRequest request,
            RemoteSessionDto session,
            ModelDto model,
            VscodeCodexSessionState sessionState
    ) {
        String cwd = firstPresent(sessionState.cwd(), session.workspace());
        ObjectNode params = objectMapper.createObjectNode();
        params.set("input", input(request));
        params.set("commentAttachments", objectMapper.createArrayNode());
        ArrayNode workspaceRoots = objectMapper.createArrayNode();
        workspaceRoots.add(cwd);
        params.set("workspaceRoots", workspaceRoots);
        params.putNull("collaborationMode");
        params.putNull("serviceTier");
        params.put("useAppServerPermissionDefault", true);
        params.put("cwd", cwd);
        params.set("attachments", objectMapper.createArrayNode());
        params.put("workspaceKind", "project");
        ObjectNode config = objectMapper.createObjectNode();
        config.put("model", model.commandModelName());
        String effort = reasoningEffort(request, sessionState);
        if (effort != null) {
            config.put("model_reasoning_effort", effort);
        }
        params.set("config", config);
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("source", "lqtigee");
        metadata.put("mode", request.mode().name());
        params.set("responsesapiClientMetadata", metadata);
        params.set("permissions", permissions(request, cwd));
        params.put("approvalsReviewer", approvalsReviewer(request));
        return params;
    }

    private ObjectNode restoreMessage(StartRunRequest request, VscodeCodexSessionState sessionState) {
        String cwd = firstPresent(sessionState.cwd(), "/");
        ObjectNode restoreMessage = objectMapper.createObjectNode();
        restoreMessage.put("id", UUID.randomUUID().toString());
        restoreMessage.put("text", request.prompt());
        restoreMessage.set("context", promptContext(request, cwd));
        restoreMessage.put("cwd", cwd);
        restoreMessage.put("createdAt", Instant.now().toEpochMilli());
        return restoreMessage;
    }

    private ArrayNode input(StartRunRequest request) {
        ArrayNode input = objectMapper.createArrayNode();
        ObjectNode text = objectMapper.createObjectNode();
        text.put("type", "text");
        text.put("text", request.prompt());
        text.set("text_elements", objectMapper.createArrayNode());
        input.add(text);
        return input;
    }

    private ObjectNode promptContext(StartRunRequest request, String cwd) {
        ObjectNode context = objectMapper.createObjectNode();
        context.put("prompt", request.prompt());
        context.set("addedFiles", objectMapper.createArrayNode());
        context.set("fileAttachments", objectMapper.createArrayNode());
        context.putNull("ideContext");
        context.set("imageAttachments", objectMapper.createArrayNode());
        ArrayNode workspaceRoots = objectMapper.createArrayNode();
        workspaceRoots.add(cwd);
        context.set("workspaceRoots", workspaceRoots);
        return context;
    }

    private ObjectNode permissions(StartRunRequest request, String cwd) {
        ObjectNode permissions = objectMapper.createObjectNode();
        permissions.put("approvalPolicy", approvalPolicy(request));
        permissions.put("approvalsReviewer", approvalsReviewer(request));
        permissions.set("sandboxPolicy", sandboxPolicy(request));
        ArrayNode roots = objectMapper.createArrayNode();
        roots.add(cwd);
        permissions.set("runtimeWorkspaceRoots", roots);
        return permissions;
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

    private String approvalPolicy(StartRunRequest request) {
        if (request.codexOptions() != null && nonBlank(request.codexOptions().approvalPolicy())) {
            return request.codexOptions().approvalPolicy();
        }
        return "on-request";
    }

    private String approvalsReviewer(StartRunRequest request) {
        if ("never".equals(approvalPolicy(request))) {
            return "auto";
        }
        return "user";
    }

    private String reasoningEffort(StartRunRequest request, VscodeCodexSessionState sessionState) {
        CodexRunOptionsDto options = request.codexOptions();
        if (options != null && options.configOverrides() != null) {
            String requested = options.configOverrides().stream()
                    .filter(override -> "model_reasoning_effort".equals(override.key()))
                    .map(CodexRunOptionsDto.ConfigOverrideDto::value)
                    .filter(this::nonBlank)
                    .findFirst()
                    .orElse(null);
            if (requested != null) {
                return requested;
            }
        }
        return sessionState.reasoningEffort();
    }

    private void publishBroadcast(String runId, JsonNode frame) {
        JsonNode change = frame.path("params").path("change");
        if ("snapshot".equals(textValue(change.path("type")))) {
            publishSnapshot(runId, change.path("conversationState"));
            return;
        }
        JsonNode patches = change.path("patches");
        if (!patches.isArray()) {
            return;
        }
        for (JsonNode patch : patches) {
            publishPatch(runId, patch);
        }
    }

    private void publishSnapshot(String runId, JsonNode conversationState) {
        JsonNode turns = conversationState.path("turns");
        if (!turns.isArray() || turns.isEmpty()) {
            return;
        }
        JsonNode latestTurn = turns.get(turns.size() - 1);
        String status = textValue(latestTurn.path("status"));
        if (status != null) {
            runEventBus.publish(runId, event(runId, "status", "VSCode Codex turn status: " + status, Map.of(
                    "transport", "vscode-ipc",
                    "status", status
            )));
            if ("completed".equals(status)) {
                markDone(runId, latestTurn);
            } else if ("failed".equals(status)) {
                markFailed(runId, "VSCode Codex turn failed", latestTurn);
            }
        }
    }

    private void publishPatch(String runId, JsonNode patch) {
        JsonNode path = patch.path("path");
        JsonNode value = patch.path("value");
        if (isTurnStatusPath(path)) {
            String status = textValue(value);
            if (status == null) {
                return;
            }
            runEventBus.publish(runId, event(runId, "status", "VSCode Codex turn status: " + status, Map.of(
                    "transport", "vscode-ipc",
                    "status", status
            )));
            if ("completed".equals(status)) {
                markDone(runId, value);
            } else if ("failed".equals(status)) {
                markFailed(runId, "VSCode Codex turn failed", value);
            }
            return;
        }
        if (!isTurnItemPath(path)) {
            return;
        }
        String itemType = textValue(value.path("type"));
        if (itemType == null) {
            return;
        }
        if ("assistantMessage".equals(itemType) || "assistant_message".equals(itemType)) {
            String text = firstPresent(textValue(value.path("text")), textValue(value.path("message")));
            if (text != null) {
                runEventBus.publish(runId, event(runId, "assistant", text, Map.of(
                        "transport", "vscode-ipc",
                        "itemType", itemType
                )));
            }
        } else if (itemType.contains("command") || itemType.contains("tool")) {
            String message = firstPresent(textValue(value.path("command")), value.toString());
            runEventBus.publish(runId, event(runId, "tool", message, Map.of(
                    "transport", "vscode-ipc",
                    "itemType", itemType
            )));
        }
    }

    private void markDone(String runId, JsonNode turn) {
        cleanupRun(runId);
        try {
            runRegistry.markExited(runId, 0);
        } catch (ApiException ignored) {
        }
        runEventBus.publish(runId, event(runId, "done", "VSCode Codex turn completed", Map.of(
                "transport", "vscode-ipc",
                "turn", turn.toString()
        )));
    }

    private void markFailed(String runId, String message, JsonNode turn) {
        cleanupRun(runId);
        try {
            runRegistry.markFailed(runId, message);
        } catch (ApiException ignored) {
        }
        runEventBus.publish(runId, event(runId, "error", message, Map.of(
                "transport", "vscode-ipc",
                "turn", turn.toString()
        )));
    }

    private void cleanupRun(String runId) {
        ActiveVscodeCodexRun activeRun = activeRuns.remove(runId);
        if (activeRun != null) {
            activeRun.subscription().close();
        }
    }

    private boolean isTurnStatusPath(JsonNode path) {
        return path.size() >= 3 && "turns".equals(path.get(0).asText(null)) && "status".equals(path.get(2).asText(null));
    }

    private boolean isTurnItemPath(JsonNode path) {
        return path.size() >= 3 && "turns".equals(path.get(0).asText(null)) && "items".equals(path.get(2).asText(null));
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

    private String firstPresent(String first, String second) {
        return first == null ? second : first;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record ActiveVscodeCodexRun(
            String conversationId,
            VscodeCodexSessionSubscription subscription
    ) {
    }
}
