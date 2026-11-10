package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum SecurityExceptionEnum implements ExceptionEnumFunction {
    AUTHENTICATION_REQUIRED("S0600000", "请先登录");

    private final String code;
    private final String message;

    SecurityExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
