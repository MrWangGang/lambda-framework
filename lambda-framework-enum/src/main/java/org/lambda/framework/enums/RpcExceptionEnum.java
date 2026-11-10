package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum RpcExceptionEnum implements ExceptionEnumFunction {
    INVALID_FACADE("S0300000", "暂时无法调用服务"),
    METHOD_UNAVAILABLE("S0300003", "所需功能暂不可用"),
    CONNECTION_UNAVAILABLE("S0300013", "服务暂不可用"),
    INVALID_PAYLOAD("S0300014", "请求参数不正确"),
    PROXY_INJECTION_FAILED("S0300016", "服务暂不可用"),
    RESPONSE_MISSING("S0300017", "未获取到所需结果");

    private final String code;
    private final String message;

    RpcExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
