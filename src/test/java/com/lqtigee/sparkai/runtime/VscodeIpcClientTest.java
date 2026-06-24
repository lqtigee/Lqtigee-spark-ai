package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VscodeIpcClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void requestInitializesClientAndUsesLengthPrefixedFrames() throws Exception {
        Path socket = tempDir.resolve("ipc-1000.sock");
        CompletableFuture<JsonNode> secondRequest = new CompletableFuture<>();
        Thread serverThread = startServer(socket, exchange -> {
            JsonNode initialize = exchange.readFrame();
            assertThat(initialize.path("method").asText()).isEqualTo("initialize");
            assertThat(initialize.path("version").asInt()).isZero();
            assertThat(initialize.path("params").path("clientType").asText()).isEqualTo("lqtigee-test");
            exchange.writeFrame(response(initialize, "initialize", "vscode-client", objectMapper.createObjectNode()
                    .put("clientId", "lqtigee-client")));

            JsonNode request = exchange.readFrame();
            secondRequest.complete(request);
            ObjectNode result = objectMapper.createObjectNode();
            result.put("revision", 9);
            exchange.writeFrame(response(request, "thread-follower-load-complete-history", "vscode-client", result));
        });

        try (VscodeIpcClient client = new VscodeIpcClient(objectMapper, socket, "lqtigee-test")) {
            ObjectNode params = objectMapper.createObjectNode();
            params.put("conversationId", "thread-1");

            VscodeIpcClient.VscodeIpcResponse response = client.request(
                    "thread-follower-load-complete-history",
                    params,
                    Duration.ofSeconds(5)
            );

            assertThat(response.handledByClientId()).isEqualTo("vscode-client");
            assertThat(response.result().path("revision").asInt()).isEqualTo(9);
            JsonNode sent = secondRequest.get(5, TimeUnit.SECONDS);
            assertThat(sent.path("sourceClientId").asText()).isEqualTo("lqtigee-client");
            assertThat(sent.path("version").asInt()).isEqualTo(1);
            assertThat(sent.path("params").path("conversationId").asText()).isEqualTo("thread-1");
        } finally {
            serverThread.join(5000);
        }
    }

    @Test
    void broadcastListenerReceivesRouterBroadcasts() throws Exception {
        Path socket = tempDir.resolve("ipc-1001.sock");
        CompletableFuture<JsonNode> broadcast = new CompletableFuture<>();
        Thread serverThread = startServer(socket, exchange -> {
            JsonNode initialize = exchange.readFrame();
            exchange.writeFrame(response(initialize, "initialize", "vscode-client", objectMapper.createObjectNode()
                    .put("clientId", "lqtigee-client")));

            ObjectNode frame = objectMapper.createObjectNode();
            frame.put("type", "broadcast");
            frame.put("method", "thread-stream-state-changed");
            frame.put("sourceClientId", "vscode-client");
            frame.put("version", 7);
            ObjectNode params = objectMapper.createObjectNode();
            params.put("conversationId", "thread-1");
            frame.set("params", params);
            exchange.writeFrame(frame);
        });

        try (VscodeIpcClient client = new VscodeIpcClient(objectMapper, socket, "lqtigee-test")) {
            client.addBroadcastListener(broadcast::complete);
            ObjectNode params = objectMapper.createObjectNode();
            params.put("conversationId", "thread-1");
            client.request("thread-follower-load-complete-history", params, Duration.ofMillis(200));
        } catch (Exception ignored) {
            // The request is only used to force connection; the test assertion is the broadcast.
        }

        assertThat(broadcast.get(5, TimeUnit.SECONDS).path("params").path("conversationId").asText()).isEqualTo("thread-1");
        serverThread.join(5000);
    }

    @Test
    void discoveryRequestsAreRejectedBecauseLqtigeeDoesNotOwnVscodeThreads() throws Exception {
        Path socket = tempDir.resolve("ipc-1002.sock");
        CompletableFuture<JsonNode> discoveryResponse = new CompletableFuture<>();
        Thread serverThread = startServer(socket, exchange -> {
            JsonNode initialize = exchange.readFrame();
            exchange.writeFrame(response(initialize, "initialize", "vscode-client", objectMapper.createObjectNode()
                    .put("clientId", "lqtigee-client")));

            ObjectNode discovery = objectMapper.createObjectNode();
            discovery.put("type", "client-discovery-request");
            discovery.put("requestId", "discover-1");
            ObjectNode request = objectMapper.createObjectNode();
            request.put("method", "thread-follower-start-turn");
            request.put("version", 1);
            request.set("params", objectMapper.createObjectNode().put("conversationId", "thread-1"));
            discovery.set("request", request);
            exchange.writeFrame(discovery);
            JsonNode frame = exchange.readFrame();
            while (!"client-discovery-response".equals(frame.path("type").asText())) {
                frame = exchange.readFrame();
            }
            discoveryResponse.complete(frame);
        });

        try (VscodeIpcClient client = new VscodeIpcClient(objectMapper, socket, "lqtigee-test")) {
            ObjectNode params = objectMapper.createObjectNode();
            params.put("conversationId", "thread-1");
            try {
                client.request("thread-follower-load-complete-history", params, Duration.ofMillis(200));
            } catch (Exception ignored) {
            }
        }

        JsonNode response = discoveryResponse.get(5, TimeUnit.SECONDS);
        assertThat(response.path("type").asText()).isEqualTo("client-discovery-response");
        assertThat(response.path("requestId").asText()).isEqualTo("discover-1");
        assertThat(response.path("response").path("canHandle").asBoolean()).isFalse();
        serverThread.join(5000);
    }

    private Thread startServer(Path socket, ServerScript script) throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        server.bind(UnixDomainSocketAddress.of(socket));
        Thread thread = new Thread(() -> {
            try (server; SocketChannel channel = server.accept()) {
                script.run(new Exchange(channel));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }, "vscode-ipc-test-server");
        thread.start();
        return thread;
    }

    private ObjectNode response(JsonNode request, String method, String handledByClientId, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "response");
        response.put("requestId", request.path("requestId").asText());
        response.put("resultType", "success");
        response.put("method", method);
        response.put("handledByClientId", handledByClientId);
        response.set("result", result);
        return response;
    }

    private interface ServerScript {
        void run(Exchange exchange) throws Exception;
    }

    private class Exchange {

        private final InputStream inputStream;
        private final OutputStream outputStream;

        Exchange(SocketChannel channel) {
            this.inputStream = Channels.newInputStream(channel);
            this.outputStream = Channels.newOutputStream(channel);
        }

        JsonNode readFrame() throws Exception {
            byte[] lengthBytes = inputStream.readNBytes(4);
            if (lengthBytes.length < 4) {
                throw new IllegalStateException("missing frame length");
            }
            int length = (lengthBytes[0] & 0xff)
                    | ((lengthBytes[1] & 0xff) << 8)
                    | ((lengthBytes[2] & 0xff) << 16)
                    | ((lengthBytes[3] & 0xff) << 24);
            byte[] payload = inputStream.readNBytes(length);
            if (payload.length < length) {
                throw new IllegalStateException("missing frame payload");
            }
            return objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
        }

        void writeFrame(JsonNode frame) throws Exception {
            byte[] payload = objectMapper.writeValueAsBytes(frame);
            outputStream.write((byte) (payload.length & 0xff));
            outputStream.write((byte) ((payload.length >>> 8) & 0xff));
            outputStream.write((byte) ((payload.length >>> 16) & 0xff));
            outputStream.write((byte) ((payload.length >>> 24) & 0xff));
            outputStream.write(payload);
            outputStream.flush();
        }
    }
}
