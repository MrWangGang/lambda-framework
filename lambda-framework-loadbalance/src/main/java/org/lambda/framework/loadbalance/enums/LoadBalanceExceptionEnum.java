package org.lambda.framework.loadbalance.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum LoadBalanceExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_LOADBALANCE_000("ES_LOADBALANCE_000","Rsocket 无法注册到nacos,请检查port配置。"),
    EB_LOADBALANCE_001("EB_LOADBALANCE_001","调用服务名不能为空"),
    EB_LOADBALANCE_002("EB_LOADBALANCE_002","该服务名下没有活跃的服务"),
    EB_LOADBALANCE_003("EB_LOADBALANCE_003","无效路由"),
    EB_LOADBALANCE_004("EB_LOADBALANCE_004","无效端口"),
    EB_LOADBALANCE_005("EB_LOADBALANCE_005","调用服务的地址不能为空"),
    EB_LOADBALANCE_006("EB_LOADBALANCE_006","调用服务的端口不能为空"),
    EB_LOADBALANCE_007("EB_LOADBALANCE_007","调用服务的路径不能为空"),
    EB_LOADBALANCE_008("EB_LOADBALANCE_008","服务间的调用必须用二进制(application/octet-stream)传输"),
    EB_LOADBALANCE_009("EB_LOADBALANCE_009","二进制数据转json失败");




    // 成员变量

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
