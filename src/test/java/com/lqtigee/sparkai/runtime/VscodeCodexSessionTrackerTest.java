package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VscodeCodexSessionTrackerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void snapshotBroadcastRegistersOwnerSessionAndStatus() {
        VscodeCodexSessionTracker tracker = newTracker();

        tracker.handleBroadcast(snapshot("conversation-1", "owner-1", "inProgress"));

        VscodeCodexSessionTracker.VscodeCodexSessionState state = tracker.find("conversation-1").orElseThrow();
        assertThat(state.ownerClientId()).isEqualTo("owner-1");
        assertThat(state.hostId()).isEqualTo("local");
        assertThat(state.revision()).isEqualTo(7);
        assertThat(state.title()).isEqualTo("Mobile control");
        assertThat(state.cwd()).isEqualTo("/home/lqtiger");
        assertThat(state.latestModel()).isEqualTo("gpt-5.5");
        assertThat(state.reasoningEffort()).isEqualTo("high");
        assertThat(state.runtimeStatus()).isEqualTo("running");
        assertThat(state.threadGoalStatus()).isEqualTo("active");
        assertThat(state.running()).isTrue();
    }

    @Test
    void patchBroadcastUpdatesRuntimeFieldsWithoutReplacingSnapshot() {
        VscodeCodexSessionTracker tracker = newTracker();
        tracker.handleBroadcast(snapshot("conversation-1", "owner-1", "completed"));

        tracker.handleBroadcast(patches("conversation-1", "owner-1",
                patch("replace", path("threadRuntimeStatus", "type"), objectMapper.createObjectNode().put("type", "idle")),
                patch("replace", path("latestReasoningEffort"), objectMapper.getNodeFactory().textNode("xhigh")),
                patch("replace", path("turns", "2", "status"), objectMapper.getNodeFactory().textNode("inProgress"))
        ));

        VscodeCodexSessionTracker.VscodeCodexSessionState state = tracker.find("conversation-1").orElseThrow();
        assertThat(state.runtimeStatus()).isEqualTo("idle");
        assertThat(state.reasoningEffort()).isEqualTo("xhigh");
        assertThat(state.running()).isTrue();
        assertThat(state.lastSnapshot()).isNotNull();
    }

    @Test
    void ignoresOtherBroadcastMethods() {
        VscodeCodexSessionTracker tracker = newTracker();
        ObjectNode frame = objectMapper.createObjectNode();
        frame.put("type", "broadcast");
        frame.put("method", "client-status-changed");

        tracker.handleBroadcast(frame);

        assertThat(tracker.find("conversation-1")).isEmpty();
    }

    private VscodeCodexSessionTracker newTracker() {
        return new VscodeCodexSessionTracker(
                objectMapper,
                new VscodeIpcClient(objectMapper, Path.of("/tmp/not-used.sock"), "lqtigee-test")
        );
    }

    private ObjectNode snapshot(String conversationId, String ownerClientId, String turnStatus) {
        ObjectNode frame = broadcast(conversationId, ownerClientId);
        ObjectNode change = objectMapper.createObjectNode();
        change.put("type", "snapshot");
        change.put("revision", 7);
        ObjectNode conversationState = objectMapper.createObjectNode();
        conversationState.put("title", "Mobile control");
        conversationState.put("cwd", "/home/lqtiger");
        conversationState.put("latestModel", "gpt-5.5");
        conversationState.put("latestReasoningEffort", "high");
        conversationState.set("threadRuntimeStatus", objectMapper.createObjectNode().put("type", "running"));
        conversationState.set("threadGoal", objectMapper.createObjectNode().put("status", "active"));
        ArrayNode turns = objectMapper.createArrayNode();
        turns.add(objectMapper.createObjectNode().put("status", turnStatus));
        conversationState.set("turns", turns);
        change.set("conversationState", conversationState);
        frame.withObject("/params").set("change", change);
        return frame;
    }

    private ObjectNode patches(String conversationId, String ownerClientId, ObjectNode... patches) {
        ObjectNode frame = broadcast(conversationId, ownerClientId);
        ObjectNode change = objectMapper.createObjectNode();
        change.put("type", "patches");
        change.put("baseRevision", 7);
        change.put("revision", 8);
        ArrayNode patchArray = objectMapper.createArrayNode();
        for (ObjectNode patch : patches) {
            patchArray.add(patch);
        }
        change.set("patches", patchArray);
        frame.withObject("/params").set("change", change);
        return frame;
    }

    private ObjectNode broadcast(String conversationId, String ownerClientId) {
        ObjectNode frame = objectMapper.createObjectNode();
        frame.put("type", "broadcast");
        frame.put("method", "thread-stream-state-changed");
        frame.put("sourceClientId", ownerClientId);
        frame.put("version", 7);
        ObjectNode params = objectMapper.createObjectNode();
        params.put("conversationId", conversationId);
        params.put("hostId", "local");
        frame.set("params", params);
        return frame;
    }

    private ObjectNode patch(String op, ArrayNode path, com.fasterxml.jackson.databind.JsonNode value) {
        ObjectNode patch = objectMapper.createObjectNode();
        patch.put("op", op);
        patch.set("path", path);
        patch.set("value", value);
        return patch;
    }

    private ArrayNode path(String... parts) {
        ArrayNode path = objectMapper.createArrayNode();
        for (String part : parts) {
            path.add(part);
        }
        return path;
    }
}
