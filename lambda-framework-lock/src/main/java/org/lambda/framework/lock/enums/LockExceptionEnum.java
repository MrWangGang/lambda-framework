package org.lambda.framework.lock.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum LockExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_LOCK_REDISSON_000("ES_LOCK_REDISSON_000","redisson host缺失"),
    ES_LOCK_REDISSON_001("ES_LOCK_REDISSON_001","redisson port缺失"),
    ES_LOCK_REDISSON_002("ES_LOCK_REDISSON_002","redisson maxActive缺失"),
    ES_LOCK_REDISSON_003("ES_LOCK_REDISSON_003","redisson maxWaitSeconds缺失"),
    ES_LOCK_REDISSON_004("ES_LOCK_REDISSON_004","redisson maxIdle缺失"),
    ES_LOCK_REDISSON_005("ES_LOCK_REDISSON_005","redisson minIdle缺失"),
    ES_LOCK_REDISSON_006("ES_LOCK_REDISSON_006","redisson database缺失"),
    ES_LOCK_REDISSON_007("ES_LOCK_REDISSON_007","redisson masterName缺失"),
    ES_LOCK_REDISSON_008("ES_LOCK_REDISSON_008","redis 部署模型不能缺失"),
    ES_LOCK_REDISSON_009("ES_LOCK_REDISSON_009","无效的redis部署模式");




    // 成员变量

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;
    // 构造方法
    private LockExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
}
