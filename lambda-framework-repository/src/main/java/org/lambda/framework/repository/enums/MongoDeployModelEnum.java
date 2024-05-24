package org.lambda.framework.repository.enums;

import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.ES_REPOSITORY_MONGO_014;

public enum MongoDeployModelEnum {
    single,
    cluster;

    public static boolean isValid(String value) {
        for (MongoDeployModelEnum enumValue : values()) {
            if (enumValue.name().equals(value)) {
                return true;
            }
        }
        throw new EventException(ES_REPOSITORY_MONGO_014);
    }
}
