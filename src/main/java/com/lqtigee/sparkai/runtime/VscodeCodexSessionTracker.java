package com.lqtigee.sparkai.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class VscodeCodexSessionTracker {

    private static final String THREAD_STREAM_STATE_CHANGED = "thread-stream-state-changed";

    private final ObjectMapper objectMapper;
    private final VscodeIpcClient ipcClient;
    private final ConcurrentMap<String, VscodeCodexSessionState> sessions = new ConcurrentHashMap<>();

    public VscodeCodexSessionTracker(ObjectMapper objectMapper, VscodeIpcClient ipcClient) {
        this.objectMapper = objectMapper;
        this.ipcClient = ipcClient;
        this.ipcClient.addBroadcastListener(this::handleBroadcast);
    }

    public Optional<VscodeCodexSessionState> find(String conversationId) {
        return Optional.ofNullable(sessions.get(conversationId));
    }

    public boolean hasOwnerSession(String conversationId) {
        return sessions.containsKey(conversationId);
    }

    void handleBroadcast(JsonNode frame) {
        if (!THREAD_STREAM_STATE_CHANGED.equals(textValue(frame.path("method")))) {
            return;
        }
        JsonNode params = frame.path("params");
        String conversationId = textValue(params.path("conversationId"));
        if (conversationId == null) {
            return;
        }
        String sourceClientId = textValue(frame.path("sourceClientId"));
        String hostId = textValue(params.path("hostId"));
        JsonNode change = params.path("change");
        String changeType = textValue(change.path("type"));
        long revision = change.path("revision").asLong(0L);
        sessions.compute(conversationId, (id, current) -> {
            VscodeCodexSessionState next = current == null
                    ? VscodeCodexSessionState.empty(id)
                    : current;
            next = next.withOwner(sourceClientId, hostId, revision, Instant.now());
            if ("snapshot".equals(changeType)) {
                return applySnapshot(next, change.path("conversationState"));
            }
            if ("patches".equals(changeType)) {
                return applyPatches(next, change.path("patches"));
            }
            return next;
        });
    }

    private VscodeCodexSessionState applySnapshot(VscodeCodexSessionState current, JsonNode conversationState) {
        if (conversationState == null || conversationState.isMissingNode() || conversationState.isNull()) {
            return current;
        }
        return current.withSnapshot(
                textValue(conversationState.path("title")),
                textValue(conversationState.path("cwd")),
                textValue(conversationState.path("latestModel")),
                textValue(conversationState.path("latestReasoningEffort")),
                textValue(conversationState.path("threadRuntimeStatus").path("type")),
                textValue(conversationState.path("threadGoal").path("status")),
                hasInProgressTurn(conversationState.path("turns")),
                conversationState.deepCopy()
        );
    }

    private VscodeCodexSessionState applyPatches(VscodeCodexSessionState current, JsonNode patches) {
        if (!patches.isArray()) {
            return current;
        }
        VscodeCodexSessionState next = current;
        for (JsonNode patch : patches) {
            JsonNode path = patch.path("path");
            JsonNode value = patch.path("value");
            if (isRuntimeStatusPath(path)) {
                next = next.withRuntimeStatus(textValue(value.path("type")));
            } else if (isLatestModelPath(path)) {
                next = next.withLatestModel(textValue(value));
            } else if (isReasoningEffortPath(path)) {
                next = next.withReasoningEffort(textValue(value));
            } else if (isThreadGoalStatusPath(path)) {
                next = next.withThreadGoalStatus(textValue(value));
            } else if (isTurnStatusPath(path)) {
                next = next.withRunning("inProgress".equals(textValue(value)));
            } else if (isTurnItemAddPath(path)) {
                next = next.withRunning(true);
            }
        }
        return next;
    }

    private boolean hasInProgressTurn(JsonNode turns) {
        if (!turns.isArray()) {
            return false;
        }
        for (JsonNode turn : turns) {
            if ("inProgress".equals(textValue(turn.path("status")))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRuntimeStatusPath(JsonNode path) {
        return pathStarts(path, "threadRuntimeStatus");
    }

    private boolean isLatestModelPath(JsonNode path) {
        return pathEquals(path, "latestModel");
    }

    private boolean isReasoningEffortPath(JsonNode path) {
        return pathEquals(path, "latestReasoningEffort");
    }

    private boolean isThreadGoalStatusPath(JsonNode path) {
        return pathEquals(path, "threadGoal", "status");
    }

    private boolean isTurnStatusPath(JsonNode path) {
        return path.size() >= 3 && "turns".equals(path.get(0).asText(null)) && "status".equals(path.get(2).asText(null));
    }

    private boolean isTurnItemAddPath(JsonNode path) {
        return path.size() >= 3 && "turns".equals(path.get(0).asText(null)) && "items".equals(path.get(2).asText(null));
    }

    private boolean pathStarts(JsonNode path, String first) {
        return path.isArray() && path.size() >= 1 && first.equals(path.get(0).asText(null));
    }

    private boolean pathEquals(JsonNode path, String... parts) {
        if (!path.isArray() || path.size() != parts.length) {
            return false;
        }
        for (int index = 0; index < parts.length; index++) {
            if (!parts[index].equals(path.get(index).asText(null))) {
                return false;
            }
        }
        return true;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    public record VscodeCodexSessionState(
            String conversationId,
            String ownerClientId,
            String hostId,
            long revision,
            String title,
            String cwd,
            String latestModel,
            String reasoningEffort,
            String runtimeStatus,
            String threadGoalStatus,
            boolean running,
            JsonNode lastSnapshot,
            Instant updatedAt
    ) {
        static VscodeCodexSessionState empty(String conversationId) {
            return new VscodeCodexSessionState(
                    conversationId,
                    null,
                    null,
                    0L,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    Instant.EPOCH
            );
        }

        VscodeCodexSessionState withOwner(String ownerClientId, String hostId, long revision, Instant updatedAt) {
            return new VscodeCodexSessionState(
                    conversationId,
                    ownerClientId,
                    hostId,
                    revision,
                    title,
                    cwd,
                    latestModel,
                    reasoningEffort,
                    runtimeStatus,
                    threadGoalStatus,
                    running,
                    lastSnapshot,
                    updatedAt
            );
        }

        VscodeCodexSessionState withSnapshot(
                String title,
                String cwd,
                String latestModel,
                String reasoningEffort,
                String runtimeStatus,
                String threadGoalStatus,
                boolean running,
                JsonNode lastSnapshot
        ) {
            return new VscodeCodexSessionState(
                    conversationId,
                    ownerClientId,
                    hostId,
                    revision,
                    title,
                    cwd,
                    latestModel,
                    reasoningEffort,
                    runtimeStatus,
                    threadGoalStatus,
                    running,
                    lastSnapshot,
                    updatedAt
            );
        }

        VscodeCodexSessionState withRuntimeStatus(String runtimeStatus) {
            return new VscodeCodexSessionState(
                    conversationId,
                    ownerClientId,
                    hostId,
                    revision,
                    title,
                    cwd,
                    latestModel,
                    reasoningEffort,
                    runtimeStatus,
                    threadGoalStatus,
                    running,
                    lastSnapshot,
                    updatedAt
            );
        }

        VscodeCodexSessionState withLatestModel(String latestModel) {
            return new VscodeCodexSessionState(
                    conversationId,
                    ownerClientId,
                    hostId,
                    revision,
                    title,
                    cwd,
                    latestModel,
                    reasoningEffort,
                    runtimeStatus,
                    threadGoalStatus,
                    running,
                    lastSnapshot,
                    updatedAt
            );
        }

        VscodeCodexSessionState withReasoningEffort(String reasoningEffort) {
            return new VscodeCodexSessionState(
                    conversationId,
                    ownerClientId,
                    hostId,
                    revision,
                    title,
                    cwd,
                    latestModel,
                    reasoningEffort,
                    runtimeStatus,
                    threadGoalStatus,
                    running,
                    lastSnapshot,
                    updatedAt
            );
        }

        VscodeCodexSessionState withThreadGoalStatus(String threadGoalStatus) {
            return new VscodeCodexSessionState(
                    conversationId,
                    ownerClientId,
                    hostId,
                    revision,
                    title,
                    cwd,
                    latestModel,
                    reasoningEffort,
                    runtimeStatus,
                    threadGoalStatus,
                    running,
                    lastSnapshot,
                    updatedAt
            );
        }

        VscodeCodexSessionState withRunning(boolean running) {
            return new VscodeCodexSessionState(
                    conversationId,
                    ownerClientId,
                    hostId,
                    revision,
                    title,
                    cwd,
                    latestModel,
                    reasoningEffort,
                    runtimeStatus,
                    threadGoalStatus,
                    running,
                    lastSnapshot,
                    updatedAt
            );
        }
    }
}
