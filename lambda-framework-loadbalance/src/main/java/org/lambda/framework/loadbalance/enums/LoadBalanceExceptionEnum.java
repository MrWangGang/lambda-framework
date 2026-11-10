package org.lambda.framework.loadbalance.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum LoadBalanceExceptionEnum implements ExceptionEnumFunction {
    EB_LOADBALANCE_001("S0400001", "所需服务不可用"),
    EB_LOADBALANCE_005("S0400005", "所需服务不可用"),
    EB_LOADBALANCE_006("S0400006", "所需服务不可用");

    private final String code;
    private final String message;

    LoadBalanceExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
