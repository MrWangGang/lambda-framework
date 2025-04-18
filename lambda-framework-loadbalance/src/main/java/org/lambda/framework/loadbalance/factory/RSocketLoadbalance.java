package org.lambda.framework.loadbalance.factory;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.SecurityStash;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.lambda.framework.loadbalance.enums.LoadBalanceExceptionEnum.*;

@Component
public class RSocketLoadbalance {
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private RSocketRequester.Builder builder;
    @Resource
    private RSocketStrategies strategies;

    private ConcurrentHashMap<String, ProcessResponse> requesters = new ConcurrentHashMap<>();


    private static final SecurityStash defaultSecurityStash = SecurityStash.builder().build();

    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessResponse {

        private RSocketRequester requester;

        public <T> Mono<T> retrieveMono(PrincipalStash principalStash,MimeType mimeType,String route, Object data, Class<T> dataType) {
            return principalStash.setSecurityStash()
                    .onErrorReturn(defaultSecurityStash)
                    .defaultIfEmpty(defaultSecurityStash).flatMap(securityStash -> {
                        return requester.route(route).metadata(securityStash,mimeType).data(data).retrieveMono(dataType);
                    });
        }

        public <T> Flux<T> retrieveFlux(PrincipalStash principalStash,MimeType mimeType,String route, Object data, Class<T> dataType) {
            return principalStash.setSecurityStash()
                    .onErrorReturn(defaultSecurityStash)
                    .defaultIfEmpty(defaultSecurityStash).flatMapMany(securityStash -> {
                        return requester.route(route).metadata(securityStash,mimeType).data(data).retrieveMono(dataType);
                    });
        }
    }


    public Mono<ProcessResponse> build(MimeType mimeType,String ip, Integer port) {
        Assert.verify(ip, EB_LOADBALANCE_005);
        Assert.verify(port, EB_LOADBALANCE_006);
        String key = ip + ":" + port;
        ProcessResponse response = requesters.get(key);
        if (response != null) {
            return Mono.just(response);
        }

        RSocketRequester requester = getBuilder().dataMimeType(mimeType)
                .transport(TcpClientTransport.create(ip, port));
        ProcessResponse rs = ProcessResponse.builder().requester(requester).build();
        requesters.put(key, rs);
        return Mono.just(rs);
    }


    public Mono<ProcessResponse> build(MimeType mimeType,String serviceName) {
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
        Assert.verify(serviceName, EB_LOADBALANCE_001);
        Assert.verify(serviceInstances, EB_LOADBALANCE_002);
        ProcessResponse response = requesters.get(serviceName);
        if (response != null) {
            return Mono.just(response);
        }

        RSocketRequester requester = getBuilder().dataMimeType(mimeType)
                .transports(loadBalanceTargets(serviceName), new RoundRobinLoadbalanceStrategy());
        ProcessResponse rs = ProcessResponse.builder().requester(requester).build();
        requesters.put(serviceName, rs);
        return Mono.just(rs);
    }

    private RSocketRequester.Builder getBuilder() {
        builder.rsocketStrategies(strategies);
        return builder;
    }


    private Flux<List<LoadbalanceTarget>> loadBalanceTargets(String serviceName) {
        return Flux.defer(() -> {
                    List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
                    if (Assert.verify(serviceInstances)) {
                        List<LoadbalanceTarget> targets = serviceInstances.stream()
                                .map(instance -> LoadbalanceTarget.from(
                                        instance.getServiceId(),
                                        TcpClientTransport.create(instance.getHost(), instance.getPort())))
                                .collect(Collectors.toList());
                        return Mono.just(targets);
                    }
                    return Mono.empty();
                })
                .repeatWhen(f -> f.delayElements(Duration.ofSeconds(30)));
    }

}
