package org.lambda.framework.lock.enums;

import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_REDISSON_019;

public enum RedissonDeployModelEnum {
    single,
    master_slave,
    cluster,
    sentinel;

    public static boolean isValid(String value) {
        for (RedissonDeployModelEnum enumValue : values()) {
            if (enumValue.name().equals(value)) {
                return true;
            }
        }
        throw new EventException(ES_LOCK_REDISSON_019);
    }
}
