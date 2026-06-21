package com.lqtigee.sparkai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.lqtigee.sparkai.dto.RunEventDto;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class RunEventBusTest {

    @Test
    void publishCompletesCurrentSubscriberAfterTerminalEvent() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        RunEventBus eventBus = new RunEventBus(() -> emitter);

        eventBus.subscribe("run-current");
        eventBus.publish("run-current", event("run-current", "done"));

        assertThat(emitter.events())
                .extracting(RunEventDto::type)
                .containsExactly("done");
        assertThat(emitter.completed()).isTrue();
        assertThat(eventBus.subscriberCount("run-current")).isZero();
    }

    @Test
    void subscribeAfterTerminalEventReplaysAndCompletes() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        RunEventBus eventBus = new RunEventBus(() -> emitter);

        eventBus.publish("run-late", event("run-late", "error"));
        eventBus.subscribe("run-late");

        assertThat(emitter.events())
                .extracting(RunEventDto::type)
                .containsExactly("error");
        assertThat(emitter.completed()).isTrue();
        assertThat(eventBus.subscriberCount("run-late")).isZero();
    }

    @Test
    void subscribeAfterNonTerminalEventDoesNotReceiveFakeEvent() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        RunEventBus eventBus = new RunEventBus(() -> emitter);

        eventBus.publish("run-open", event("run-open", "output"));
        eventBus.subscribe("run-open");

        assertThat(emitter.events()).isEmpty();
        assertThat(emitter.completed()).isFalse();
        assertThat(eventBus.subscriberCount("run-open")).isOne();
    }

    @Test
    void completionCallbackRemovesEmptySubscriberList() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        RunEventBus eventBus = new RunEventBus(() -> emitter);

        eventBus.subscribe("run-client-closed");

        assertThat(eventBus.subscriberCount("run-client-closed")).isOne();

        emitter.simulateClientCompletion();

        assertThat(eventBus.subscriberCount("run-client-closed")).isZero();
    }

    private RunEventDto event(String runId, String type) {
        return new RunEventDto(runId, type, "message", Instant.now(), Map.of());
    }

    private static class CapturingSseEmitter extends SseEmitter {

        private final List<RunEventDto> events = new CopyOnWriteArrayList<>();
        private Runnable completionCallback;
        private boolean completed;

        CapturingSseEmitter() {
            super(0L);
        }

        @Override
        public void send(Object object) throws IOException {
            events.add((RunEventDto) object);
        }

        @Override
        public synchronized void complete() {
            completed = true;
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
            completionCallback = callback;
        }

        void simulateClientCompletion() {
            if (completionCallback != null) {
                completionCallback.run();
            }
        }

        List<RunEventDto> events() {
            return events;
        }

        boolean completed() {
            return completed;
        }
    }
}
