package org.lambda.framework.loadbalance.factory;

import io.rsocket.DuplexConnection;
import io.rsocket.RSocket;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.RSocketProxy;
import jakarta.annotation.PreDestroy;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.enums.RSocketContract;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.util.OptionalUtil;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.lambda.framework.enums.LoadBalanceExceptionEnum.*;

public class RSocketLoadbalance {
    private static final Logger log = LoggerFactory.getLogger(RSocketLoadbalance.class);
    private static final Retry TCP_CONNECT_RETRY = Retry.backoff(2, Duration.ofMillis(250))
            .maxBackoff(Duration.ofSeconds(1)).onRetryExhaustedThrow((spec, failure) -> failure.failure());
    private final DiscoveryClient discoveryClient;
    private final ObjectProvider<RSocketRequester.Builder> builders;
    private final RSocketStrategies strategies;
    private final RSocketClientResources resources;
    private final int maxInboundPayloadBytes;
    private final int connectionsPerInstance;
    private final ConcurrentHashMap<RequesterKey, ProcessResponse> requesters = new ConcurrentHashMap<>();

    public enum Traffic { UNARY, STREAM, CHANNEL }

    private record RequesterKey(String target, Integer port, MimeType dataMimeType,
                                int connectionsPerInstance, Traffic traffic) {}

    public RSocketLoadbalance(DiscoveryClient discoveryClient, ObjectProvider<RSocketRequester.Builder> builders,
                              RSocketStrategies strategies, RSocketClientResources resources,
                              int maxInboundPayloadBytes, int connectionsPerInstance) {
        this.discoveryClient = discoveryClient;
        this.builders = builders;
        this.strategies = strategies;
        this.resources = resources;
        this.maxInboundPayloadBytes = maxInboundPayloadBytes;
        this.connectionsPerInstance = validatedConnections(connectionsPerInstance);
    }

    public static final class ProcessResponse {
        private static final MimeType FORY = MimeType.valueOf(RSocketContract.FORY_MIME_TYPE);
        private final RSocketRequester requester;
        private final MimeType identityMime;

