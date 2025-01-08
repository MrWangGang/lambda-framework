package org.lambda.framework.repository.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.data.convert.ReadingConverter;

import java.math.BigDecimal;

@ReadingConverter
public class BigDecimalReadConverter implements ConverterFactory<Double, BigDecimal>  {




    @Override
    public <T extends BigDecimal> Converter<Double, T> getConverter(Class<T> targetType) {
        return new BigDecimalToDoubleConverterImpl<>();
    }

    private static class BigDecimalToDoubleConverterImpl<T extends BigDecimal> implements Converter<Double, T> {
        @Override
        public T convert(Double source) {
            return (T) new BigDecimal(source);
        }
    }
}
