package org.lambda.framework.rsocket.adapter;

import org.lambda.framework.rsocket.error.RsocketGlobalExceptionHandler;
import org.lambda.framework.common.support.GatewayRouteContract;
import io.rsocket.frame.FrameType;
import org.lambda.framework.common.annotation.rsocket.RSocketFacade;
import org.lambda.framework.common.enums.GatewayInteraction;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.common.support.UiFlux;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.rsocket.autoconfigure.RSocketMessageHandlerCustomizer;
import org.springframework.boot.rsocket.autoconfigure.RSocketMessagingAutoConfiguration;
import org.springframework.boot.rsocket.autoconfigure.RSocketServerAutoConfiguration;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.validation.Validator;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;

@AutoConfiguration(before = {RSocketMessagingAutoConfiguration.class, RSocketServerAutoConfiguration.class})
@Conditional(RsocketInterceptorConfig.ServerConfigured.class)
@Import({GatewayContractController.class, RsocketInterceptorConfig.RejectHttpListener.class,
        RSocketAcceptorInterceptor.class, RsocketGlobalExceptionHandler.class})
public class RsocketInterceptorConfig {
    @Bean
    @ConditionalOnMissingBean(RSocketMessageHandler.class)
    RSocketMessageHandler lambdaRSocketMessageHandler(RSocketStrategies strategies,
            ObjectProvider<RSocketMessageHandlerCustomizer> customizers, ObjectProvider<Validator> validators,
            @Value("${spring.application.name:}") String service) {
        var handler = new FacadeRSocketMessageHandler(service);
        handler.setRSocketStrategies(strategies);
        validators.ifUnique(handler::setValidator);
        customizers.orderedStream().forEach(customizer -> customizer.customize(handler));
        return handler;
    }

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
    RSocketServerCustomizer gatewayRouteVersionCustomizer(ObjectProvider<GatewayRouteContract> routes) {
        return server -> server.interceptors(registry -> registry.forSocketAcceptor(new GatewayRouteVersionInterceptor(routes)));
    }

    @Bean
    SmartInitializingSingleton validateMappings(ObjectProvider<RSocketMessageHandler> handlers) {
        return () -> handlers.ifAvailable(handler -> handler.getHandlerMethods().forEach((mapping, method) -> {
            var destinations = mapping.getCondition(DestinationPatternsMessageCondition.class);
            boolean nativeRoute = destinations != null && destinations.getPatterns().stream()
                    .anyMatch(path -> !path.startsWith("/") && !GatewayInteraction.DISCOVERY_ROUTE.equals(path));
            if (nativeRoute && !AnnotatedElementUtils.hasAnnotation(method.getBeanType(), RSocketFacade.class)) {
                throw new IllegalStateException("Java RSocket RPC routes require @RSocketFacade on the implementation: " + method);
            }
            var frames = mapping.getCondition(RSocketFrameTypeMessageCondition.class);
            boolean channel = frames != null && frames.getFrameTypes().contains(FrameType.REQUEST_CHANNEL);
            var adapters = handler.getRSocketStrategies().reactiveAdapterRegistry();
            var streamingParameters = Arrays.stream(method.getMethodParameters()).filter(parameter -> {
                var adapter = adapters.getAdapter(parameter.getParameterType());
                return adapter != null && adapter.isMultiValue();
            }).toList();
            var result = method.getReturnType();
            Class<?> resultType = result.getParameterType();
            if ((resultType == BiFlux.class || resultType == UiFlux.class || channel)
                    && (!(result.getGenericParameterType() instanceof ParameterizedType)
                        || (channel && (resultType != BiFlux.class || streamingParameters.size() != 1
                            || !(streamingParameters.getFirst().getGenericParameterType() instanceof ParameterizedType))))) {
                throw new IllegalStateException("A streaming request requires a typed publisher and BiFlux<O> return: " + method);
            }
        }));
    }

    static final class ServerConfigured extends AnyNestedCondition {
        ServerConfigured() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty("spring.rsocket.server.port")
        static class HasPort {}

        @ConditionalOnProperty("spring.rsocket.server.mapping-path")
        static class HasMappingPath {}
    }

    @ConditionalOnWebApplication
    static class RejectHttpListener {
        RejectHttpListener() {
            throw new IllegalStateException("Business RSocket services must use spring.main.web-application-type=none; expose HTTP through the gateway");
        }
    }
}
