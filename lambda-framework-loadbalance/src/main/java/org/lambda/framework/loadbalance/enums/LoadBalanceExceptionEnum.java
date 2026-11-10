package org.lambda.framework.loadbalance.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum LoadBalanceExceptionEnum implements ExceptionEnumFunction {

    EB_LOADBALANCE_001("EB_LOADBALANCE_001", "调用服务名不能为空"),
    EB_LOADBALANCE_005("EB_LOADBALANCE_005", "调用服务的地址不能为空"),
    EB_LOADBALANCE_006("EB_LOADBALANCE_006", "调用服务的端口不能为空");

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;
    // 构造方法
    private LoadBalanceExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
}
