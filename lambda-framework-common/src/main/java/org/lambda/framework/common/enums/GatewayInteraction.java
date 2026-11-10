package org.lambda.framework.common.enums;

public enum GatewayInteraction {
    REQUEST_RESPONSE,
    REQUEST_VOID,
    REQUEST_ARRAY,
    REQUEST_STREAM,
    REQUEST_CHANNEL;

    public static final String DISCOVERY_ROUTE = "_lambda.gateway.interactions";

    public boolean isUnary() {
        return this == REQUEST_RESPONSE || this == REQUEST_VOID;
    }

    public boolean isNdjson() {
        return this == REQUEST_STREAM || this == REQUEST_CHANNEL;
    }

    public static boolean isPublicPath(String path) {
        return path != null && path.startsWith("/")
                && path.indexOf('*') < 0 && path.indexOf('?') < 0 && path.indexOf('{') < 0;
    }
}
