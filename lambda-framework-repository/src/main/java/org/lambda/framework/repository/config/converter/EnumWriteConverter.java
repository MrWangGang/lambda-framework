package org.lambda.framework.repository.config.converter;

import org.lambda.framework.common.enums.ConverterEnum;
import org.springframework.core.convert.converter.Converter;

public class EnumWriteConverter implements Converter<ConverterEnum,Integer> {
    @Override
    public Integer convert(ConverterEnum source) {
        return source.getValue();
    }
}
