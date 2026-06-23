package com.lqtigee.sparkai.runtime;

import com.lqtigee.sparkai.dto.RunEventDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class RunEventBus {

    private static final Set<String> TERMINAL_TYPES = Set.of("done", "error", "stopped");
    private static final int MAX_HISTORY_EVENTS = 500;

    private final Map<String, CopyOnWriteArrayList<RunEventSubscriber>> subscribers = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<RunEventDto>> eventHistory = new ConcurrentHashMap<>();
    private final Map<String, RunEventDto> terminalEvents = new ConcurrentHashMap<>();
    private final Supplier<SseEmitter> emitterFactory;

    public RunEventBus() {
        this(() -> new SseEmitter(0L));
    }

    RunEventBus(Supplier<SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    public void publish(String runId, RunEventDto event) {
        List<RunEventSubscriber> subscriberSnapshot;
        boolean terminal = isTerminal(event);
        synchronized (this) {
            appendHistory(runId, event);
            if (terminal) {
                terminalEvents.put(runId, event);
            }
            CopyOnWriteArrayList<RunEventSubscriber> runSubscribers = subscribers.get(runId);
            if (runSubscribers == null) {
                return;
            }
            subscriberSnapshot = List.copyOf(runSubscribers);
        }

        for (RunEventSubscriber subscriber : subscriberSnapshot) {
            if (send(runId, subscriber, event) && terminal) {
                complete(runId, subscriber);
            }
        }
    }

    public SseEmitter subscribe(String runId) {
        SseEmitter emitter = emitterFactory.get();
        RunEventSubscriber subscriber = new SseRunEventSubscriber(emitter);
        synchronized (this) {
            addSubscriber(runId, subscriber);
        }
        emitter.onCompletion(() -> removeSubscriber(runId, subscriber));
        emitter.onTimeout(() -> removeSubscriber(runId, subscriber));
        emitter.onError(exception -> removeSubscriber(runId, subscriber));

        RunEventDto terminalEvent = terminalEvents.get(runId);
        if (terminalEvent != null && send(runId, subscriber, terminalEvent)) {
            complete(runId, subscriber);
        }
        return emitter;
    }

    public RunEventSubscription subscribeReplay(String runId, Consumer<RunEventDto> consumer) {
        RunEventSubscriber subscriber = new ConsumerRunEventSubscriber(consumer);
        List<RunEventDto> replayEvents;
        synchronized (this) {
            addSubscriber(runId, subscriber);
            replayEvents = new ArrayList<>(eventHistory.getOrDefault(runId, new CopyOnWriteArrayList<>()));
        }

        for (RunEventDto event : replayEvents) {
            if (!send(runId, subscriber, event)) {
                return () -> {
                };
            }
            if (isTerminal(event)) {
                complete(runId, subscriber);
                return () -> {
                };
            }
        }

        return () -> removeSubscriber(runId, subscriber);
    }

    private boolean send(String runId, RunEventSubscriber subscriber, RunEventDto event) {
        try {
            subscriber.send(event);
            return true;
        } catch (RuntimeException exception) {
            removeSubscriber(runId, subscriber);
            return false;
        }
    }

    private void complete(String runId, RunEventSubscriber subscriber) {
        try {
            subscriber.complete();
        } finally {
            removeSubscriber(runId, subscriber);
        }
    }

    private synchronized void addSubscriber(String runId, RunEventSubscriber subscriber) {
        subscribers.computeIfAbsent(runId, id -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    private synchronized void removeSubscriber(String runId, RunEventSubscriber subscriber) {
        CopyOnWriteArrayList<RunEventSubscriber> runSubscribers = subscribers.get(runId);
        if (runSubscribers == null) {
            return;
        }
        runSubscribers.remove(subscriber);
        if (runSubscribers.isEmpty()) {
            subscribers.remove(runId, runSubscribers);
        }
    }

    private void appendHistory(String runId, RunEventDto event) {
        CopyOnWriteArrayList<RunEventDto> events = eventHistory.computeIfAbsent(runId, id -> new CopyOnWriteArrayList<>());
        events.add(event);
        while (events.size() > MAX_HISTORY_EVENTS) {
            events.remove(0);
        }
    }

    private boolean isTerminal(RunEventDto event) {
        return event != null && TERMINAL_TYPES.contains(event.type());
    }

    int subscriberCount(String runId) {
        List<RunEventSubscriber> runSubscribers = subscribers.get(runId);
        return runSubscribers == null ? 0 : runSubscribers.size();
    }

    public interface RunEventSubscription {
        void close();
    }

    private interface RunEventSubscriber {
        void send(RunEventDto event);

        void complete();
    }

    private static class SseRunEventSubscriber implements RunEventSubscriber {

        private final SseEmitter emitter;

        private SseRunEventSubscriber(SseEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void send(RunEventDto event) {
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException exception) {
                throw new IllegalStateException("SSE event send failed", exception);
            }
        }

        @Override
        public void complete() {
            emitter.complete();
        }
    }

    private static class ConsumerRunEventSubscriber implements RunEventSubscriber {

        private final Consumer<RunEventDto> consumer;

        private ConsumerRunEventSubscriber(Consumer<RunEventDto> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void send(RunEventDto event) {
            consumer.accept(event);
        }

        @Override
        public void complete() {
        }
    }
}
