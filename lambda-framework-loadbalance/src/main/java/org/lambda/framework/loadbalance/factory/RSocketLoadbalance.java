package org.lambda.framework.loadbalance.factory;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.support.SecurityStash;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import static org.lambda.framework.common.enums.ConmonContract.AUTHTOKEN_STASH_NAMING;
import static org.lambda.framework.common.enums.ConmonContract.PRINCIPAL_STASH_NAMING;
import static org.lambda.framework.nacos.enums.LoadBalanceExceptionEnum.*;

@Component
public class RSocketLoadbalance {
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private RSocketRequester.Builder builder;
    @Resource
    private RsocketPrincipalStash rsocketPrincipalStash;
    @Resource
    private RSocketStrategies strategies;

    public Mono<RSocketRequester> build(String ip, Integer port){
        Assert.verify(ip,EB_LOADBALANCE_005);
        Assert.verify(port,EB_LOADBALANCE_006);
        return rsocketPrincipalStash.setSecurityStash()
                .onErrorReturn(defaultSecurityStash)
                .defaultIfEmpty(defaultSecurityStash).map(securityStash->{
                    return setRSocketRequester(getBuilder(),securityStash)
                                .transport(TcpClientTransport.create(ip,port));
        });
    }


    public Mono<RSocketRequester> build(String serviceName){
        Assert.verify(serviceName,EB_LOADBALANCE_001);
        return rsocketPrincipalStash.setSecurityStash()
                .onErrorReturn(defaultSecurityStash)
                .defaultIfEmpty(defaultSecurityStash).map(securityStash->{
                 return setRSocketRequester(getBuilder(),securityStash)
                            .transports(loadBalanceTargets(serviceName),new RoundRobinLoadbalanceStrategy());
        });
    }

    public RSocketRequester.Builder setRSocketRequester(RSocketRequester.Builder requester,SecurityStash securityStash){
        if(StringUtils.isNotBlank(securityStash.getAuthToken())){
            requester.setupMetadata(securityStash.getAuthToken(), MimeTypeUtils.parseMimeType(AUTHTOKEN_STASH_NAMING));
        }
        if(StringUtils.isNotBlank(securityStash.getPrincipal())){
            requester.setupMetadata(securityStash.getPrincipal(), MimeTypeUtils.parseMimeType(PRINCIPAL_STASH_NAMING));
        }

        return requester;
    }

    private RSocketRequester.Builder getBuilder(){
        builder.rsocketStrategies(strategies);
        return builder;
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

    private static SecurityStash defaultSecurityStash= SecurityStash.builder().build();


    public interface RsocketPrincipalStash{
        public Mono<SecurityStash> setSecurityStash();}
    @Component
    @ConditionalOnMissingBean(RsocketPrincipalStash.class)
    public static class Stash implements RsocketPrincipalStash{

        @Override
        public Mono<SecurityStash> setSecurityStash() {
            return Mono.just(defaultSecurityStash);
        }

    }


    @Configuration
    public static class RsocketLoadbalanceConfig {
        @Bean
        public RSocketStrategies getStrategies(){
            RSocketStrategies strategies = RSocketStrategies.builder()
                    .encoder(new Jackson2JsonEncoder())
                    .decoder(new Jackson2JsonDecoder())
                    .build();
            return strategies;
        }
    }
}
