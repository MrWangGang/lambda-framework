package org.lambda.framework.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public interface ConverterEnum<V> {
    public String getDescription();
    @JsonValue
    public V getValue();
}
