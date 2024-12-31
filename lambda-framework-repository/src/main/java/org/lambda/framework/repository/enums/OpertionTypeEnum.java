package org.lambda.framework.repository.enums;

import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.ES_REPOSITORY_107;

public enum OpertionTypeEnum {
    INSERT,
    UPDATE,
    DELETE;

    public static boolean isValid(String value) {
        for (OpertionTypeEnum enumValue : values()) {
            if (enumValue.name().equals(value)) {
                return true;
            }
        }
        throw new EventException(ES_REPOSITORY_107);
    }
}
