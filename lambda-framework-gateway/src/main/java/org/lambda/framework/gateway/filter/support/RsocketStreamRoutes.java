package org.lambda.framework.gateway.filter.support;

import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class RsocketStreamRoutes {
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
        return loadbalance.build(MediaType.APPLICATION_JSON, service)
                .flatMap(requester -> requester.retrieveMono(Mono::empty, GatewayInteraction.DISCOVERY_ROUTE,
                        new byte[0], INTERACTIONS))
                .map(Map::copyOf)
                .timeout(Duration.ofMillis(500))
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("Empty route contract response")))
                .onErrorMap(error -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "RSocket route contract unavailable", error))
                .cache(ignored -> Duration.ofSeconds(30), error -> Duration.ofSeconds(1), () -> Duration.ZERO);
    }
}
