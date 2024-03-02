package org.lambda.framework.loadbalance.factory;

import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.Assert;
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

import static org.lambda.framework.nacos.enums.LoadBalanceExceptionEnum.*;

@Component
public class RSocketLoadbalance {
    @Resource
    private DiscoveryClient discoveryClient;
    @Resource
    private RSocketRequester.Builder builder;

    public RSocketRequester build(String ip,Integer port){
        RSocketStrategies strategies = RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder())  // 使用 ByteArrayEncoder 处理二进制数据的编码
                .decoder(new Jackson2JsonDecoder())  // 使用 ByteArrayDecoder 处理二进制数据的解码
                .build();
        Assert.verify(ip,EB_LOADBALANCE_001);
        Assert.verify(port,EB_LOADBALANCE_004);
        RSocketRequester requester = builder
                .rsocketStrategies(strategies)
                .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                .metadataMimeType(MimeTypeUtils.parseMimeType("message/x.rsocket.routing.v0"))
                .transport(TcpClientTransport.create(ip,port));
        return requester;
    }

    public RSocketRequester build(String serviceName){
        Assert.verify(serviceName,EB_LOADBALANCE_001);
        RSocketStrategies strategies = RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder())  // 使用 ByteArrayEncoder 处理二进制数据的编码
                .decoder(new Jackson2JsonDecoder())  // 使用 ByteArrayDecoder 处理二进制数据的解码
                .build();
        RSocketRequester requester = builder
                .rsocketStrategies(strategies)
                .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                .metadataMimeType(MimeTypeUtils.parseMimeType("message/x.rsocket.routing.v0"))
                .transports(loadBalanceTargets(serviceName),new RoundRobinLoadbalanceStrategy());
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
        }).flux();
    }
}
