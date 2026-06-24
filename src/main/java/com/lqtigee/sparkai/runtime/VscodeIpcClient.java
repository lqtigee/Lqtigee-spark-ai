package com.lqtigee.sparkai.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;

public class VscodeIpcClient implements AutoCloseable {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_FRAME_BYTES = 256 * 1024 * 1024;
    private static final String INITIALIZING_CLIENT_ID = "initializing-client";
    private static final Map<String, Integer> METHOD_VERSIONS = Map.ofEntries(
            Map.entry("initialize", 0),
            Map.entry("thread-follower-start-turn", 1),
            Map.entry("thread-follower-load-complete-history", 1),
            Map.entry("thread-follower-compact-thread", 1),
            Map.entry("thread-follower-steer-turn", 1),
            Map.entry("thread-follower-interrupt-turn", 2),
            Map.entry("thread-follower-update-thread-settings", 1),
            Map.entry("thread-follower-edit-last-user-turn", 1),
            Map.entry("thread-follower-command-approval-decision", 1),
            Map.entry("thread-follower-file-approval-decision", 1),
            Map.entry("thread-follower-permissions-request-approval-response", 1),
            Map.entry("thread-follower-submit-user-input", 1),
            Map.entry("thread-follower-submit-mcp-server-elicitation-response", 1),
            Map.entry("thread-follower-set-queued-follow-ups-state", 1)
    );

    private final ObjectMapper objectMapper;
    private final Path socketPath;
    private final String clientType;
    private final Map<String, CompletableFuture<VscodeIpcResponse>> pendingResponses = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<VscodeIpcBroadcastListener> broadcastListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();
    private final Object writeLock = new Object();

    private SocketChannel channel;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread readerThread;
    private String clientId = INITIALIZING_CLIENT_ID;

    public VscodeIpcClient(ObjectMapper objectMapper) {
        this(objectMapper, resolveDefaultSocketPath(), "lqtigee");
    }

    VscodeIpcClient(ObjectMapper objectMapper, Path socketPath, String clientType) {
        this.objectMapper = objectMapper;
        this.socketPath = socketPath;
        this.clientType = clientType;
    }

    public VscodeIpcResponse request(String method, ObjectNode params) {
        return request(method, params, REQUEST_TIMEOUT);
    }

    public VscodeIpcResponse request(String method, ObjectNode params, Duration timeout) {
        return request(method, params, timeout, true);
    }

