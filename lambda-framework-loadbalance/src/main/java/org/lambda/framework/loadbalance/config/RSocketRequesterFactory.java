package org.lambda.framework.loadbalance.config;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static org.lambda.framework.nacos.enums.LoadBalanceExceptionEnum.*;

@Component
public class RSocketRequesterFactory {
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private RSocketRequester.Builder builder;

    public RSocketRequester build(String serviceName){
        Assert.verify(serviceName,EB_LOADBALANCE_001);
        RSocketRequester requester = builder.transports(loadBalanceTargets(serviceName),new RoundRobinLoadbalanceStrategy());
        return requester;
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
        }).flux(); // <- continuously retrieve new List of ServiceInstances
    }
/*    public void test(String... args) throws Exception {
        RSocketRequester requester = builder.transports(convertToLoadbalanceTargets("ace-microservices-user"),new RoundRobinLoadbalanceStrategy());
        Map map = new HashMap<>();
        map.put("mobilePhoneNo","18758206911");
        map.put("captcha","774471");
        Mono<String> response = requester.route("/ace/microservices/user/login/smsLogin")
                .data(map)
                .retrieveMono(String.class);
        response.subscribe(e->{
            System.out.println(e);
        });
    }*/
}
