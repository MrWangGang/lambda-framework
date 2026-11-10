package org.lambda.framework.rpc.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum RpcExceptionEnum implements ExceptionEnumFunction {
    ES_RPC_000("ES_RPC_000", "@RSocketRpc 只能标注接口类型的字段"),
    ES_RPC_003("ES_RPC_003", "Facade 方法未注册 RPC 路由"),
    ES_RPC_004("ES_RPC_004", "Facade 方法的服务名须一致"),
    ES_RPC_013("ES_RPC_013", "无效的 RSocket RPC 客户端"),
    ES_RPC_014("ES_RPC_014", "Facade 方法最多接收一个非空参数"),
    ES_RPC_015("ES_RPC_015", "返回类型必须是带具体泛型的 Mono、Flux、UiFlux 或 BiFlux"),
    ES_RPC_016("ES_RPC_016", "注入 @RSocketRpc Facade 失败");

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;

    RpcExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
