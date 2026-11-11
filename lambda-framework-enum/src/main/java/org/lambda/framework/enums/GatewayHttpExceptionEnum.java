package org.lambda.framework.enums;

public enum GatewayHttpExceptionEnum implements ExceptionEnumFunction {
    BAD_REQUEST(400, GatewayExceptionEnum.INVALID_REQUEST),
    NOT_FOUND(404, GatewayExceptionEnum.NOT_FOUND),
    METHOD_NOT_ALLOWED(405, GatewayExceptionEnum.NOT_ALLOWED),
    CONTENT_TOO_LARGE(413, GatewayExceptionEnum.CONTENT_TOO_LARGE),
    UNSUPPORTED_MEDIA_TYPE(415, GatewayExceptionEnum.INVALID_REQUEST),
    UPGRADE_REQUIRED(426, GatewayExceptionEnum.NOT_ALLOWED),
    BAD_GATEWAY(502, GatewayExceptionEnum.RESPONSE_MISSING),
    SERVICE_UNAVAILABLE(503, GatewayExceptionEnum.UPSTREAM_UNAVAILABLE),
    GATEWAY_TIMEOUT(504, GatewayExceptionEnum.TIMEOUT);

    private final int status;
    private final GatewayExceptionEnum error;

    GatewayHttpExceptionEnum(int status, GatewayExceptionEnum error) {
        this.status = status;
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return error.getCode();
    }

    @Override
    public String getMessage() {
        return error.getMessage();
    }
}
