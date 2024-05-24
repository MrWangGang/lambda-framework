package org.lambda.framework.redis.enums;

import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.redis.enums.RedisExceptionEnum.ES_REDIS_029;

public enum RedisDeployModelEnum {
    single,
    master_slave,
    cluster,
    sentinel;

    public static boolean isValid(String value) {
        for (RedisDeployModelEnum enumValue : values()) {
            if (enumValue.name().equals(value)) {
                return true;
            }
        }
        throw new EventException(ES_REDIS_029);
    }
}
