package org.lambda.framework.rsocket.adapter;

import io.rsocket.frame.FrameType;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.common.support.TFlux;
import org.lambda.framework.common.support.UiFlux;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

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

    @Bean
    SmartInitializingSingleton validateChannelMappings(ObjectProvider<RSocketMessageHandler> handlers) {
        return () -> handlers.ifAvailable(handler -> handler.getHandlerMethods().forEach((mapping, method) -> {
            var frames = mapping.getCondition(RSocketFrameTypeMessageCondition.class);
            boolean channel = frames != null && frames.getFrameTypes().contains(FrameType.REQUEST_CHANNEL);
            var adapters = handler.getRSocketStrategies().reactiveAdapterRegistry();
            var streamingParameters = Arrays.stream(method.getMethodParameters()).filter(parameter -> {
                var adapter = adapters.getAdapter(parameter.getParameterType());
                return adapter != null && adapter.isMultiValue();
            }).toList();
            var result = method.getReturnType();
            Class<?> resultType = result.getParameterType();
            if (resultType == TFlux.class) {
                if (!channel || streamingParameters.size() != 1
                        || streamingParameters.getFirst().getParameterType() != TFlux.class
                        || !(streamingParameters.getFirst().getGenericParameterType() instanceof ParameterizedType)
                        || !(result.getGenericParameterType() instanceof ParameterizedType)) {
                    throw new IllegalStateException("Legacy TFlux requires TFlux<I> input and TFlux<O> return: " + method);
                }
            } else if (resultType == BiFlux.class || resultType == UiFlux.class || channel) {
                if (!(result.getGenericParameterType() instanceof ParameterizedType)
                        || (channel && (resultType != BiFlux.class || streamingParameters.size() != 1
                            || !(streamingParameters.getFirst().getGenericParameterType() instanceof ParameterizedType)))) {
                    throw new IllegalStateException("A streaming request requires a typed publisher and BiFlux<O> return: " + method);
                }
            }
        }));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication
    static class RejectHttpListener {
        RejectHttpListener() {
            throw new IllegalStateException("Business RSocket services must use spring.main.web-application-type=none; expose HTTP through the gateway");
        }
    }
}
