package org.lambda.framework.repository.operation;

import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;

/** Applies the audit fields from UnifyPO to MongoDB updates that bypass entity callbacks. */
public final class MongoAuditUpdates {
    private MongoAuditUpdates() {}

    public static Update modified(Update update) {
        return update.currentDate("updatedAt").inc("version", 1L);
    }

    public static Update insertOnly(Update update, Instant createdAt, Instant updatedAt) {
        return update.setOnInsert("createdAt", createdAt)
                .setOnInsert("updatedAt", updatedAt)
                .setOnInsert("version", 0L);
    }
}
