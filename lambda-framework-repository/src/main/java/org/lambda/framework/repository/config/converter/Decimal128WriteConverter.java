package org.lambda.framework.repository.config.converter;

import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class Decimal128WriteConverter implements Converter<Decimal128,Double> {
    @Override
    public Double convert(Decimal128 source) {
        return source.doubleValue();
    }
}
