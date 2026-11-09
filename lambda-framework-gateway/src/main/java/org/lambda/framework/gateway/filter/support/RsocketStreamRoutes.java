package org.lambda.framework.gateway.filter.support;

import org.lambda.framework.common.annotation.rsocket.GatewayStream;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

final class RsocketStreamRoutes {
    private static final ParameterizedTypeReference<List<String>> PATHS = new ParameterizedTypeReference<>() {};
    private final ConcurrentHashMap<String, Mono<Set<String>>> byService = new ConcurrentHashMap<>();
    private final RSocketLoadbalance loadbalance;

    RsocketStreamRoutes(RSocketLoadbalance loadbalance) {
        this.loadbalance = loadbalance;
    }

    Mono<Set<String>> forService(String service) {
        return byService.computeIfAbsent(service, this::cachedLookup);
    }

    private Mono<Set<String>> cachedLookup(String service) {
        var lastKnown = new AtomicReference<Set<String>>(Set.of());
        return loadbalance.build(MediaType.APPLICATION_JSON, service)
                .flatMap(requester -> requester.retrieveMono(Mono::empty, GatewayStream.DISCOVERY_ROUTE,
                        new byte[0], PATHS))
                .map(Set::copyOf)
                .timeout(Duration.ofMillis(500))
                .doOnNext(lastKnown::set)
                .onErrorResume(error -> Mono.fromSupplier(lastKnown::get))
                .switchIfEmpty(Mono.fromSupplier(lastKnown::get))
                .cache(Duration.ofSeconds(30));
    }
}
