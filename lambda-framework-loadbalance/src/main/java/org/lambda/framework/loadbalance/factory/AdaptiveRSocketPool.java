package org.lambda.framework.loadbalance.factory;

import io.rsocket.RSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.ServiceInstanceSource.Endpoint;
import org.lambda.framework.enums.LoadBalanceExceptionEnum;
import org.springframework.messaging.rsocket.RSocketRequester;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

final class AdaptiveRSocketPool implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AdaptiveRSocketPool.class);
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration RECONNECT_DELAY = Duration.ofMillis(250);
    private static final Retry RECONNECT_RETRY = Retry.backoff(Long.MAX_VALUE, RECONNECT_DELAY)
            .maxBackoff(Duration.ofSeconds(10)).transientErrors(true);
    private static final long INITIAL_LATENCY_NANOS = Duration.ofMillis(5).toNanos();
    private static final long ERROR_PENALTY_NANOS = Duration.ofMillis(200).toNanos();

    private final String service;
    private final int connectionsPerInstance;
    private final Function<Endpoint, RSocketRequester> requesterFactory;
    private final boolean adaptiveSelection;
    private final Sinks.Many<Boolean> readyChanges = Sinks.many().replay().latest();
    private final Disposable.Swap discovery = Disposables.swap();
    private final Sinks.One<Void> firstConnection = Sinks.one();
    private final AtomicInteger nextInstance = new AtomicInteger();
    private Map<Endpoint, Instance> instances = Map.of();
    private volatile List<Instance> snapshot = List.of();

    AdaptiveRSocketPool(String service, int connectionsPerInstance, boolean adaptiveSelection,
                        Flux<Set<Endpoint>> targets, Function<Endpoint, RSocketRequester> requesterFactory) {
        this.service = service;
        this.connectionsPerInstance = connectionsPerInstance;
        this.requesterFactory = requesterFactory;
        this.adaptiveSelection = adaptiveSelection;
        discovery.update(targets.subscribe(this::refresh,
                error -> log.warn("RSocket target updates stopped for {}", service, error)));
    }

    private synchronized void refresh(Set<Endpoint> targets) {
        if (discovery.isDisposed()) {
            return;
        }
        var next = new LinkedHashMap<Endpoint, Instance>();
        var added = new ArrayList<Instance>();
        for (Endpoint target : targets) {
            next.computeIfAbsent(target, key -> {
                Instance current = instances.get(key);
                if (current != null) {
                    return current;
                }
                var instance = new Instance(key, connectionsPerInstance, () -> requesterFactory.apply(key), this::signalReady);
                added.add(instance);
                return instance;
            });
        }
        var previous = instances;
        instances = next;
        snapshot = List.copyOf(next.values());
        previous.forEach((address, instance) -> {
            if (!next.containsKey(address)) {
                instance.close();
            }
        });
        added.forEach(Instance::start);
    }

    private void signalReady() {
        firstConnection.tryEmitEmpty();
        synchronized (readyChanges) {
            readyChanges.tryEmitNext(true);
        }
    }

    <T> Mono<T> request(Function<RSocketRequester, Mono<T>> action, Set<Endpoint> eligible) {
        if (!adaptiveSelection) {
            return destination(eligible).flatMap(destination -> action.apply(destination.connection().requester()));
        }
        return destination(eligible).flatMap(destination -> {
            if (!destination.adaptive()) {
                return action.apply(destination.connection().requester());
            }
            Instance instance = destination.instance();
            return Mono.using(instance::beginRequest,
                    attempt -> action.apply(destination.connection().requester())
                            .doOnSuccess(ignored -> attempt.finish(instance, false))
                            .doOnError(ignored -> attempt.finish(instance, true)),
                    attempt -> {
                        instance.pending.decrementAndGet();
                        attempt.cancel();
                    });
        });
    }

    <T> Flux<T> stream(Function<RSocketRequester, Flux<T>> action, Set<Endpoint> eligible) {
        return destination(eligible).flatMapMany(destination -> action.apply(destination.connection().requester()));
    }

    Mono<Void> awaitReady(Set<Endpoint> eligible) {
        return destination(eligible).then();
    }

    private Mono<Destination> destination(Set<Endpoint> eligible) {
        return Mono.defer(() -> {
            var selected = select(eligible);
            if (selected.isPresent()) {
                return Mono.just(selected.orElseThrow(this::unavailable));
            }
            if (eligible != null) {
                return readyChanges.asFlux().startWith(true)
                        .concatMap(ignored -> Mono.justOrEmpty(select(eligible)))
                        .next().timeout(Duration.ofMillis(500), Mono.error(this::unavailable))
                        .switchIfEmpty(Mono.error(this::unavailable));
            }
            return firstConnection.asMono().timeout(CONNECTION_TIMEOUT, Mono.error(this::unavailable))
                    .then(Mono.defer(() -> Mono.justOrEmpty(select(eligible))))
                    .switchIfEmpty(Mono.error(this::unavailable));
        });
    }

    private EventException unavailable() {
        return new EventException(LoadBalanceExceptionEnum.CONNECTION_UNAVAILABLE, "No ready RSocket connection for " + service);
    }

    private Optional<Destination> select(Set<Endpoint> eligible) {
        var candidates = snapshot;
        int size = candidates.size();
        if (size == 0) {
            return Optional.empty();
        }
        if (size == 1) {
            var single = candidates.getFirst();
            if (!single.eligible(eligible)) {
                return Optional.empty();
            }
            return single.nextConnection().map(value -> new Destination(single, value, false));
        }
        if (!adaptiveSelection) {
            int offset = Math.floorMod(nextInstance.getAndIncrement(), size);
            return roundRobin(candidates, offset, eligible, false);
        }
        var random = ThreadLocalRandom.current();
        int idx1 = random.nextInt(size);
        // Keep a small sample from slower instances so that recovery is visible to the selector.
        if (random.nextInt(256) == 0) {
            var probe = candidates.get(idx1);
            if (probe.eligible(eligible)) {
                var connection = probe.nextConnection();
                if (connection.isPresent()) {
                    return Optional.of(new Destination(probe, connection.orElseThrow(this::unavailable), true));
                }
            }
        }
        int idx2 = (idx1 + 1 + random.nextInt(size - 1)) % size;
        var first = candidates.get(idx1);
        var second = candidates.get(idx2);
        boolean firstOk = first.eligible(eligible) && first.hasConnection();
        boolean secondOk = second.eligible(eligible) && second.hasConnection();
        Instance best = null;
        if (firstOk && secondOk) {
            best = first.score() <= second.score() ? first : second;
        } else if (firstOk) {
            best = first;
        } else if (secondOk) {
            best = second;
        }
        if (best != null) {
            var connection = best.nextConnection();
            if (connection.isPresent()) {
                return Optional.of(new Destination(best, connection.orElseThrow(this::unavailable), true));
            }
        }
        return roundRobin(candidates, idx1, eligible, true);
    }

    private static Optional<Destination> roundRobin(List<Instance> candidates, int offset, Set<Endpoint> eligible,
            boolean adaptive) {
        int size = candidates.size();
        for (int i = 0; i < size; i++) {
            var candidate = candidates.get((offset + i) % size);
            if (candidate.eligible(eligible)) {
                var connection = candidate.nextConnection();
                if (connection.isPresent()) {
                    return connection.map(value -> new Destination(candidate, value, adaptive));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized void close() {
        discovery.dispose();
        snapshot = List.of();
        firstConnection.tryEmitError(unavailable());
        synchronized (readyChanges) {
            readyChanges.tryEmitComplete();
        }
        instances.values().forEach(Instance::close);
        instances = Map.of();
    }

    private record Destination(Instance instance, Connected connection, boolean adaptive) {}

    private record Connected(RSocketRequester requester, RSocket socket) {
        boolean available() {
            return !socket.isDisposed() && socket.availability() > 0.0;
        }
    }

    private static final class Instance {
        private final Endpoint endpoint;
        private final List<Slot> connections;
        private final AtomicInteger cursor = new AtomicInteger();
        private final AtomicInteger pending = new AtomicInteger();
        private final AtomicLong latency = new AtomicLong(INITIAL_LATENCY_NANOS);

        Instance(Endpoint endpoint, int count, Supplier<RSocketRequester> factory, Runnable onReady) {
            this.endpoint = endpoint;
            connections = IntStream.range(0, count).mapToObj(i -> new Slot(factory, onReady)).toList();
        }

        boolean eligible(Set<Endpoint> eligible) {
            return eligible == null || eligible.contains(endpoint);
        }

        void start() {
            connections.forEach(Slot::start);
        }

        void close() {
            connections.forEach(Slot::close);
        }

        boolean hasConnection() {
            for (Slot slot : connections) {
                if (slot.available()) {
                    return true;
                }
            }
            return false;
        }

        Optional<Connected> nextConnection() {
            int size = connections.size();
            if (size == 1) {
                Connected connection = connections.getFirst().connected;
                return connection != null && connection.available() ? Optional.of(connection) : Optional.empty();
            }
            int offset = ThreadLocalRandom.current().nextInt(size);
            for (int i = 0; i < size; i++) {
                Connected connection = connections.get((offset + i) % size).connected;
                if (connection != null && connection.available()) {
                    return Optional.of(connection);
                }
            }
            return Optional.empty();
        }

        double score() {
            return (pending.get() + 1.0) * latency.get();
        }

        Attempt beginRequest() {
            pending.incrementAndGet();
            return new Attempt();
        }

        void recordLatency(long elapsedNanos) {
            latency.updateAndGet(previous -> previous + (elapsedNanos - previous) / 8);
        }
    }

    private static final class Attempt extends AtomicBoolean {
        private final long started = System.nanoTime();

        private void finish(Instance instance, boolean failed) {
            if (compareAndSet(false, true)) {
                long elapsed = Math.max(1L, System.nanoTime() - started);
                long sample = failed
                        ? Math.max(elapsed, Math.max(instance.latency.get() * 4L, ERROR_PENALTY_NANOS))
                        : elapsed;
                instance.recordLatency(sample);
            }
        }

        private void cancel() {
            // Cancelled callers drop in-flight count without penalizing backend latency.
            compareAndSet(false, true);
        }
    }

    private static final class Slot {
        private final Supplier<RSocketRequester> factory;
        private final Runnable onReady;
        private final Disposable.Swap lifecycle = Disposables.swap();
        private volatile Connected connected;

        Slot(Supplier<RSocketRequester> factory, Runnable onReady) {
            this.factory = factory;
            this.onReady = onReady;
        }

        void start() {
            lifecycle.update(Flux.using(factory::get,
                            requester -> requester.rsocketClient().source().timeout(CONNECTION_TIMEOUT)
                                    .flatMapMany(socket -> {
                                        // Bind requests to this socket. Only the slot lifecycle dials a replacement.
                                        var connectedRequester = RSocketRequester.wrap(socket, requester.dataMimeType(),
                                                requester.metadataMimeType(), requester.strategies());
                                        return Flux.just(new Connected(connectedRequester, socket))
                                                .concatWith(socket.onClose().then(Mono.empty()));
                                    }),
                            requester -> {
                                connected = null;
                                requester.rsocketClient().dispose();
                            })
                    .retryWhen(RECONNECT_RETRY)
                    .repeatWhen(completion -> completion.delayElements(RECONNECT_DELAY))
                    .subscribe(connection -> {
                        connected = connection;
                        onReady.run();
                    }, error -> log.warn("RSocket connection maintenance stopped", error)));
        }

        boolean available() {
            var connection = connected;
            return connection != null && connection.available();
        }

        void close() {
            lifecycle.dispose();
            connected = null;
        }
    }
}
