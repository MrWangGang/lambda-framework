package org.lambda.framework.common.enums;

import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_029;

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
        throw new EventException(ES_COMMON_029);
    }
}
