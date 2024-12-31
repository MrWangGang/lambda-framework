package org.lambda.framework.repository.enums;

import lombok.Getter;
import org.lambda.framework.common.enums.ConverterEnum;
import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.ES_REPOSITORY_107;
@Getter
public enum EnumOpertionType implements ConverterEnum {
    ENUM_OPERTION_TYPE_INSERT("插入","INSERT"),
    ENUM_OPERTION_TYPE_UPDATE("更新","UPDATE"),
    ENUM_OPERTION_TYPE_DELETE("删除","DELETE");

    public static boolean isValid(String value) {
        for (EnumOpertionType enumValue : values()) {
            if (enumValue.getValue().equals(value)) {
                return true;
            }
        }
        throw new EventException(ES_REPOSITORY_107);
    }

    EnumOpertionType(String description, String value) {
        this.description = description;
        this.value = value;
    }

    private final String value;
    private final String description;
}
