package org.lambda.framework.loadbalance.autoconfigure;

import org.lambda.framework.common.support.PrincipalStash;
import org.lambda.framework.common.support.SecurityStash;
import org.lambda.framework.loadbalance.factory.RSocketClientResources;
import org.lambda.framework.loadbalance.factory.RSocketLoadbalance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import reactor.core.publisher.Mono;

@AutoConfiguration
public class RSocketLoadbalanceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    RSocketClientResources rSocketClientResources(
            @Value("${lambda.rsocket.client.io-workers.unary:2}") int unaryWorkers,
            @Value("${lambda.rsocket.client.io-workers.stream:2}") int streamWorkers,
            @Value("${lambda.rsocket.client.io-workers.channel:2}") int channelWorkers) {
        return new RSocketClientResources(unaryWorkers, streamWorkers, channelWorkers);
    }

    @Bean
    @ConditionalOnMissingBean
    RSocketLoadbalance rSocketLoadbalance(DiscoveryClient discoveryClient,
            ObjectProvider<RSocketRequester.Builder> builders, RSocketStrategies strategies,
            RSocketClientResources resources,
            @Value("${lambda.rsocket.client.max-inbound-payload-bytes:16777216}") int maxInboundPayloadBytes,
            @Value("${lambda.rsocket.client.connections-per-instance:2}") int connectionsPerInstance) {
        return new RSocketLoadbalance(discoveryClient, builders, strategies, resources,
                maxInboundPayloadBytes, connectionsPerInstance);
    }

    @Bean
    @ConditionalOnMissingBean
    PrincipalStash rsocketPrincipalStash() {
        return () -> Mono.deferContextual(context -> Mono.justOrEmpty(context.<SecurityStash>getOrEmpty(SecurityStash.class)));
    }
}
