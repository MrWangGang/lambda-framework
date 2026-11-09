package org.lambda.framework.common.enums;

public enum GatewayInteraction {
    REQUEST_RESPONSE,
    REQUEST_STREAM,
    REQUEST_CHANNEL;

    public static final String DISCOVERY_ROUTE = "_lambda.gateway.interactions";

    public static boolean isPublicPath(String path) {
        return path != null && path.startsWith("/")
                && path.indexOf('*') < 0 && path.indexOf('?') < 0 && path.indexOf('{') < 0;
    }
}
