package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.RunEventDto;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class RunEventBus {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public void publish(String runId, RunEventDto event) {
        List<SseEmitter> emitters = subscribers.get(runId);
        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException exception) {
                emitters.remove(emitter);
            }
        }
    }

    public SseEmitter subscribe(String runId) {
        SseEmitter emitter = new SseEmitter(0L);
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.computeIfAbsent(
                runId,
                id -> new CopyOnWriteArrayList<>()
        );
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(exception -> emitters.remove(emitter));
        return emitter;
    }
}
