package org.lambda.framework.loadbalance.factory;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
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

    private static final SecurityStash defaultSecurityStash= SecurityStash.builder().build();
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessResponse{

        private RSocketRequester requester;

        public <T> Mono<T> retrieveMono(String route,Object data, Class<T> dataType){
            return requester.route(route).data(data).retrieveMono(dataType).doFinally(e->requester.dispose());
        }

        public <T> Flux<T> retrieveFlux(String route,Object data,Class<T> dataType){
            return requester.route(route).data(data).retrieveFlux(dataType).doFinally(e->requester.dispose());
        }

        private RSocketRequester getRequester() {
            return requester;
        }

        public void setRequester(RSocketRequester requester) {
            this.requester = requester;
        }

    }



    public Mono<ProcessResponse> build(String ip, Integer port, MimeType mimeType){
        Assert.verify(ip,EB_LOADBALANCE_005);
        Assert.verify(port,EB_LOADBALANCE_006);
        return principalStash.setSecurityStash()
                .onErrorReturn(defaultSecurityStash)
                .defaultIfEmpty(defaultSecurityStash).map(securityStash->{
                    RSocketRequester requester = setRSocketRequester(getBuilder(),securityStash)
                            .dataMimeType(mimeType)
                            .transport(TcpClientTransport.create(ip,port));
                    return ProcessResponse.builder().requester(requester).build();
                });
    }


    public Mono<ProcessResponse> build(String serviceName,MimeType mimeType){
        Assert.verify(serviceName,EB_LOADBALANCE_001);
        return principalStash.setSecurityStash()
                .onErrorReturn(defaultSecurityStash)
                .defaultIfEmpty(defaultSecurityStash).map(securityStash->{
                    RSocketRequester requester = setRSocketRequester(getBuilder(),securityStash)
                            .dataMimeType(mimeType)
                            .transports(loadBalanceTargets(serviceName),new RoundRobinLoadbalanceStrategy());
                    return ProcessResponse.builder().requester(requester).build();
                });
    }

    public RSocketRequester.Builder setRSocketRequester(RSocketRequester.Builder requester, SecurityStash securityStash){
        if(securityStash!=null){
            requester.setupData(securityStash);
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


}
