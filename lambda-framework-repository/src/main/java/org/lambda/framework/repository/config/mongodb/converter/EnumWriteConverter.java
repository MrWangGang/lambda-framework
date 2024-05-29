package org.lambda.framework.repository.config.mongodb.converter;

import org.lambda.framework.common.enums.ConverterEnum;
import org.springframework.core.convert.converter.Converter;

public class EnumWriteConverter implements Converter<ConverterEnum,String> {
    @Override
    public String convert(ConverterEnum source) {
        return source.getValue();
    }
}
