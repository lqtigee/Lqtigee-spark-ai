package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.RunEventDto;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class RunEventBus {

    private static final Set<String> TERMINAL_TYPES = Set.of("done", "error", "stopped");

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, RunEventDto> terminalEvents = new ConcurrentHashMap<>();
    private final Supplier<SseEmitter> emitterFactory;

    public RunEventBus() {
        this(() -> new SseEmitter(0L));
    }

    RunEventBus(Supplier<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public void publish(String runId, RunEventDto event) {
        boolean terminal = isTerminal(event);
        if (terminal) {
            terminalEvents.put(runId, event);
        }
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(runId);
        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            if (send(runId, emitter, event, emitters) && terminal) {
                complete(runId, emitter, emitters);
            }
        }
    }

    public SseEmitter subscribe(String runId) {
        SseEmitter emitter = emitterFactory.get();
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.computeIfAbsent(
                runId,
                id -> new CopyOnWriteArrayList<>()
        );
        emitters.add(emitter);
        emitter.onCompletion(() -> removeSubscriber(runId, emitters, emitter));
        emitter.onTimeout(() -> removeSubscriber(runId, emitters, emitter));
        emitter.onError(exception -> removeSubscriber(runId, emitters, emitter));

        RunEventDto terminalEvent = terminalEvents.get(runId);
        if (terminalEvent != null && send(runId, emitter, terminalEvent, emitters)) {
            complete(runId, emitter, emitters);
        }
        return emitter;
    }

    private boolean send(String runId, SseEmitter emitter, RunEventDto event, CopyOnWriteArrayList<SseEmitter> emitters) {
        try {
            emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException exception) {
            removeSubscriber(runId, emitters, emitter);
            return false;
        }
    }

    private void complete(String runId, SseEmitter emitter, CopyOnWriteArrayList<SseEmitter> emitters) {
        try {
            emitter.complete();
        } finally {
            removeSubscriber(runId, emitters, emitter);
        }
    }

    private void removeSubscriber(String runId, CopyOnWriteArrayList<SseEmitter> emitters, SseEmitter emitter) {
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            subscribers.remove(runId, emitters);
        }
    }

    private boolean isTerminal(RunEventDto event) {
        return event != null && TERMINAL_TYPES.contains(event.type());
    }

    int subscriberCount(String runId) {
        List<SseEmitter> emitters = subscribers.get(runId);
        return emitters == null ? 0 : emitters.size();
    }
}
