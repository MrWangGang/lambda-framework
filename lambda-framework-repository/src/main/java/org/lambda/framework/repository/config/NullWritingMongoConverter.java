package org.lambda.framework.repository.config;

import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

/** Normalizes blank scalar text to BSON null when a mapped entity is saved. */
public final class NullWritingMongoConverter extends MappingMongoConverter {
    public NullWritingMongoConverter(DbRefResolver resolver,
            MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> context) {
        super(resolver, context);
    }

    @Override
    public void write(Object source, Bson target) {
        super.write(source, target);
        if (target instanceof Document document) normalizeDocument(document);
    }

    private static void normalizeDocument(Document document) {
        document.replaceAll((key, value) -> normalizeValue(value));
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof String text && text.isBlank()) return null;
        if (value instanceof Document document) normalizeDocument(document);
        if (value instanceof List<?> list) {
            // Keep list length and positions unchanged; service validation rejects invalid elements.
            return list.stream().map(NullWritingMongoConverter::normalizeValue).toList();
        }
        return value;
    }
}
