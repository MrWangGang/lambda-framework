package org.lambda.framework.repository.config.converter;

import org.lambda.framework.common.enums.ConverterEnum;
import org.springframework.core.convert.converter.Converter;

public class EnumWriteConverter<V> implements Converter<ConverterEnum<V>,V> {
    @Override
    public V convert(ConverterEnum<V> source) {
        return source.getValue();
    }
}
