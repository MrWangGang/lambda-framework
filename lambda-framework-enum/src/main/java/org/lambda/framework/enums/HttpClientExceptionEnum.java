package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum HttpClientExceptionEnum implements ExceptionEnumFunction {
    UPSTREAM_RESPONSE_FAILED("S1000000", "外部服务暂不可用"),
    REQUEST_SIGNING_FAILED("S1000001", "外部服务请求签名失败");

    private final String code;
    private final String message;

    HttpClientExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