    private VscodeIpcResponse request(String method, ObjectNode params, Duration timeout, boolean ensureConnection) {
        if (ensureConnection) {
            ensureConnected();
        }
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<VscodeIpcResponse> responseFuture = new CompletableFuture<>();
        pendingResponses.put(requestId, responseFuture);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("type", "request");
        request.put("requestId", requestId);
        request.put("sourceClientId", clientId);
        request.put("version", versionOf(method));
        request.put("method", method);
        request.set("params", params == null ? objectMapper.createObjectNode() : params);
        request.put("timeoutMs", timeout.toMillis());
        writeFrame(request);

        try {
            return responseFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            pendingResponses.remove(requestId);
            throw ipcRequestFailed("VSCode IPC request timed out", method);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            pendingResponses.remove(requestId);
            throw ipcRequestFailed("VSCode IPC request interrupted", method);
        } catch (Exception exception) {
            pendingResponses.remove(requestId);
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof ApiException apiException) {
                throw apiException;
            }
            throw ipcRequestFailed("VSCode IPC request failed", cause.getMessage());
        }
    }

    public void addBroadcastListener(VscodeIpcBroadcastListener listener) {
        broadcastListeners.add(listener);
    }

    public void removeBroadcastListener(VscodeIpcBroadcastListener listener) {
        broadcastListeners.remove(listener);
    }

    public boolean isConnected() {
        return initialized.get();
    }

    public String clientId() {
        return clientId;
    }

    public Path socketPath() {
        return socketPath;
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            initialized.set(false);
            closeChannel();
            failPending(ipcRequestFailed("VSCode IPC closed", "socket closed"));
            clientId = INITIALIZING_CLIENT_ID;
        }
    }

    private void ensureConnected() {
        if (initialized.get()) {
            return;
        }
        synchronized (lifecycleLock) {
            if (initialized.get()) {
                return;
            }
            openSocket();
            startReader();
            initializeClient();
            initialized.set(true);
        }
    }

    private void openSocket() {
        if (!Files.exists(socketPath)) {
            throw new ApiException(
                    ErrorCode.VSCODE_IPC_SOCKET_NOT_FOUND,
                    HttpStatus.FAILED_DEPENDENCY,
                    "VSCode Codex IPC socket is not available",
                    socketPath.toString()
            );
        }
        try {
            channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            channel.connect(UnixDomainSocketAddress.of(socketPath));
            inputStream = Channels.newInputStream(channel);
            outputStream = Channels.newOutputStream(channel);
        } catch (IOException exception) {
            closeChannel();
            throw new ApiException(
                    ErrorCode.VSCODE_IPC_CONNECT_FAILED,
                    HttpStatus.FAILED_DEPENDENCY,
                    "VSCode Codex IPC connect failed",
                    exception.getMessage()
            );
        }
    }

    private void startReader() {
        readerThread = new Thread(this::readLoop, "lqtigee-vscode-ipc-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void initializeClient() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("clientType", clientType);
        VscodeIpcResponse response = request("initialize", params, Duration.ofSeconds(10), false);
        String nextClientId = textValue(response.result().path("clientId"));
        if (nextClientId == null) {
            throw ipcRequestFailed("VSCode IPC initialize did not return clientId", response.raw().toString());
        }
        clientId = nextClientId;
    }

    private void readLoop() {
        try {
            InputStream readerInputStream = inputStream;
            while (true) {
                JsonNode frame = readFrame(readerInputStream);
                if (frame == null) {
                    break;
                }
                handleFrame(frame);
            }
        } catch (IOException exception) {
            failPending(ipcRequestFailed("VSCode IPC read failed", exception.getMessage()));
        } catch (RuntimeException exception) {
            failPending(ipcRequestFailed("VSCode IPC reader failed", exception.getMessage()));
        } finally {
            synchronized (lifecycleLock) {
                initialized.set(false);
                closeChannel();
                clientId = INITIALIZING_CLIENT_ID;
            }
            failPending(ipcRequestFailed("VSCode IPC disconnected", socketPath.toString()));
        }
    }

    private JsonNode readFrame(InputStream readerInputStream) throws IOException {
        byte[] lengthBytes = readExactly(readerInputStream, 4);
        if (lengthBytes == null) {
            return null;
        }
        int length = littleEndianInt(lengthBytes);
        if (length < 0 || length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid VSCode IPC frame length: " + length);
        }
        byte[] payload = readExactly(readerInputStream, length);
        if (payload == null) {
            throw new EOFException("VSCode IPC frame ended early");
        }
        return objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
    }

    private byte[] readExactly(InputStream readerInputStream, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = readerInputStream.read(data, offset, length - offset);
            if (count < 0) {
                return offset == 0 ? null : throwEof();
            }
            offset += count;
        }
        return data;
    }

    private byte[] throwEof() throws EOFException {
        throw new EOFException("VSCode IPC socket closed while reading frame");
    }

    private void handleFrame(JsonNode frame) {
        String type = textValue(frame.path("type"));
        if ("response".equals(type)) {
            handleResponse(frame);
        } else if ("broadcast".equals(type)) {
            broadcastListeners.forEach(listener -> listener.onBroadcast(frame));
        } else if ("client-discovery-request".equals(type)) {
            respondCannotHandleDiscovery(frame);
        } else if ("request".equals(type)) {
            respondNoHandler(frame);
        }
    }

    private void handleResponse(JsonNode frame) {
        String requestId = textValue(frame.path("requestId"));
        if (requestId == null) {
            return;
        }
        CompletableFuture<VscodeIpcResponse> responseFuture = pendingResponses.remove(requestId);
        if (responseFuture == null) {
            return;
        }
        String resultType = textValue(frame.path("resultType"));
        if ("error".equals(resultType)) {
            responseFuture.completeExceptionally(ipcRequestFailed(
                    "VSCode IPC returned an error",
                    textValue(frame.path("error"), frame.toString())
            ));
            return;
        }
        responseFuture.complete(new VscodeIpcResponse(
                frame.path("method").asText(null),
                frame.path("handledByClientId").asText(null),
                frame.path("result"),
                frame
        ));
    }

    private void respondCannotHandleDiscovery(JsonNode frame) {
        String requestId = textValue(frame.path("requestId"));
        if (requestId == null) {
            return;
        }
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "client-discovery-response");
        response.put("requestId", requestId);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("canHandle", false);
        response.set("response", body);
        safeWriteFrame(response);
    }

    private void respondNoHandler(JsonNode frame) {
        String requestId = textValue(frame.path("requestId"));
        if (requestId == null) {
            return;
        }
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "response");
        response.put("requestId", requestId);
        response.put("resultType", "error");
        response.put("error", "no-handler-for-request");
        safeWriteFrame(response);
    }

    private void safeWriteFrame(JsonNode message) {
        try {
            writeFrame(message);
        } catch (ApiException ignored) {
        }
    }

    private void writeFrame(JsonNode message) {
        synchronized (writeLock) {
            if (outputStream == null) {
                throw new ApiException(
                        ErrorCode.VSCODE_IPC_CONNECT_FAILED,
                        HttpStatus.FAILED_DEPENDENCY,
                        "VSCode Codex IPC is not writable",
                        socketPath.toString()
                );
            }
            try {
                byte[] payload = objectMapper.writeValueAsBytes(message);
                if (payload.length > MAX_FRAME_BYTES) {
                    throw ipcRequestFailed("VSCode IPC frame is too large", String.valueOf(payload.length));
                }
                outputStream.write((byte) (payload.length & 0xff));
                outputStream.write((byte) ((payload.length >>> 8) & 0xff));
                outputStream.write((byte) ((payload.length >>> 16) & 0xff));
                outputStream.write((byte) ((payload.length >>> 24) & 0xff));
                outputStream.write(payload);
                outputStream.flush();
            } catch (IOException exception) {
                throw new ApiException(
                        ErrorCode.VSCODE_IPC_CONNECT_FAILED,
                        HttpStatus.FAILED_DEPENDENCY,
                        "VSCode Codex IPC write failed",
                        exception.getMessage()
                );
            }
        }
    }

    private void closeChannel() {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException ignored) {
        }
        channel = null;
        inputStream = null;
        outputStream = null;
    }

    private void failPending(ApiException exception) {
        pendingResponses.forEach((id, future) -> future.completeExceptionally(exception));
        pendingResponses.clear();
    }

    private int versionOf(String method) {
        return METHOD_VERSIONS.getOrDefault(method, 0);
    }

    private int littleEndianInt(byte[] bytes) {
        return (bytes[0] & 0xff)
                | ((bytes[1] & 0xff) << 8)
                | ((bytes[2] & 0xff) << 16)
                | ((bytes[3] & 0xff) << 24);
    }

    private ApiException ipcRequestFailed(String message, String detail) {
        return new ApiException(
                ErrorCode.VSCODE_IPC_REQUEST_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                message,
                detail
        );
    }

    private static Path resolveDefaultSocketPath() {
        String explicitSocket = System.getenv("LQTIGEE_VSCODE_IPC_SOCKET");
        if (explicitSocket != null && !explicitSocket.isBlank()) {
            return Path.of(explicitSocket);
        }
        Path ipcDirectory = Path.of(System.getProperty("java.io.tmpdir"), "codex-ipc");
        String uid = currentUid();
        if (uid != null && !uid.isBlank()) {
            return ipcDirectory.resolve("ipc-" + uid + ".sock");
        }
        Path onlySocket = onlySocketIn(ipcDirectory);
        return onlySocket == null ? ipcDirectory.resolve("ipc.sock") : onlySocket;
    }

    private static String currentUid() {
        String envUid = System.getenv("UID");
        if (envUid != null && !envUid.isBlank()) {
            return envUid;
        }
        try {
            Object uid = Files.getAttribute(Path.of(System.getProperty("user.home")), "unix:uid");
            return uid == null ? null : uid.toString();
        } catch (IOException | UnsupportedOperationException exception) {
            return null;
        }
    }

    private static Path onlySocketIn(Path directory) {
        if (!Files.isDirectory(directory)) {
            return null;
        }
        Path match = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "ipc-*.sock")) {
            for (Path candidate : stream) {
                if (match != null) {
                    return null;
                }
                match = candidate;
            }
        } catch (IOException exception) {
            return null;
        }
        return match;
    }

    private String textValue(JsonNode node) {
        return textValue(node, null);
    }

    private String textValue(JsonNode node, String defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
            return defaultValue;
        }
        String value = node.asText();
        return value.isBlank() ? defaultValue : value;
    }

    public interface VscodeIpcBroadcastListener {
        void onBroadcast(JsonNode frame);
    }

    public record VscodeIpcResponse(
            String method,
            String handledByClientId,
            JsonNode result,
            JsonNode raw
    ) {
    }
}
