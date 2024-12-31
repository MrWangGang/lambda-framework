package org.lambda.framework.repository.enums;

import lombok.Getter;
import org.lambda.framework.common.enums.ConverterEnum;
import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.ES_REPOSITORY_107;
@Getter
public enum OpertionTypeEnum  implements ConverterEnum {
    INSERT("插入","INSERT"),
    UPDATE("更新","UPDATE"),
    DELETE("删除","DELETE");

    public static boolean isValid(String value) {
        for (OpertionTypeEnum enumValue : values()) {
            if (enumValue.name().equals(value)) {
                return true;
            }
        }
        throw new EventException(ES_REPOSITORY_107);
    }

    OpertionTypeEnum(String description, String value) {
        this.description = description;
        this.value = value;
    }

    private final String value;
    private final String description;
}
