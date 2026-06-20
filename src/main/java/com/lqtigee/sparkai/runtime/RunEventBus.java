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
        List<SseEmitter> emitters = subscribers.get(runId);
        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            if (send(emitter, event, emitters) && terminal) {
                complete(emitter, emitters);
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
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(exception -> emitters.remove(emitter));

        RunEventDto terminalEvent = terminalEvents.get(runId);
        if (terminalEvent != null && emitters.remove(emitter) && send(emitter, terminalEvent, emitters)) {
            complete(emitter, emitters);
        }
        return emitter;
    }

    private boolean send(SseEmitter emitter, RunEventDto event, List<SseEmitter> emitters) {
        try {
            emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException exception) {
            emitters.remove(emitter);
            return false;
        }
    }

    private void complete(SseEmitter emitter, List<SseEmitter> emitters) {
        try {
            emitter.complete();
        } finally {
            emitters.remove(emitter);
        }
    }

    private boolean isTerminal(RunEventDto event) {
        return event != null && TERMINAL_TYPES.contains(event.type());
    }
}
