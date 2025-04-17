package org.lambda.framework.loadbalance.factory;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.Resource;
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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.lambda.framework.loadbalance.enums.LoadBalanceExceptionEnum.*;

@Component
public class RSocketLoadbalance {
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private RSocketRequester.Builder builder;
    @Resource
    private PrincipalStash principalStash;
    @Resource
    private RSocketStrategies strategies;

    Logger log = Logger.getLogger(RSocketLoadbalance.class.getName());

    private final ConcurrentHashMap<String, Mono<RSocketRequester>> requesterIpCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RequesterWrapper> requesterServiceCache = new ConcurrentHashMap<>();

    private static SecurityStash defaultSecurityStash = SecurityStash.builder().build();

    public static class RequesterWrapper {
        private final Mono<RSocketRequester> requester;
        private final List<LoadbalanceTarget> targets;

        public RequesterWrapper(Mono<RSocketRequester> requester, List<LoadbalanceTarget> targets) {
            this.requester = requester;
            this.targets = targets;
        }

        public Mono<RSocketRequester> getRequester() {
            return requester;
        }

        public List<LoadbalanceTarget> getTargets() {
            return targets;
        }
    }

    private boolean deepCompareTargets(List<LoadbalanceTarget> oldTargets, List<LoadbalanceTarget> newTargets) {
        if (oldTargets.size() != newTargets.size()) {
            return false; // 长度不一致，直接返回 false
        }

        for (int i = 0; i < oldTargets.size(); i++) {
            if (!oldTargets.get(i).equals(newTargets.get(i))) {
                return false; // 找到不同的元素，返回 false
            }
        }
        return true; // 都一致时返回 true
    }

    public Mono<RSocketRequester> build(String serviceName, MimeType mimeType) {
        Assert.verify(serviceName, EB_LOADBALANCE_001);
        String cacheKey = serviceName + "::" + mimeType.toString();

        return loadBalanceTargets(serviceName)
                .next()
                .flatMap(currentTargets -> {
                    RequesterWrapper cachedWrapper = requesterServiceCache.get(cacheKey);

                    // 如果目标列表一致，复用缓存
                    if (cachedWrapper != null && deepCompareTargets(cachedWrapper.getTargets(), currentTargets)) {
                        return cachedWrapper.getRequester();
                    }

                    // 如果不一致，先关闭旧连接
                    if (cachedWrapper != null) {
                        cachedWrapper.getRequester()
                                .subscribe(req -> {
                                    if(!req.isDisposed()){
                                        req.dispose(); // 释放旧连接
                                        log.info("释放连接:------------------>>>>> " + cacheKey);
                                    }
                                });
                    }

                    // 创建新的 requester 并缓存
                    Mono<RSocketRequester> newRequester = principalStash.setSecurityStash()
                            .onErrorReturn(defaultSecurityStash)
                            .defaultIfEmpty(defaultSecurityStash)
                            .map(securityStash ->
                                    setRSocketRequester(getBuilder(), securityStash)
                                            .dataMimeType(mimeType)
                                            .transports(Flux.just(currentTargets), new RoundRobinLoadbalanceStrategy())
                            )
                            .cache();

                    // 缓存新的请求器
                    requesterServiceCache.put(cacheKey, new RequesterWrapper(newRequester, currentTargets));

                    return newRequester;
                });
    }



    public Mono<RSocketRequester> build(String ip, Integer port, MimeType mimeType) {
        Assert.verify(ip, EB_LOADBALANCE_005);
        Assert.verify(port, EB_LOADBALANCE_006);
        String cacheKey = ip + ":" + port + "::" + mimeType.toString();

        return requesterIpCache.computeIfAbsent(cacheKey, key ->
                principalStash.setSecurityStash()
                        .onErrorReturn(defaultSecurityStash)
                        .defaultIfEmpty(defaultSecurityStash)
                        .map(securityStash ->
                                setRSocketRequester(getBuilder(), securityStash)
                                        .dataMimeType(mimeType)
                                        .transport(TcpClientTransport.create(ip, port))
                        )
                        .cache()
        );
    }

    public RSocketRequester.Builder setRSocketRequester(RSocketRequester.Builder requester, SecurityStash securityStash) {
        if (securityStash != null) {
            requester.setupData(securityStash);
        }
        return requester;
    }

    private RSocketRequester.Builder getBuilder() {
        builder.rsocketStrategies(strategies);
        return builder;
    }

    private Flux<List<LoadbalanceTarget>> loadBalanceTargets(String serviceName) {
        return Mono.fromCallable(() -> {
            Assert.verify(serviceName, EB_LOADBALANCE_001);
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
            Assert.verify(serviceInstances, EB_LOADBALANCE_002);
            return serviceInstances.stream()
                    .map(instance -> LoadbalanceTarget.from(instance.getServiceId(), TcpClientTransport.create(instance.getHost(), instance.getPort())))
                    .collect(Collectors.toList());
        }).flux();
    }
}
