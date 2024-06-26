package org.lambda.framework.repository.config.converter;

import org.lambda.framework.common.enums.ConverterEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class EnumWriteConverter implements Converter<ConverterEnum,String> {
    @Override
    public String convert(ConverterEnum source) {
        return source.getValue();
    }
}
