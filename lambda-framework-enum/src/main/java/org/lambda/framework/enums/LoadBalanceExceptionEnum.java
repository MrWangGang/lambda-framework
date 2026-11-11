package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum LoadBalanceExceptionEnum implements ExceptionEnumFunction {
    SERVICE_NAME_REQUIRED("S0400001", "所需服务不可用"),
    HOST_REQUIRED("S0400005", "所需服务不可用"),
    PORT_REQUIRED("S0400006", "所需服务不可用"),
    CONFIGURATION_INVALID("S0400007", "RSocket 客户端配置不正确"),
    CONNECTION_UNAVAILABLE("S0400008", "服务暂不可用，请稍后重试");

    private final String code;
    private final String message;

    LoadBalanceExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
