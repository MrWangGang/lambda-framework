package org.lambda.framework.lock.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum LockExceptionEnum implements ExceptionEnumFunction {
    ES_LOCK_000("ES_LOCK_000","缺失lockPath"),

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
    ES_LOCK_REDISSON_009("ES_LOCK_REDISSON_009","无效的redis部署模式"),
    ES_LOCK_REDISSON_010("ES_LOCK_REDISSON_010","redisson RLock 未初始化"),
    ES_LOCK_REDISSON_011("ES_LOCK_REDISSON_011","redisson RReadWriteLock 未初始化"),
    ES_LOCK_REDISSON_012("ES_LOCK_REDISSON_012","redisson RLockReactive 未初始化"),
    ES_LOCK_REDISSON_013("ES_LOCK_REDISSON_013","redisson RReadWriteLockReactive 未初始化"),
    ES_LOCK_REDISSON_017("ES_LOCK_REDISSON_017","redisson 加锁失败"),
    ES_LOCK_REDISSON_018("ES_LOCK_REDISSON_018","redisson 释放锁失败"),
    ES_LOCK_REDISSON_019("ES_LOCK_REDISSON_019","无效的redis部署模式"),
    ES_LOCK_REDISSON_020("ES_LOCK_REDISSON_020","redisson 未配置lambda.lock.redisson.host"),
    ES_LOCK_REDISSON_021("ES_LOCK_REDISSON_021","redisson 未配置lambda.lock.redisson.password"),
    ES_LOCK_REDISSON_022("ES_LOCK_REDISSON_022","redisson 未配置lambda.lock.redisson.database"),




    ES_LOCK_ZOOKEEPER_010("ES_LOCK_ZOOKEEPER_010","zookeeper host缺失"),
    ES_LOCK_ZOOKEEPER_011("ES_LOCK_ZOOKEEPER_011","zookeeper sessionTimeoutMs缺失"),
    ES_LOCK_ZOOKEEPER_012("ES_LOCK_ZOOKEEPER_012","zookeeper connectionTimeoutMs缺失"),
    ES_LOCK_ZOOKEEPER_013("ES_LOCK_ZOOKEEPER_013","zookeeper baseSleepTimeMs缺失"),
    ES_LOCK_ZOOKEEPER_014("ES_LOCK_ZOOKEEPER_014","zookeeper maxRetries缺失"),
    ES_LOCK_ZOOKEEPER_015("ES_LOCK_ZOOKEEPER_015","zookeeper 连接失败"),
    ES_LOCK_ZOOKEEPER_017("ES_LOCK_ZOOKEEPER_017","zookeeper 加锁失败"),
    ES_LOCK_ZOOKEEPER_018("ES_LOCK_ZOOKEEPER_018","zookeeper 释放锁失败"),
    ES_LOCK_ZOOKEEPER_019("ES_LOCK_ZOOKEEPER_019","zookeeper InterProcessMutex未初始化"),
    ES_LOCK_ZOOKEEPER_020("ES_LOCK_ZOOKEEPER_020","zookeeper InterProcessReadWriteLock 未初始化"),
    ES_LOCK_ZOOKEEPER_021("ES_LOCK_ZOOKEEPER_021","zookeeper 未配置lambda.lock.zookeeper.host");










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
