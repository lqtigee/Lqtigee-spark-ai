package com.lqtigee.sparkai.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lqtigee.sparkai.error.ApiException;
import com.lqtigee.sparkai.error.ErrorCode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;

public class CodexAppServerClient implements AutoCloseable {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final ProcessBuilder processBuilder;
    private final AtomicLong requestIds = new AtomicLong(1L);
    private final Map<Long, CompletableFuture<JsonNode>> pendingResponses = new ConcurrentHashMap<>();
    private final List<CodexAppServerNotificationListener> notificationListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();
    private final Object writeLock = new Object();

    private Process process;
    private BufferedWriter writer;
    private Thread stdoutReader;
    private Thread stderrReader;

    public CodexAppServerClient(ObjectMapper objectMapper) {
        this(
                objectMapper,
                new ProcessBuilder("codex", "app-server", "--stdio")
        );
    }

    CodexAppServerClient(ObjectMapper objectMapper, ProcessBuilder processBuilder) {
        this.objectMapper = objectMapper;
        this.processBuilder = processBuilder;
    }

    public JsonNode request(String method, ObjectNode params) {
        return request(method, params, true);
    }

    private JsonNode request(String method, ObjectNode params, boolean ensureStarted) {
        if (ensureStarted) {
            ensureStarted();
        }
        long id = requestIds.getAndIncrement();
        CompletableFuture<JsonNode> responseFuture = new CompletableFuture<>();
        pendingResponses.put(id, responseFuture);

        ObjectNode request = objectMapper.createObjectNode();
        request.put("id", id);
        request.put("method", method);
        request.set("params", params == null ? objectMapper.createObjectNode() : params);
        writeJson(request);

        try {
            return responseFuture.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            pendingResponses.remove(id);
            throw appServerFailed("Codex app-server request timed out", method);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            pendingResponses.remove(id);
            throw appServerFailed("Codex app-server request interrupted", method);
        } catch (Exception exception) {
            pendingResponses.remove(id);
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof ApiException apiException) {
                throw apiException;
            }
            throw appServerFailed("Codex app-server request failed", cause.getMessage());
        }
    }

    public void addNotificationListener(CodexAppServerNotificationListener listener) {
        notificationListeners.add(listener);
    }

    public void removeNotificationListener(CodexAppServerNotificationListener listener) {
        notificationListeners.remove(listener);
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
            process = null;
            writer = null;
            initialized.set(false);
            pendingResponses.forEach((id, future) -> future.completeExceptionally(
                    appServerFailed("Codex app-server closed", "requestId=" + id)
            ));
            pendingResponses.clear();
        }
    }

    private void ensureStarted() {
        if (initialized.get()) {
            return;
        }
        synchronized (lifecycleLock) {
            if (initialized.get()) {
                return;
            }
            startProcess();
            initializeProtocol();
            initialized.set(true);
        }
    }

    private void startProcess() {
        try {
            initialized.set(false);
            process = processBuilder.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            stdoutReader = startReader("stdout", process.getInputStream());
            stderrReader = startStderrReader();
        } catch (IOException exception) {
            throw appServerFailed("Codex app-server start failed", exception.getMessage());
        }
    }

    private Thread startReader(String name, java.io.InputStream inputStream) {
        Thread thread = new Thread(() -> readStdout(inputStream), "lqtigee-codex-app-server-" + name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private Thread startStderrReader() {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // Stderr is noisy in normal Codex startup; protocol errors arrive on stdout JSON-RPC.
                }
            } catch (IOException ignored) {
            }
        }, "lqtigee-codex-app-server-stderr");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void initializeProtocol() {
        ObjectNode clientInfo = objectMapper.createObjectNode();
        clientInfo.put("name", "lqtigee");
        clientInfo.put("title", "Lqtigee");
        clientInfo.put("version", "0.1.0");
        ObjectNode capabilities = objectMapper.createObjectNode();
        capabilities.put("experimentalApi", true);
        ObjectNode params = objectMapper.createObjectNode();
        params.set("clientInfo", clientInfo);
        params.set("capabilities", capabilities);
        request("initialize", params, false);

        ObjectNode initializedNotification = objectMapper.createObjectNode();
        initializedNotification.put("method", "initialized");
        initializedNotification.set("params", objectMapper.createObjectNode());
        writeJson(initializedNotification);
    }

    private void readStdout(java.io.InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException exception) {
            failPending(appServerFailed("Codex app-server stdout failed", exception.getMessage()));
        } finally {
            failPending(appServerFailed("Codex app-server exited", "stdout closed"));
            synchronized (lifecycleLock) {
                initialized.set(false);
                process = null;
                writer = null;
            }
        }
    }

    private void handleLine(String line) {
        JsonNode message;
        try {
            message = objectMapper.readTree(line);
        } catch (IOException exception) {
            return;
        }
        JsonNode idNode = message.path("id");
        if (idNode.isIntegralNumber()) {
            handleResponse(idNode.asLong(), message);
            return;
        }
        String method = textValue(message.path("method"));
        if (method == null) {
            return;
        }
        JsonNode params = message.path("params");
        notificationListeners.forEach(listener -> listener.onNotification(method, params));
    }

    private void handleResponse(long id, JsonNode message) {
        CompletableFuture<JsonNode> responseFuture = pendingResponses.remove(id);
        if (responseFuture == null) {
            return;
        }
        JsonNode error = message.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            responseFuture.completeExceptionally(appServerFailed(
                    textValue(error.path("message"), "Codex app-server returned an error"),
                    error.toString()
            ));
            return;
        }
        responseFuture.complete(message.path("result"));
    }

    private void writeJson(JsonNode message) {
        synchronized (writeLock) {
            if (writer == null) {
                throw appServerFailed("Codex app-server is not writable", "writer=null");
            }
            try {
                writer.write(objectMapper.writeValueAsString(message));
                writer.newLine();
                writer.flush();
            } catch (IOException exception) {
                throw appServerFailed("Codex app-server write failed", exception.getMessage());
            }
        }
    }

    private void failPending(ApiException exception) {
        pendingResponses.forEach((id, future) -> future.completeExceptionally(exception));
        pendingResponses.clear();
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

    private ApiException appServerFailed(String message, String detail) {
        return new ApiException(
                ErrorCode.PROCESS_START_FAILED,
                HttpStatus.FAILED_DEPENDENCY,
                message,
                detail
        );
    }

    public interface CodexAppServerNotificationListener {
        void onNotification(String method, JsonNode params);
    }
}
