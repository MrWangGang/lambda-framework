package org.lambda.framework.security.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum SecurityExceptionEnum implements ExceptionEnumFunction {
    ES_SECURITY_000("S0600000", "请先登录");

    private final String code;
    private final String message;

    SecurityExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
