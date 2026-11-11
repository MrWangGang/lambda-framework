package org.lambda.framework.loadbalance.factory;

import io.rsocket.DuplexConnection;
import io.rsocket.RSocket;
import io.rsocket.exceptions.ConnectionErrorException;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.RSocketProxy;
import jakarta.annotation.PreDestroy;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.enums.RSocketContract;
import org.lambda.framework.common.support.GatewayRouteContract;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.ServiceInstanceSource;
import org.lambda.framework.common.support.ServiceInstanceSource.Endpoint;
import org.lambda.framework.common.util.OptionalUtil;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.lambda.framework.enums.LoadBalanceExceptionEnum.*;

public class RSocketLoadbalance {
    private static final MimeType ROUTE_VERSION_MIME = MimeType.valueOf(GatewayRouteContract.SETUP_MIME_TYPE);
    private static final Logger log = LoggerFactory.getLogger(RSocketLoadbalance.class);
    private static final Retry TCP_CONNECT_RETRY = Retry.backoff(2, Duration.ofMillis(250))
            .maxBackoff(Duration.ofSeconds(1)).onRetryExhaustedThrow((spec, failure) -> failure.failure());
    private final DiscoveryClient discoveryClient;
    private final ObjectProvider<RSocketRequester.Builder> builders;
    private final RSocketStrategies strategies;
    private final RSocketClientResources resources;
    private final int maxInboundPayloadBytes;
    private final int connectionsPerInstance;
    private final boolean adaptiveLoadBalancing;
    private final Optional<ServiceInstanceSource> instanceSource;
    private final ConcurrentHashMap<String, Flux<Set<Endpoint>>> serviceInstances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RequesterKey, ProcessResponse> requesters = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public enum Traffic { UNARY, STREAM, CHANNEL }

    private record RequesterKey(String target, Integer port, MimeType dataMimeType,
                                int connectionsPerInstance, Traffic traffic, boolean gateway) {}

    public RSocketLoadbalance(DiscoveryClient discoveryClient, ObjectProvider<RSocketRequester.Builder> builders,
                              RSocketStrategies strategies, RSocketClientResources resources,
                              int maxInboundPayloadBytes, int connectionsPerInstance) {
        this(discoveryClient, builders, strategies, resources, maxInboundPayloadBytes, connectionsPerInstance, true);
    }

    public RSocketLoadbalance(DiscoveryClient discoveryClient, ObjectProvider<RSocketRequester.Builder> builders,
                              RSocketStrategies strategies, RSocketClientResources resources,
                              int maxInboundPayloadBytes, int connectionsPerInstance, boolean adaptiveLoadBalancing) {
        this(discoveryClient, builders, strategies, resources, maxInboundPayloadBytes, connectionsPerInstance,
                adaptiveLoadBalancing, Optional.empty());
    }

    public RSocketLoadbalance(DiscoveryClient discoveryClient, ObjectProvider<RSocketRequester.Builder> builders,
                              RSocketStrategies strategies, RSocketClientResources resources,
                              int maxInboundPayloadBytes, int connectionsPerInstance, boolean adaptiveLoadBalancing,
                              Optional<ServiceInstanceSource> instanceSource) {
        this.discoveryClient = discoveryClient;
        this.builders = builders;
        this.strategies = strategies;
        this.resources = resources;
        this.maxInboundPayloadBytes = maxInboundPayloadBytes;
        this.connectionsPerInstance = validatedConnections(connectionsPerInstance);
        this.adaptiveLoadBalancing = adaptiveLoadBalancing;
        this.instanceSource = instanceSource;
    }

    public static final class ProcessResponse {
        private static final MimeType FORY = MimeType.valueOf(RSocketContract.FORY_MIME_TYPE);
        private final RSocketRequester requester;
        private final AdaptiveRSocketPool adaptive;
        private final MimeType identityMime;
        private final Set<Endpoint> eligible;

        private ProcessResponse(RSocketRequester requester) {
            this.requester = requester;
            this.adaptive = null;
            this.eligible = null;
            this.identityMime = identityMime(requester.dataMimeType());
        }

        private ProcessResponse(AdaptiveRSocketPool adaptive, MimeType identityMime, Set<Endpoint> eligible) {
            this.requester = null;
            this.adaptive = adaptive;
            this.identityMime = identityMime;
            this.eligible = eligible;
        }

        private ProcessResponse restrict(Set<Endpoint> endpoints) {
            return new ProcessResponse(adaptive, identityMime, endpoints);
        }

