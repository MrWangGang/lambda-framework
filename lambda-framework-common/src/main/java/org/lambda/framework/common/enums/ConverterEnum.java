package org.lambda.framework.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public interface ConverterEnum {
    public String getDescription();
    @JsonValue
    public Integer getValue();
}
