package org.lambda.framework.gateway.autoconfigure;

import org.lambda.framework.gateway.adapter.GatewayGlobalExceptionHandler;
import org.lambda.framework.gateway.filter.GatewayFilter;
import org.lambda.framework.gateway.filter.support.RsocketRequestFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnBooleanProperty(name = "spring.cloud.gateway.server.webflux.enabled", matchIfMissing = true)
@Import({GatewayFilter.class, RsocketRequestFactory.class, GatewayGlobalExceptionHandler.class})
public class RSocketGatewayAutoConfiguration {}
