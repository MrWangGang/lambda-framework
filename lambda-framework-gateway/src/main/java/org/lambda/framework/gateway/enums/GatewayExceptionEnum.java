package org.lambda.framework.gateway.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum GatewayExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_GATEWAY_000("ES_GATEWAY_000","系统错误[未知]");







    // 成员变量

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;
    // 构造方法
    private GatewayExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
}
