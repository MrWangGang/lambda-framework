package org.lambda.framework.repository.operation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.lambda.framework.common.exception.EventException;

class MongoFieldChangesTest {
    @Test
    void setsOnlySubmittedFieldsAndExplicitlyClearedFields() {
        var update = MongoFieldChanges.toUpdate(Map.of("name", "新名字"), List.of("description"),
                Set.of("name", "description", "age"), Set.of("description"));
        var changes = (Document) update.getUpdateObject().get("$set");
        assertEquals("新名字", changes.get("name"));
        assertTrue(changes.containsKey("description"));
        assertNull(changes.get("description"));
        assertFalse(changes.containsKey("age"));
    }

    @Test
    void rejectsUnknownAndNonNullableFields() {
        assertThrows(EventException.class, () -> MongoFieldChanges.toUpdate(Map.of("role", "admin"), List.of(),
                Set.of("name"), Set.of()));
        assertThrows(EventException.class, () -> MongoFieldChanges.toUpdate(Map.of(), List.of("name"),
                Set.of("name"), Set.of()));
    }

    @Test
    void doesNotTurnFalsyValuesIntoNull() {
        var changes = (Document) MongoFieldChanges.toUpdate(Map.of("count", 0, "enabled", false), List.of(),
                Set.of("count", "enabled"), Set.of()).getUpdateObject().get("$set");
        assertEquals(0, changes.get("count"));
        assertEquals(false, changes.get("enabled"));
    }

    @Test
    void rejectsBlankOrNullValuesAndDuplicateClearOperations() {
        var allowed = Set.of("name", "description");
        assertThrows(EventException.class, () -> MongoFieldChanges.toUpdate(Map.of("name", "  "), List.of(), allowed, Set.of("description")));
        var values = new java.util.HashMap<String, Object>();
        values.put("description", null);
        assertThrows(EventException.class, () -> MongoFieldChanges.toUpdate(values, List.of(), allowed, Set.of("description")));
        assertThrows(EventException.class, () -> MongoFieldChanges.toUpdate(Map.of(), List.of("description", "description"), allowed, Set.of("description")));
        assertDoesNotThrow(() -> MongoFieldChanges.toUpdate(Map.of("name", List.of()), List.of(), allowed, Set.of("description")));
    }

    @Test
    void insertOnlyFieldsDoNotOverwriteAnExistingDocument() {
        var update = MongoFieldChanges.toInsertOnlyUpdate(Map.of("kind", "notice"), List.of("detail"),
                Set.of("kind", "detail"), Set.of("detail")).getUpdateObject();
        assertFalse(update.containsKey("$set"));
        var fields = update.get("$setOnInsert", Document.class);
        assertEquals("notice", fields.get("kind"));
        assertTrue(fields.containsKey("detail")); assertNull(fields.get("detail"));
    }
}
