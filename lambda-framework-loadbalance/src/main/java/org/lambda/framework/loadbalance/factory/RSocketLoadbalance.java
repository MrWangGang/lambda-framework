package org.lambda.framework.loadbalance.factory;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lambda.framework.common.exception.Assert;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static org.lambda.framework.common.enums.SecurityContract.LAMBDA_EMPTY_PRINCIPAL;
import static org.lambda.framework.common.enums.SecurityContract.PRINCIPAL_STASH_NAMING;
import static org.lambda.framework.nacos.enums.LoadBalanceExceptionEnum.EB_LOADBALANCE_001;
import static org.lambda.framework.nacos.enums.LoadBalanceExceptionEnum.EB_LOADBALANCE_002;

@Component
public class RSocketLoadbalance {
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private RSocketRequester.Builder builder;
    @Resource
    private RsocketPrincipalStash rsocketPrincipalStash;

    public Mono<RSocketRequester> build(String ip,Integer port){
        RSocketRequester rSocketRequester = getBuilder().transport(TcpClientTransport.create(ip,port));
        return rsocketPrincipalStash.getPrincipal().defaultIfEmpty(LAMBDA_EMPTY_PRINCIPAL).map(principal->{
            rSocketRequester.metadata(principal,MimeTypeUtils.parseMimeType(PRINCIPAL_STASH_NAMING));
            return rSocketRequester;
        });
    }

    public Mono<RSocketRequester> build(String serviceName){
        Assert.verify(serviceName,EB_LOADBALANCE_001);
        RSocketRequester rSocketRequester = getBuilder().transports(loadBalanceTargets(serviceName),new RoundRobinLoadbalanceStrategy());
        return rsocketPrincipalStash.getPrincipal().defaultIfEmpty(LAMBDA_EMPTY_PRINCIPAL).map(principal->{
            rSocketRequester.metadata(principal,MimeTypeUtils.parseMimeType(PRINCIPAL_STASH_NAMING));
            return rSocketRequester;
        });
    }

    public RSocketRequester.Builder getBuilder(){
        RSocketStrategies strategies = RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder())
                .decoder(new Jackson2JsonDecoder())
                .build();
        return builder
                .rsocketStrategies(strategies)
                .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                .metadataMimeType(MimeTypeUtils.parseMimeType(PRINCIPAL_STASH_NAMING));
    }
    private Flux<List<LoadbalanceTarget>> loadBalanceTargets(String serviceName) {
        return Mono.fromCallable(() -> {
            Assert.verify(serviceName,EB_LOADBALANCE_001);
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
            Assert.verify(serviceInstances,EB_LOADBALANCE_002);
            return serviceInstances
                    .stream()
                    .map(instance -> LoadbalanceTarget.from(instance.getServiceId(), TcpClientTransport.create(instance.getHost(), instance.getPort())))
                    .collect(Collectors.toList());
        }).flux();
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetadataBuilder{
        private String key;
        private String value;
    }

    public interface RsocketPrincipalStash{
        public Mono<String> getPrincipal();
    }
    @Component
    @ConditionalOnMissingBean(RsocketPrincipalStash.class)
    public static class Stash implements RsocketPrincipalStash{
        @Override
        public Mono<String> getPrincipal() {
            return Mono.just(LAMBDA_EMPTY_PRINCIPAL);
        }
    }
}
