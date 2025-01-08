package org.lambda.framework.repository.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.math.BigDecimal;

@WritingConverter
public class BigDecimalWriteConverter implements Converter<BigDecimal,Double> {
    @Override
    public Double convert(BigDecimal source) {
        return source.doubleValue();
    }
}
