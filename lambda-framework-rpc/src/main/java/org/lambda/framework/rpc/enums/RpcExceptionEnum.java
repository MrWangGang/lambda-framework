package org.lambda.framework.rpc.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum RpcExceptionEnum implements ExceptionEnumFunction {
    ES_RPC_000("S0300000", "暂时无法调用服务"),
    ES_RPC_003("S0300003", "所需功能暂不可用"),
    ES_RPC_004("S0300004", "暂时无法调用服务"),
    ES_RPC_013("S0300013", "服务暂不可用"),
    ES_RPC_014("S0300014", "请求参数不正确"),
    ES_RPC_015("S0300015", "暂时无法获取结果"),
    ES_RPC_016("S0300016", "服务暂不可用"),
    ES_RPC_017("S0300017", "未获取到所需结果");

    private final String code;
    private final String message;

    RpcExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
