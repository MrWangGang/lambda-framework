package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum LockExceptionEnum implements ExceptionEnumFunction {
    LOCK_NAME_REQUIRED("S0900000", "操作繁忙，请稍后重试"),
    CONFIGURATION_INVALID("S0900001", "锁组件配置不正确"),
    REDIS_MUTEX_UNAVAILABLE("S0910010", "操作繁忙，请稍后重试"),
    REDIS_READ_WRITE_LOCK_UNAVAILABLE("S0910011", "操作繁忙，请稍后重试"),
    REDIS_LOCK_ACQUIRE_FAILED("S0910017", "操作繁忙，请稍后重试"),
    REDIS_LOCK_RELEASE_FAILED("S0910018", "操作繁忙，请稍后重试"),
    ZOOKEEPER_LOCK_ACQUIRE_FAILED("S0920017", "操作繁忙，请稍后重试"),
    ZOOKEEPER_LOCK_RELEASE_FAILED("S0920018", "操作繁忙，请稍后重试"),
    ZOOKEEPER_MUTEX_UNAVAILABLE("S0920019", "操作繁忙，请稍后重试"),
    ZOOKEEPER_READ_WRITE_LOCK_UNAVAILABLE("S0920020", "操作繁忙，请稍后重试");

    private final String code;
    private final String message;

    LockExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
