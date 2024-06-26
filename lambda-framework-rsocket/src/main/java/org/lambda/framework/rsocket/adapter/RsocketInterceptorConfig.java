package org.lambda.framework.rsocket.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsocketInterceptorConfig  {
    @Bean
    public RSocketServerCustomizer rSocketServerCustomizer(@Autowired(required = false) RSocketAcceptorInterceptor rSocketSocketAccpInterceptor
    ) {
        return server -> server.interceptors(interceptorRegistry ->
                interceptorRegistry.forSocketAcceptor(rSocketSocketAccpInterceptor));
    }
}
