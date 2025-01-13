package org.lambda.framework.repository.config.converter;

import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.data.convert.ReadingConverter;

import java.math.BigDecimal;

@ReadingConverter
public class Decimal128ReadConverter implements ConverterFactory<Double, Decimal128>  {




    @Override
    public <T extends Decimal128> Converter<Double, T> getConverter(Class<T> targetType) {
        return new Decimal128ToDoubleConverterImpl<>();
    }

    private static class Decimal128ToDoubleConverterImpl<T extends Decimal128> implements Converter<Double, T> {
        @Override
        public T convert(Double source) {
            BigDecimal bigDecimalValue = new BigDecimal(source.toString());
            return (T) new Decimal128(bigDecimalValue);
        }
    }
}
