package org.lambda.framework.common.annotation.rsocket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Publishes a fixed HTTP-facing RSocket request-stream route. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewayStream {
    String DISCOVERY_ROUTE = "_lambda.gateway.stream-routes";
}
