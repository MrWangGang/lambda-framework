package org.lambda.framework.repository.enums;

import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.ES_REPOSITORY_REDIS_040;

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
        throw new EventException(ES_REPOSITORY_REDIS_040);
    }
}
