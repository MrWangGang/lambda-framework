package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum NacosExceptionEnum implements ExceptionEnumFunction {
    REGISTRATION_FAILED("S1300001", "服务暂不可用，请稍后重试"),
    SUBSCRIPTION_FAILED("S1300002", "服务发现暂不可用，请稍后重试");

    private final String code;
    private final String message;

    NacosExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
