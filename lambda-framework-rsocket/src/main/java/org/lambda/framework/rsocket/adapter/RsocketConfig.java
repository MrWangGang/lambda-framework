/*
package org.lambda.framework.rsocket.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketStrategies;

@Configuration
public class RsocketConfig {
    @Bean
    public RSocketServerCustomizer rSocketServerCustomizer(RSocketMetadataInterceptor rSocketMetadataInterceptor,RSocketResponseInterceptor rSocketResponseInterceptor) {
        return server -> server.interceptors(interceptorRegistry ->
                interceptorRegistry.forSocketAcceptor(
                        rSocketMetadataInterceptor::apply
                ).forResponder(
                        rSocketResponseInterceptor::apply
                )
        );
    }

    @Bean
    public RSocketMetadataInterceptor rSocketMetadataInterceptor(@Autowired(required = false) RSocketStrategies strategies) {
        return new RSocketMetadataInterceptor(strategies);
    }

    @Bean
    public RSocketResponseInterceptor rSocketResponseInterceptor() {
        return new RSocketResponseInterceptor();
    }

}
*/
