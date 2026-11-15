package org.lambda.framework.repository.config.converter;

import org.springframework.data.mongodb.core.convert.MongoConversionContext;
import org.springframework.data.mongodb.core.convert.MongoValueConverter;

/** Persists blank optional text as BSON null and preserves the existing Java string contract. */
public final class NullableMongoText implements MongoValueConverter<String, String> {
    @Override
    public String write(String value, MongoConversionContext context) {
        return value.isBlank() ? null : value;
    }

    @Override
    public String read(String value, MongoConversionContext context) {
        return value;
    }

    @Override
    public String readNull(MongoConversionContext context) {
        return "";
    }
}
