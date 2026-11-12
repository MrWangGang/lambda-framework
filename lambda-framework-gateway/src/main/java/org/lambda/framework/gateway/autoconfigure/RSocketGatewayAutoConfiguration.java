package org.lambda.framework.gateway.autoconfigure;

import org.lambda.framework.gateway.error.GatewayGlobalExceptionHandler;
import org.lambda.framework.gateway.filter.GatewayFilter;
import org.lambda.framework.gateway.forward.RsocketRequestFactory;
import org.lambda.framework.gateway.forward.RsocketRouteWarmup;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnBooleanProperty(name = "spring.cloud.gateway.server.webflux.enabled", matchIfMissing = true)
@Import({GatewayFilter.class, RsocketRequestFactory.class, RsocketRouteWarmup.class, GatewayGlobalExceptionHandler.class})
public class RSocketGatewayAutoConfiguration {}
