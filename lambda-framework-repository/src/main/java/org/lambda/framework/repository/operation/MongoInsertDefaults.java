package org.lambda.framework.repository.operation;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.RepositoryExceptionEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/** Supplies absent reference fields only when an atomic upsert creates a document. */
public final class MongoInsertDefaults {
    private static final ClassValue<List<DefaultField>> FIELDS = new ClassValue<>() {
        @Override
        protected List<DefaultField> computeValue(Class<?> entity) {
            var fields = new ArrayList<DefaultField>();
            for (var type = entity; type != null && type != Object.class; type = type.getSuperclass()) {
                for (var property : type.getDeclaredFields()) {
                    if (property.isSynthetic() || property.getType().isPrimitive()
                            || Modifier.isStatic(property.getModifiers())
                            || Modifier.isTransient(property.getModifiers())
                            || property.isAnnotationPresent(Id.class)
                            || property.isAnnotationPresent(Transient.class)) continue;
                    var mapping = property.getAnnotation(Field.class);
                    var name = mapping != null && !mapping.name().isBlank() ? mapping.name()
                            : mapping != null && !mapping.value().isBlank() ? mapping.value() : property.getName();
                    fields.add(new DefaultField(property.getName(), name));
                }
            }
            return List.copyOf(fields);
        }
    };

    private MongoInsertDefaults() {}

    public static Update withNullFields(Query query, Update update, Class<?> entity) {
        if (query == null || update == null || entity == null)
            throw new EventException(RepositoryExceptionEnum.CONFIGURATION_INVALID, "MongoDB upsert arguments required");
        var used = new HashSet<String>();
        collect(query.getQueryObject(), used);
        collect(update.getUpdateObject(), used);
        for (var field : FIELDS.get(entity)) {
            if (!used.contains(field.propertyName()) && used.add(field.documentFieldName())) {
                update.setOnInsert(field.documentFieldName(), null);
            }
        }
        return update;
    }

    private record DefaultField(String propertyName, String documentFieldName) {}

    private static void collect(Object value, Set<String> used) {
        if (value instanceof Map<?, ?> fields) {
            for (var entry : fields.entrySet()) {
                if (entry.getKey() instanceof String key && !key.startsWith("$")) used.add(key.split("\\.", 2)[0]);
                if (entry.getKey() instanceof String key && key.startsWith("$")) collect(entry.getValue(), used);
            }
        } else if (value instanceof Iterable<?> values) {
            for (var entry : values) collect(entry, used);
        }
    }
}
