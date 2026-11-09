package org.lambda.framework.loadbalance.factory;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.SecurityStash;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.lambda.framework.loadbalance.enums.LoadBalanceExceptionEnum.*;

@Component
public class RSocketLoadbalance {
    private static final Logger log = LoggerFactory.getLogger(RSocketLoadbalance.class);
    private final DiscoveryClient discoveryClient;
    private final ObjectProvider<RSocketRequester.Builder> builders;
    private final RSocketStrategies strategies;
    private final int maxInboundPayloadBytes;
    private final ConcurrentHashMap<RequesterKey, ProcessResponse> requesters = new ConcurrentHashMap<>();

    private record RequesterKey(String target, Integer port, MimeType dataMimeType) {}

    public RSocketLoadbalance(DiscoveryClient discoveryClient, ObjectProvider<RSocketRequester.Builder> builders,
                              RSocketStrategies strategies,
                              @Value("${lambda.rsocket.client.max-inbound-payload-bytes:16777216}") int maxInboundPayloadBytes) {
        this.discoveryClient = discoveryClient;
        this.builders = builders;
        this.strategies = strategies;
        this.maxInboundPayloadBytes = maxInboundPayloadBytes;
    }

    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessResponse {
        private RSocketRequester requester;

        public <T> Mono<T> retrieveMono(PrincipalStash principalStash, MimeType mimeType, String route,
                                         Object data, Class<T> dataType) {
            return request(principalStash, route).flatMap(spec -> spec.data(data).retrieveMono(dataType));
        }

        public <T> Flux<T> retrieveFlux(PrincipalStash principalStash, MimeType mimeType, String route,
                                         Object data, Class<T> dataType) {
            return request(principalStash, route).flatMapMany(spec -> spec.data(data).retrieveFlux(dataType));
        }

        public <T> Mono<T> retrieveMono(PrincipalStash principalStash, String route,
                                        Object data, ParameterizedTypeReference<T> dataType) {
            return request(principalStash, route).flatMap(spec -> spec.data(data).retrieveMono(dataType));
        }

        public <T> Flux<T> retrieveFlux(PrincipalStash principalStash, String route,
                                        Object data, ParameterizedTypeReference<T> dataType) {
            return request(principalStash, route).flatMapMany(spec -> spec.data(data).retrieveFlux(dataType));
        }

        public <T> Flux<T> retrieveChannel(PrincipalStash principalStash, String route,
                                            Publisher<byte[]> data, Class<T> dataType) {
            return request(principalStash, route)
                    .flatMapMany(spec -> spec.data(data, byte[].class).retrieveFlux(dataType));
        }

        private Mono<RSocketRequester.RequestSpec> request(PrincipalStash principalStash, String route) {
            return Mono.defer(principalStash::setSecurityStash)
                    .switchIfEmpty(Mono.fromSupplier(SecurityStash::new))
                    .map(stash -> requester.route(route).metadata(stash, MimeTypeUtils.APPLICATION_JSON));
        }
    }

    public Mono<ProcessResponse> build(MimeType mimeType, String ip, Integer port) {
        Assert.verify(ip, EB_LOADBALANCE_005);
        Assert.verify(port, EB_LOADBALANCE_006);
        RequesterKey key = new RequesterKey(ip, port, mimeType);
        return Mono.fromSupplier(() -> requesters.computeIfAbsent(key, this::createRequester));
    }

    public Mono<ProcessResponse> build(MimeType mimeType, String serviceName) {
        Assert.verify(serviceName, EB_LOADBALANCE_001);
        RequesterKey key = new RequesterKey(serviceName, null, mimeType);
        return Mono.fromSupplier(() -> requesters.computeIfAbsent(key, this::createRequester));
    }

    private ProcessResponse createRequester(RequesterKey key) {
        RSocketRequester.Builder builder = builders.getObject();
        RSocketRequester requester;
        synchronized (builder) {
            builder.rsocketStrategies(strategies).dataMimeType(key.dataMimeType());
            builder.rsocketConnector(connector -> connector.maxInboundPayloadSize(maxInboundPayloadBytes));
            requester = key.port() == null
                    ? builder.transports(loadBalanceTargets(key.target()), new RoundRobinLoadbalanceStrategy())
                    : builder.transport(TcpClientTransport.create(key.target(), key.port()));
        }
        return new ProcessResponse(requester);
    }

    private Flux<List<LoadbalanceTarget>> loadBalanceTargets(String serviceName) {
        return Mono.fromCallable(() -> discoveryClient.getInstances(serviceName).stream()
                        .map(instance -> LoadbalanceTarget.from(instance.getHost() + ":" + instance.getPort(),
                                TcpClientTransport.create(instance.getHost(), instance.getPort())))
                        .toList())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.warn("RSocket discovery failed for {}", serviceName, error);
                    return Mono.empty();
                })
                .repeatWhen(completions -> completions.delayElements(Duration.ofSeconds(30)));
    }

    @PreDestroy
    void close() {
        requesters.values().forEach(response -> response.requester.rsocketClient().dispose());
        requesters.clear();
    }
}