        private ProcessResponse(RSocketRequester requester) {
            this.requester = requester;
            this.identityMime = FORY.equalsTypeAndSubtype(requester.dataMimeType()) ? FORY : MimeTypeUtils.APPLICATION_JSON;
        }

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
                    .filter(stash -> stash.getAuthToken() != null || stash.getPrincipal() != null)
                    .map(stash -> requester.route(route).metadata(stash, identityMime))
                    .switchIfEmpty(Mono.fromSupplier(() -> requester.route(route)));
        }
    }

    public Mono<ProcessResponse> build(MimeType mimeType, String ip, Integer port) {
        Assert.verify(ip, HOST_REQUIRED);
        Assert.verify(port, PORT_REQUIRED);
        RequesterKey key = new RequesterKey(ip, port, mimeType, 1, Traffic.UNARY);
        return Mono.fromSupplier(() -> requesters.computeIfAbsent(key, this::createRequester));
    }

    public Mono<ProcessResponse> build(MimeType mimeType, String serviceName) {
        return buildForService(mimeType, serviceName, Traffic.UNARY);
    }

    public Mono<ProcessResponse> buildForService(MimeType mimeType, String serviceName, Traffic traffic) {
        return buildForService(mimeType, serviceName, connectionsPerInstance, traffic);
    }

    public Mono<ProcessResponse> buildForService(MimeType mimeType, String serviceName, int connections) {
        return buildForService(mimeType, serviceName, connections, Traffic.UNARY);
    }

    public Mono<ProcessResponse> buildForService(MimeType mimeType, String serviceName,
                                                 int connections, Traffic traffic) {
        Assert.verify(serviceName, SERVICE_NAME_REQUIRED);
        var key = new RequesterKey(serviceName, null, mimeType, validatedConnections(connections), traffic);
        return Mono.fromSupplier(() -> requesters.computeIfAbsent(key, this::createRequester));
    }

    private static int validatedConnections(int connections) {
        if (connections < 1 || connections > 8) {
            throw new IllegalArgumentException("RSocket connections per instance must be between 1 and 8");
        }
        return connections;
    }

    private TcpClientTransport tcpTransport(String host, int port, Traffic traffic) {
        return TcpClientTransport.create(TcpClient.create().host(host).port(port).runOn(resources.forTraffic(traffic)));
    }

    private ProcessResponse createRequester(RequesterKey key) {
        RSocketRequester.Builder builder = builders.getObject();
        RSocketRequester requester;
        synchronized (builder) {
            builder.rsocketStrategies(strategies).dataMimeType(key.dataMimeType());
            if (key.port() == null) {
                // The pool owns the subscription. A single pending signal is enough to replay its target list.
                Sinks.Many<Boolean> reconnect = Sinks.many().unicast()
                        .onBackpressureBuffer(new ArrayBlockingQueue<>(1));
                builder.rsocketConnector(connector -> connector.maxInboundPayloadSize(maxInboundPayloadBytes)
                        .interceptors(registry -> registry.forRequester((RSocket socket) -> new RSocketProxy(socket) {
                            @Override
                            public Mono<Void> onClose() {
                                // Signal only after RSocket's pool has handled the close and evicted this socket.
                                return super.onClose().doAfterTerminate(() -> reconnect.tryEmitNext(true));
                            }
                        })));
                Flux<Long> triggers = reconnect.asFlux()
                        .concatMap(signal -> Mono.delay(Duration.ofMillis(ThreadLocalRandom.current().nextLong(100, 251))), 0)
                        .startWith(0L);
                Flux<List<LoadbalanceTarget>> targets = Flux.combineLatest(loadBalanceTargets(key, reconnect),
                        triggers, (snapshot, ignored) -> snapshot);
                requester = builder.transports(targets, new RoundRobinLoadbalanceStrategy());
            } else {
                builder.rsocketConnector(connector -> connector.maxInboundPayloadSize(maxInboundPayloadBytes));
                requester = builder.transport(tcpTransport(key.target(), key.port(), key.traffic()));
            }
        }
        return new ProcessResponse(requester);
    }

    private Flux<List<LoadbalanceTarget>> loadBalanceTargets(RequesterKey key, Sinks.Many<Boolean> reconnect) {
        return Mono.fromCallable(() -> OptionalUtil.listOrEmpty(discoveryClient.getInstances(key.target())).stream()
                        .flatMap(instance -> {
                            String address = instance.getHost() + ":" + instance.getPort();
                            var transport = new RecoveringTransport(
                                    tcpTransport(instance.getHost(), instance.getPort(), key.traffic()), reconnect);
                            return IntStream.range(0, key.connectionsPerInstance()).mapToObj(index ->
                                    LoadbalanceTarget.from(index == 0 ? address : address + "#" + index, transport));
                        })
                        .toList())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.warn("RSocket discovery failed for {}", key.target(), error);
                    return Mono.empty();
                })
                .repeatWhen(completions -> completions.delayElements(Duration.ofSeconds(30)));
    }

    private record RecoveringTransport(TcpClientTransport delegate, Sinks.Many<Boolean> reconnect)
            implements ClientTransport {
        @Override
        public Mono<DuplexConnection> connect() {
            return delegate.connect().retryWhen(TCP_CONNECT_RETRY).doFinally(signal -> {
                if (signal == SignalType.ON_ERROR) {
                    reconnect.tryEmitNext(true);
                }
            });
        }

        @Override
        public int maxFrameLength() {
            return delegate.maxFrameLength();
        }
    }

    @PreDestroy
    void close() {
        requesters.values().forEach(response -> response.requester.rsocketClient().dispose());
        requesters.clear();
    }
}