        private static MimeType identityMime(MimeType dataMimeType) {
            return FORY.equalsTypeAndSubtype(dataMimeType) ? FORY : MimeTypeUtils.APPLICATION_JSON;
        }

        public <T> Mono<T> retrieveMono(PrincipalStash principalStash, MimeType mimeType, String route,
                                         Object data, Class<T> dataType) {
            return unary(client -> request(client, principalStash, route)
                    .flatMap(spec -> spec.data(data).retrieveMono(dataType)));
        }

        public <T> Flux<T> retrieveFlux(PrincipalStash principalStash, MimeType mimeType, String route,
                                         Object data, Class<T> dataType) {
            return stream(client -> request(client, principalStash, route)
                    .flatMapMany(spec -> spec.data(data).retrieveFlux(dataType)));
        }

        public <T> Mono<T> retrieveMono(PrincipalStash principalStash, String route,
                                        Object data, ParameterizedTypeReference<T> dataType) {
            return unary(client -> request(client, principalStash, route)
                    .flatMap(spec -> spec.data(data).retrieveMono(dataType)));
        }

        public <T> Flux<T> retrieveFlux(PrincipalStash principalStash, String route,
                                        Object data, ParameterizedTypeReference<T> dataType) {
            return stream(client -> request(client, principalStash, route)
                    .flatMapMany(spec -> spec.data(data).retrieveFlux(dataType)));
        }

        public <T> Flux<T> retrieveChannel(PrincipalStash principalStash, String route,
                                            Publisher<byte[]> data, Class<T> dataType) {
            return stream(client -> request(client, principalStash, route)
                    .flatMapMany(spec -> spec.data(data, byte[].class).retrieveFlux(dataType)));
        }

        private <T> Mono<T> unary(Function<RSocketRequester, Mono<T>> action) {
            return adaptive == null ? action.apply(requester) : adaptive.request(action, eligible);
        }

        private <T> Flux<T> stream(Function<RSocketRequester, Flux<T>> action) {
            return adaptive == null ? action.apply(requester) : adaptive.stream(action, eligible);
        }

        private void close() {
            if (adaptive == null) {
                requester.rsocketClient().dispose();
            } else {
                adaptive.close();
            }
        }

        private Mono<RSocketRequester.RequestSpec> request(RSocketRequester requester,
                                                           PrincipalStash principalStash, String route) {
            return Mono.defer(principalStash::setSecurityStash)
                    .filter(stash -> stash.getAuthToken() != null || stash.getPrincipal() != null)
                    .map(stash -> requester.route(route).metadata(stash, identityMime))
                    .switchIfEmpty(Mono.fromSupplier(() -> requester.route(route)));
        }
    }

    public Mono<ProcessResponse> build(MimeType mimeType, String ip, Integer port) {
        Assert.verify(ip, HOST_REQUIRED);
        Assert.verify(port, PORT_REQUIRED);
        RequesterKey key = new RequesterKey(ip, port, mimeType, 1, Traffic.UNARY, false);
        return Mono.fromSupplier(() -> requester(key));
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
        var key = new RequesterKey(serviceName, null, mimeType, validatedConnections(connections), traffic, false);
        return Mono.fromSupplier(() -> requester(key));
    }

    public Mono<ProcessResponse> buildForGateway(MimeType mimeType, String service, Traffic traffic,
                                                  Optional<Integer> connections, Set<Endpoint> eligible) {
        Assert.verify(service, SERVICE_NAME_REQUIRED);
        int count = connections.map(RSocketLoadbalance::validatedConnections).orElse(connectionsPerInstance);
        var key = new RequesterKey(service, null, mimeType, count, traffic, true);
        return Mono.fromSupplier(() -> requester(key).restrict(eligible));
    }

    public <T> Mono<T> withEndpoint(MimeType mimeType, Endpoint endpoint,
                                    Function<ProcessResponse, Mono<T>> action) {
        return Mono.using(() -> new ProcessResponse(directRequester(mimeType, endpoint.host(), endpoint.port(), Traffic.UNARY)),
                action, ProcessResponse::close);
    }

    public Flux<Set<Endpoint>> watchService(String service) {
        return serviceInstances.computeIfAbsent(service, target -> instanceSource
                .map(source -> Flux.defer(() -> source.watch(target)))
                .orElseGet(() -> discover(target).map(instances -> instances.stream()
                        .map(instance -> new Endpoint(instance.getHost(), instance.getPort(),
                                instance.getMetadata().getOrDefault(GatewayRouteContract.METADATA_KEY, "")))
                        .collect(Collectors.toUnmodifiableSet())))
                .distinctUntilChanged()
                .replay(1).refCount(1));
    }

