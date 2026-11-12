package org.lambda.framework.gateway.forward;


import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.support.GatewayRouteContract;
import org.lambda.framework.enums.GatewayHttpExceptionEnum;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.lambda.framework.common.support.ServiceInstanceSource.Endpoint;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class RsocketStreamRoutes implements AutoCloseable {
    private static final Duration LOOKUP_TIMEOUT = Duration.ofMillis(500);
    private static final ParameterizedTypeReference<Map<String, GatewayInteraction>> INTERACTIONS =
            new ParameterizedTypeReference<>() {};
    private final ConcurrentHashMap<String, ServiceRoutes> byService = new ConcurrentHashMap<>();
    private final Disposable.Composite subscriptions = Disposables.composite();
    private final RSocketLoadbalance loadbalance;

    record Route(GatewayInteraction interaction, Set<Endpoint> endpoints) {}

    RsocketStreamRoutes(RSocketLoadbalance loadbalance) {
        this.loadbalance = loadbalance;
    }

    Mono<Route> resolve(String service, String path) {
        return Mono.defer(() -> byService.computeIfAbsent(service, this::watch).resolve(path));
    }

    Mono<Map<String, Route>> warm(String service) {
        return Mono.defer(() -> byService.computeIfAbsent(service, this::watch).updates
                .filter(snapshot -> snapshot.hasInstances() && !snapshot.pending())
                .next()
                .map(Snapshot::routes));
    }

    private ServiceRoutes watch(String service) {
        var contracts = new ConcurrentHashMap<ContractKey, Map<String, GatewayInteraction>>();
        return new ServiceRoutes(loadbalance.watchService(service)
                .switchMap(endpoints -> snapshots(endpoints, contracts)));
    }

    private final class ServiceRoutes {
        private final Flux<Snapshot> updates;
        private volatile Snapshot latest;

        private ServiceRoutes(Flux<Snapshot> snapshots) {
            updates = snapshots.doOnNext(snapshot -> latest = snapshot)
                    .replay(1).autoConnect(1, subscriptions::add);
        }

        private Mono<Route> resolve(String path) {
            var snapshot = latest;
            if (snapshot != null) {
                var route = snapshot.routes().get(path);
                if (route != null) {
                    return Mono.just(route);
                }
                if (!snapshot.pending() || snapshot.conflicts().contains(path)) {
                    return Mono.just(snapshot.resolve(path));
                }
            }
            return updates.map(ignored -> latest)
                    .filter(candidate -> candidate.knows(path) || !candidate.pending())
                    .next()
                    .timeout(LOOKUP_TIMEOUT, Mono.error(RsocketStreamRoutes::unavailable))
                    .switchIfEmpty(Mono.error(RsocketStreamRoutes::unavailable))
                    .map(candidate -> candidate.resolve(path));
        }
    }

    private Flux<Snapshot> snapshots(Set<Endpoint> endpoints,
                                     Map<ContractKey, Map<String, GatewayInteraction>> contracts) {
        var groups = new HashMap<ContractKey, List<Endpoint>>();
        for (Endpoint endpoint : endpoints) {
            groups.computeIfAbsent(ContractKey.from(endpoint), key -> new ArrayList<>()).add(endpoint);
        }
        contracts.keySet().retainAll(groups.keySet());
        var initial = Map.copyOf(contracts);
        var missing = groups.entrySet().stream()
                .filter(entry -> !initial.containsKey(entry.getKey()))
                .map(entry -> loadContract(entry.getKey(), entry.getValue())
                        .doOnNext(loaded -> contracts.put(loaded.key(), loaded.routes())))
                .toList();
        return Flux.merge(missing)
                .scan(initial, (loaded, update) -> {
                    var next = new HashMap<>(loaded);
                    next.put(update.key(), update.routes());
                    return next;
                })
                .map(loaded -> Snapshot.from(groups, loaded));
    }

    private Mono<LoadedContract> loadContract(ContractKey key, List<Endpoint> endpoints) {
        return Flux.fromIterable(endpoints)
                .concatMap(endpoint -> loadbalance.withEndpoint(MediaType.APPLICATION_JSON, endpoint,
                                requester -> requester.retrieveMono(Mono::empty, GatewayInteraction.DISCOVERY_ROUTE,
                                        new byte[0], INTERACTIONS))
                        .timeout(LOOKUP_TIMEOUT)
                        .map(Map::copyOf)
                        .filter(routes -> key.version().isEmpty()
                                || key.version().equals(GatewayRouteContract.versionOf(routes)))
                        .onErrorResume(error -> Mono.empty()))
                .next()
                .switchIfEmpty(Mono.error(RsocketStreamRoutes::unavailable))
                .map(routes -> new LoadedContract(key, routes))
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(250)).maxBackoff(Duration.ofSeconds(10)));
    }

    private static EventException unavailable() {
        return new EventException(GatewayHttpExceptionEnum.SERVICE_UNAVAILABLE, "RSocket route contract unavailable");
    }

    @Override
    public void close() {
        subscriptions.dispose();
        byService.clear();
    }

    // An older provider has no digest. Its snapshot is reusable only while this endpoint stays discovered.
    private record ContractKey(String version, String host, int port) {
        static ContractKey from(Endpoint endpoint) {
            return endpoint.routeVersion().isEmpty()
                    ? new ContractKey("", endpoint.host(), endpoint.port())
                    : new ContractKey(endpoint.routeVersion(), "", 0);
        }
    }

    private record LoadedContract(ContractKey key, Map<String, GatewayInteraction> routes) {}

    private record Snapshot(Map<String, Route> routes, Set<String> conflicts, boolean pending, boolean hasInstances) {
        static Snapshot from(Map<ContractKey, List<Endpoint>> groups,
                             Map<ContractKey, Map<String, GatewayInteraction>> contracts) {
            var candidates = new HashMap<String, Map<GatewayInteraction, Set<Endpoint>>>();
            groups.forEach((key, endpoints) -> contracts.getOrDefault(key, Map.of())
                    .forEach((path, interaction) -> candidates
                            .computeIfAbsent(path, ignored -> new EnumMap<>(GatewayInteraction.class))
                            .computeIfAbsent(interaction, ignored -> new HashSet<>()).addAll(endpoints)));
            var routes = new HashMap<String, Route>();
            var conflicts = new HashSet<String>();
            candidates.forEach((path, types) -> {
                if (types.size() == 1) {
                    var type = types.entrySet().iterator().next();
                    routes.put(path, new Route(type.getKey(), Set.copyOf(type.getValue())));
                } else {
                    conflicts.add(path);
                }
            });
            boolean pending = groups.keySet().stream().anyMatch(key -> !contracts.containsKey(key));
            return new Snapshot(Map.copyOf(routes), Set.copyOf(conflicts), pending, !groups.isEmpty());
        }

        boolean knows(String path) {
            return routes.containsKey(path) || conflicts.contains(path);
        }

        Route resolve(String path) {
            if (!hasInstances) {
                throw unavailable();
            }
            if (conflicts.contains(path)) {
                throw new EventException(GatewayHttpExceptionEnum.SERVICE_UNAVAILABLE, "Conflicting RSocket route contracts");
            }
            var route = routes.get(path);
            if (route == null) {
                throw new EventException(GatewayHttpExceptionEnum.NOT_FOUND, "No public MessageMapping for this path");
            }
            return route;
        }
    }
}
