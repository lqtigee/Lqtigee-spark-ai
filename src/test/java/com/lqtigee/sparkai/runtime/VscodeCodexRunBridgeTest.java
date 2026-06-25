package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lqtigee.sparkai.dto.AgentSource;
import com.lqtigee.sparkai.dto.CodexRunOptionsDto;
import com.lqtigee.sparkai.dto.CommandMode;
import com.lqtigee.sparkai.dto.ModelDto;
import com.lqtigee.sparkai.dto.RemoteSessionDto;
import com.lqtigee.sparkai.dto.RunEventDto;
import com.lqtigee.sparkai.dto.SessionStatus;
import com.lqtigee.sparkai.dto.StartRunRequest;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VscodeCodexRunBridgeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void startSendsThreadFollowerStartTurnForOwnerSession() {
        RecordingVscodeIpcClient ipcClient = new RecordingVscodeIpcClient(objectMapper);
        VscodeCodexSessionTracker tracker = new VscodeCodexSessionTracker(objectMapper, ipcClient);
        tracker.handleBroadcast(snapshot("session-1", "owner-1", false));
        CapturingRunEventBus eventBus = new CapturingRunEventBus();
        VscodeCodexRunBridge bridge = new VscodeCodexRunBridge(objectMapper, ipcClient, tracker, eventBus, new RunRegistry());

        bridge.start("run-1", request("hello"), session(), model());

        assertThat(ipcClient.requests()).extracting(RecordedRequest::method).contains(
                "thread-follower-load-complete-history",
                "thread-follower-start-turn"
        );
        RecordedRequest startTurn = ipcClient.requests().stream()
                .filter(request -> request.method().equals("thread-follower-start-turn"))
                .findFirst()
                .orElseThrow();
        JsonNode turnStartParams = startTurn.params().path("turnStartParams");
        assertThat(startTurn.params().path("conversationId").asText()).isEqualTo("session-1");
        assertThat(turnStartParams.path("input").get(0).path("text").asText()).isEqualTo("hello");
        assertThat(turnStartParams.path("cwd").asText()).isEqualTo("/home/lqtiger/project");
        assertThat(turnStartParams.path("config").path("model").asText()).isEqualTo("gpt-5.5");
        assertThat(turnStartParams.path("config").path("model_reasoning_effort").asText()).isEqualTo("xhigh");
        assertThat(turnStartParams.path("permissions").path("sandboxPolicy").path("type").asText()).isEqualTo("workspaceWrite");
        assertThat(eventBus.events()).extracting(RunEventDto::type).contains("status");
    }

    @Test
    void startSendsSteerTurnWhenTrackerMarksSessionRunning() {
        RecordingVscodeIpcClient ipcClient = new RecordingVscodeIpcClient(objectMapper);
        VscodeCodexSessionTracker tracker = new VscodeCodexSessionTracker(objectMapper, ipcClient);
        tracker.handleBroadcast(snapshot("session-1", "owner-1", true));
        VscodeCodexRunBridge bridge = new VscodeCodexRunBridge(objectMapper, ipcClient, tracker, new CapturingRunEventBus(), new RunRegistry());

        bridge.start("run-1", request("continue"), session(), model());

        RecordedRequest steer = ipcClient.requests().stream()
                .filter(request -> request.method().equals("thread-follower-steer-turn"))
                .findFirst()
                .orElseThrow();
        assertThat(steer.params().path("conversationId").asText()).isEqualTo("session-1");
        assertThat(steer.params().path("input").get(0).path("text").asText()).isEqualTo("continue");
        assertThat(steer.params().path("restoreMessage").path("text").asText()).isEqualTo("continue");
    }

    @Test
    void startFallsBackToNewTurnWhenSteerFindsEndedTurn() {
        RecordingVscodeIpcClient ipcClient = new RecordingVscodeIpcClient(objectMapper);
        ipcClient.failSteerTurnInactive();
        VscodeCodexSessionTracker tracker = new VscodeCodexSessionTracker(objectMapper, ipcClient);
        tracker.handleBroadcast(snapshot("session-1", "owner-1", true));
        VscodeCodexRunBridge bridge = new VscodeCodexRunBridge(objectMapper, ipcClient, tracker, new CapturingRunEventBus(), new RunRegistry());

        bridge.start("run-1", request("continue"), session(), model());

        assertThat(ipcClient.requests()).extracting(RecordedRequest::method).containsSubsequence(
                "thread-follower-steer-turn",
                "thread-follower-start-turn"
        );
    }

    @Test
    void startFailsFastWhenVscodeOwnerSessionIsNotOpen() {
        RecordingVscodeIpcClient ipcClient = new RecordingVscodeIpcClient(objectMapper);
        ipcClient.fail("thread-follower-load-complete-history");
        VscodeCodexSessionTracker tracker = new VscodeCodexSessionTracker(objectMapper, ipcClient);
        VscodeCodexRunBridge bridge = new VscodeCodexRunBridge(objectMapper, ipcClient, tracker, new CapturingRunEventBus(), new RunRegistry());

        assertThatThrownBy(() -> bridge.start("run-1", request("hello"), session(), model()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo(ErrorCode.VSCODE_CODEX_SESSION_NOT_OPEN));
    }

    @Test
    void sessionBroadcastPatchPublishesAssistantEventAndTerminalDone() {
        RecordingVscodeIpcClient ipcClient = new RecordingVscodeIpcClient(objectMapper);
        VscodeCodexSessionTracker tracker = new VscodeCodexSessionTracker(objectMapper, ipcClient);
        tracker.handleBroadcast(snapshot("session-1", "owner-1", false));
        CapturingRunEventBus eventBus = new CapturingRunEventBus();
        VscodeCodexRunBridge bridge = new VscodeCodexRunBridge(objectMapper, ipcClient, tracker, eventBus, new RunRegistry());
        bridge.start("run-1", request("hello"), session(), model());

        tracker.handleBroadcast(patchFrame("session-1", patch(
                path("turns", "0", "items", "0"),
                objectMapper.createObjectNode()
                        .put("type", "assistantMessage")
                        .put("text", "answer")
        )));
        tracker.handleBroadcast(patchFrame("session-1", patch(
                path("turns", "0", "status"),
                objectMapper.getNodeFactory().textNode("completed")
        )));

        assertThat(eventBus.events()).extracting(RunEventDto::type).contains("assistant", "done");
    }

    private ObjectNode snapshot(String conversationId, String ownerClientId, boolean running) {
        ObjectNode frame = objectMapper.createObjectNode();
        frame.put("type", "broadcast");
        frame.put("method", "thread-stream-state-changed");
        frame.put("sourceClientId", ownerClientId);
        ObjectNode params = objectMapper.createObjectNode();
        params.put("conversationId", conversationId);
        params.put("hostId", "local");
        ObjectNode change = objectMapper.createObjectNode();
        change.put("type", "snapshot");
        change.put("revision", 7);
        ObjectNode state = objectMapper.createObjectNode();
        state.put("title", "Session");
        state.put("cwd", "/home/lqtiger/project");
        state.put("latestModel", "gpt-5.5");
        state.put("latestReasoningEffort", "xhigh");
        state.set("threadRuntimeStatus", objectMapper.createObjectNode().put("type", running ? "running" : "idle"));
        state.set("threadGoal", objectMapper.createObjectNode().put("status", "active"));
        state.set("turns", objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                .put("status", running ? "inProgress" : "completed")));
        change.set("conversationState", state);
        params.set("change", change);
        frame.set("params", params);
        return frame;
    }

    private ObjectNode patchFrame(String conversationId, ObjectNode patch) {
        ObjectNode frame = objectMapper.createObjectNode();
        frame.put("type", "broadcast");
        frame.put("method", "thread-stream-state-changed");
        frame.put("sourceClientId", "owner-1");
        ObjectNode params = objectMapper.createObjectNode();
        params.put("conversationId", conversationId);
        params.put("hostId", "local");
        ObjectNode change = objectMapper.createObjectNode();
        change.put("type", "patches");
        change.put("revision", 8);
        change.set("patches", objectMapper.createArrayNode().add(patch));
        params.set("change", change);
        frame.set("params", params);
        return frame;
    }

    private ObjectNode patch(JsonNode path, JsonNode value) {
        ObjectNode patch = objectMapper.createObjectNode();
        patch.put("op", "add");
        patch.set("path", path);
        patch.set("value", value);
        return patch;
    }

    private JsonNode path(String... parts) {
        com.fasterxml.jackson.databind.node.ArrayNode path = objectMapper.createArrayNode();
        for (String part : parts) {
            path.add(part);
        }
        return path;
    }

    private StartRunRequest request(String prompt) {
        return new StartRunRequest(
                "session-1",
                AgentSource.CODEX,
                "gpt-5.5",
                CommandMode.EDIT,
                prompt,
                false,
                new CodexRunOptionsDto(
                        null,
                        null,
                        "workspace-write",
                        "on-request",
                        null,
                        null,
                        List.of(new CodexRunOptionsDto.ConfigOverrideDto("model_reasoning_effort", "xhigh")),
                        null
                )
        );
    }

    private RemoteSessionDto session() {
        return new RemoteSessionDto(
                "session-1",
                AgentSource.CODEX,
                "Session",
                "/home/lqtiger/project",
                "gpt-5.5",
                SessionStatus.ACTIVE,
                Instant.parse("2026-06-20T00:00:00Z"),
                "",
                "/home/lqtiger/.codex/sessions/session-1.jsonl"
        );
    }

    private ModelDto model() {
        return new ModelDto("gpt-5.5", "GPT-5.5", "gpt-5.5", List.of(AgentSource.CODEX), true);
    }

    private record RecordedRequest(String method, ObjectNode params) {
    }

    private static class RecordingVscodeIpcClient extends VscodeIpcClient {

        private final ObjectMapper objectMapper;
        private final List<RecordedRequest> requests = new ArrayList<>();
        private String failingMethod;
        private boolean failSteerTurnInactive;

        RecordingVscodeIpcClient(ObjectMapper objectMapper) {
            super(objectMapper, Path.of("/tmp/not-used.sock"), "test");
            this.objectMapper = objectMapper;
        }

        @Override
        public VscodeIpcResponse request(String method, ObjectNode params, Duration timeout) {
            requests.add(new RecordedRequest(method, params.deepCopy()));
            if ("thread-follower-steer-turn".equals(method) && failSteerTurnInactive) {
                throw new ApiException(
                        ErrorCode.VSCODE_IPC_REQUEST_FAILED,
                        org.springframework.http.HttpStatus.FAILED_DEPENDENCY,
                        "VSCode IPC returned an error",
                        "SteerTurnInactiveError: Cannot steer conversation session-1 because its active turn already ended"
                );
            }
            if (method.equals(failingMethod)) {
                throw new ApiException(
                        ErrorCode.VSCODE_IPC_REQUEST_FAILED,
                        org.springframework.http.HttpStatus.FAILED_DEPENDENCY,
                        "failed",
                        method
                );
            }
            ObjectNode result = objectMapper.createObjectNode();
            if ("thread-follower-load-complete-history".equals(method)) {
                result.put("revision", 7);
            } else {
                result.put("ok", true);
            }
            return new VscodeIpcResponse(method, "owner-1", result, result);
        }

        void fail(String method) {
            this.failingMethod = method;
        }

        void failSteerTurnInactive() {
            this.failSteerTurnInactive = true;
        }

        List<RecordedRequest> requests() {
            return requests;
        }
    }

    private static class CapturingRunEventBus extends RunEventBus {

        private final List<RunEventDto> events = new ArrayList<>();

        @Override
        public void publish(String runId, RunEventDto event) {
            events.add(event);
            super.publish(runId, event);
        }

        List<RunEventDto> events() {
            return events;
        }
    }
}
