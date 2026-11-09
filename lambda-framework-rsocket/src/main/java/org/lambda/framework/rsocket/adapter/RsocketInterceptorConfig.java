package org.lambda.framework.rsocket.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsocketInterceptorConfig  {
    @Bean
    public RSocketServerCustomizer rSocketServerCustomizer(RSocketAcceptorInterceptor interceptor,
            @Value("${lambda.rsocket.server.max-inbound-payload-bytes:16777216}") int maxInboundPayloadBytes) {
        return server -> server.maxInboundPayloadSize(maxInboundPayloadBytes)
                .interceptors(registry -> registry.forSocketAcceptor(interceptor));
    }
}
