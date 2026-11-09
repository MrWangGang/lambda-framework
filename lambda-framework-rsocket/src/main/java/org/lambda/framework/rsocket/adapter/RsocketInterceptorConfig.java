package org.lambda.framework.rsocket.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({GatewayStreamContractController.class, RsocketInterceptorConfig.RejectHttpListener.class})
public class RsocketInterceptorConfig  {
    @Bean
    public RSocketServerCustomizer rSocketServerCustomizer(RSocketAcceptorInterceptor interceptor,
            @Value("${lambda.rsocket.server.max-inbound-payload-bytes:16777216}") int maxInboundPayloadBytes,
            @Value("${spring.rsocket.server.transport:tcp}") String transport) {
        if (!"tcp".equalsIgnoreCase(transport)) {
            throw new IllegalStateException("Business RSocket services require spring.rsocket.server.transport=tcp");
        }
        return server -> server.maxInboundPayloadSize(maxInboundPayloadBytes)
                .interceptors(registry -> registry.forSocketAcceptor(interceptor)
                        .forSocketAcceptor(new RpcDataMimeInterceptor()));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication
    static class RejectHttpListener {
        RejectHttpListener() {
            throw new IllegalStateException("Business RSocket services must use spring.main.web-application-type=none; expose HTTP through the gateway");
        }
    }
}
