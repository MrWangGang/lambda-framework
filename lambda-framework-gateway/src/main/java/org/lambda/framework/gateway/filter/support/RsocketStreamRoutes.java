package org.lambda.framework.gateway.filter.support;

import org.lambda.framework.common.annotation.rsocket.GatewayStream;
import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

final class RsocketStreamRoutes {
    private static final ParameterizedTypeReference<List<String>> PATHS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, GatewayInteraction>> INTERACTIONS =
            new ParameterizedTypeReference<>() {};
    private final ConcurrentHashMap<String, Mono<Map<String, GatewayInteraction>>> byService = new ConcurrentHashMap<>();
    private final RSocketLoadbalance loadbalance;

    RsocketStreamRoutes(RSocketLoadbalance loadbalance) {
        this.loadbalance = loadbalance;
    }

    Mono<Map<String, GatewayInteraction>> forService(String service) {
        return byService.computeIfAbsent(service, this::cachedLookup);
    }

    private Mono<Map<String, GatewayInteraction>> cachedLookup(String service) {
        var lastKnown = new AtomicReference<Map<String, GatewayInteraction>>(Map.of());
        return loadbalance.build(MediaType.APPLICATION_JSON, service)
                .flatMap(requester -> requester.retrieveMono(Mono::empty, GatewayInteraction.DISCOVERY_ROUTE,
                                new byte[0], INTERACTIONS)
                        .onErrorResume(error -> requester.retrieveMono(Mono::empty, GatewayStream.DISCOVERY_ROUTE,
                                        new byte[0], PATHS)
                                .map(paths -> paths.stream().distinct().collect(Collectors.toUnmodifiableMap(
                                        Function.identity(), ignored -> GatewayInteraction.REQUEST_STREAM)))))
                .map(Map::copyOf)
                .timeout(Duration.ofMillis(500))
                .doOnNext(lastKnown::set)
                .onErrorResume(error -> Mono.fromSupplier(lastKnown::get))
                .switchIfEmpty(Mono.fromSupplier(lastKnown::get))
                .cache(Duration.ofSeconds(30));
    }
}
