package org.lambda.framework.repository.operation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.RSocketExceptionEnum;
import org.springframework.data.mongodb.core.query.Update;

/** Converts validated, presence-aware field changes into a MongoDB partial update. */
public final class MongoFieldChanges {
    private MongoFieldChanges() {}

    public static Update toUpdate(Map<String, ?> values, Collection<String> clearFields,
                                  Set<String> allowedFields, Set<String> nullableFields) {
        return build(values, clearFields, allowedFields, nullableFields, false);
    }

    public static Update toInsertOnlyUpdate(Map<String, ?> values, Collection<String> clearFields,
                                            Set<String> allowedFields, Set<String> nullableFields) {
        return build(values, clearFields, allowedFields, nullableFields, true);
    }

    private static Update build(Map<String, ?> values, Collection<String> clearFields,
                                Set<String> allowedFields, Set<String> nullableFields, boolean insertOnly) {
        if (values == null || clearFields == null || allowedFields == null || nullableFields == null
                || !allowedFields.containsAll(nullableFields)) throw invalid();
        if (clearFields.stream().anyMatch(field -> field == null)) throw invalid();
        var clears = Set.copyOf(clearFields);
        if (clears.size() != clearFields.size() || !nullableFields.containsAll(clears)) throw invalid();
        var update = new Update();
        values.forEach((field, value) -> {
            if (field == null || !allowedFields.contains(field) || clears.contains(field) || emptyValue(value)) throw invalid();
            if (insertOnly) update.setOnInsert(field, value); else update.set(field, value);
        });
        clears.forEach(field -> { if (insertOnly) update.setOnInsert(field, null); else update.set(field, null); });
        return update;
    }

    private static boolean emptyValue(Object value) {
        if (value == null || value instanceof String text && text.isBlank()) return true;
        if (value instanceof Collection<?> items) return items.stream().anyMatch(MongoFieldChanges::emptyValue);
        if (value instanceof Map<?, ?> values) return values.values().stream().anyMatch(MongoFieldChanges::emptyValue);
        return false;
    }

    private static EventException invalid() {
        return new EventException(RSocketExceptionEnum.INVALID_ARGUMENTS);
    }
}
