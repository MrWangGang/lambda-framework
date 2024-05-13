package org.lambda.framework.zookeeper.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum ZookeeperExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_ZOOKEEPER_000("ES_ZOOKEEPER_000","系统错误[未知]"),
    ES_ZOOKEEPER_001("ES_ZOOKEEPER_001","zookeeper主机host缺失"),
    ES_ZOOKEEPER_002("ES_ZOOKEEPER_002","zookeeper端口缺失"),
    ES_ZOOKEEPER_003("ES_ZOOKEEPER_003","zookeeper用户名缺失"),
    ES_ZOOKEEPER_004("ES_ZOOKEEPER_004","zookeeper密码缺失"),
    ES_ZOOKEEPER_005("ES_ZOOKEEPER_005","zookeeper最大等待时间缺失"),
    ES_ZOOKEEPER_006("ES_ZOOKEEPER_006","zookeeper两次重试之间的等待时间缺失"),
    ES_ZOOKEEPER_007("ES_ZOOKEEPER_007","zookeeper不能缺失客户端实例"),
    ES_ZOOKEEPER_008("ES_ZOOKEEPER_008","zookeeper不能缺失根目录");

    // 成员变量

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;
    // 构造方法
    private ZookeeperExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
}