    private ProcessResponse requester(RequesterKey key) {
        if (closed) {
            throw new ConnectionErrorException("RSocket client is closed");
        }
        ProcessResponse response = requesters.computeIfAbsent(key, this::createRequester);
        if (closed) {
            requesters.remove(key, response);
            response.close();
            throw new ConnectionErrorException("RSocket client is closed");
        }
        return response;
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

    private RSocketRequester directRequester(MimeType dataMimeType, String host, int port, Traffic traffic) {
        return directRequester(dataMimeType, host, port, traffic, "");
    }

    private RSocketRequester directRequester(MimeType dataMimeType, String host, int port, Traffic traffic,
                                            String routeVersion) {
        RSocketRequester.Builder builder = builders.getObject();
        synchronized (builder) {
            builder.rsocketStrategies(strategies).dataMimeType(dataMimeType)
                    .rsocketConnector(connector -> connector.maxInboundPayloadSize(maxInboundPayloadBytes));
            if (!routeVersion.isEmpty()) {
                builder.setupMetadata(routeVersion.getBytes(StandardCharsets.US_ASCII), ROUTE_VERSION_MIME);
            }
            return builder.transport(tcpTransport(host, port, traffic));
        }
    }

    private ProcessResponse createRequester(RequesterKey key) {
        if (key.port() != null) {
            return new ProcessResponse(directRequester(key.dataMimeType(), key.target(), key.port(), key.traffic()));
        }
        boolean adaptiveSelection = adaptiveLoadBalancing && key.traffic() == Traffic.UNARY;
        if (adaptiveSelection || key.gateway()) {
            var adaptive = new AdaptiveRSocketPool(key.target(), key.connectionsPerInstance(), adaptiveSelection,
                    watchService(key.target()),
                    address -> directRequester(key.dataMimeType(), address.host(), address.port(), key.traffic(),
                            key.gateway() ? address.routeVersion() : ""));
            return new ProcessResponse(adaptive, ProcessResponse.identityMime(key.dataMimeType()), null);
        }

        RSocketRequester.Builder builder = builders.getObject();
        synchronized (builder) {
            builder.rsocketStrategies(strategies).dataMimeType(key.dataMimeType());
            // The pool owns the subscription. A single pending signal is enough to replay its target list.
            Sinks.Many<Boolean> reconnect = Sinks.many().unicast()
                    .onBackpressureBuffer(new ArrayBlockingQueue<>(1));
            builder.rsocketConnector(connector -> connector.maxInboundPayloadSize(maxInboundPayloadBytes)
                    .interceptors(registry -> registry.forRequester((RSocket socket) -> new RSocketProxy(socket) {
                        @Override
                        public Mono<Void> onClose() {
                            // Signal after RSocket's pool has handled the close and evicted this socket.
                            return super.onClose().doAfterTerminate(() -> reconnect.tryEmitNext(true));
                        }
                    })));
            Flux<Long> triggers = reconnect.asFlux()
                    .concatMap(signal -> Mono.delay(Duration.ofMillis(ThreadLocalRandom.current().nextLong(100, 251))), 0)
                    .startWith(0L);
            Flux<List<LoadbalanceTarget>> targets = Flux.combineLatest(loadBalanceTargets(key, reconnect),
                    triggers, (snapshot, ignored) -> snapshot);
            return new ProcessResponse(builder.transports(targets, new RoundRobinLoadbalanceStrategy()));
        }
    }

    private Flux<List<LoadbalanceTarget>> loadBalanceTargets(RequesterKey key, Sinks.Many<Boolean> reconnect) {
        return watchService(key.target()).map(instances -> instances.stream().flatMap(instance -> {
            String address = instance.host() + ":" + instance.port() + "/" + instance.routeVersion();
            var transport = new RecoveringTransport(
                    tcpTransport(instance.host(), instance.port(), key.traffic()), reconnect);
            return IntStream.range(0, key.connectionsPerInstance()).mapToObj(index ->
                    LoadbalanceTarget.from(index == 0 ? address : address + "#" + index, transport));
        }).toList());
    }

    private Flux<List<ServiceInstance>> discover(String service) {
        return Mono.fromCallable(() -> OptionalUtil.listOrEmpty(discoveryClient.getInstances(service)))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(error -> {
                    log.warn("RSocket discovery failed for {}", service, error);
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
        closed = true;
        requesters.values().forEach(ProcessResponse::close);
        requesters.clear();
        serviceInstances.clear();
    }
}
