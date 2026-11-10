package org.lambda.framework.httpclient.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum HttpclientExceptionEnum implements ExceptionEnumFunction {
    ES_HTTPCLIENT_000("S1000000", "外部服务暂不可用");

    private final String code;
    private final String message;

    HttpclientExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
