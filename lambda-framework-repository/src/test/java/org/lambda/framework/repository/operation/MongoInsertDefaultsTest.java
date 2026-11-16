package org.lambda.framework.repository.operation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.lambda.framework.common.po.UnifyPO;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class MongoInsertDefaultsTest {
    static class Sample extends UnifyPO<ObjectId> {
        String owner;
        @Field("optional_alias") String optional;
        String retained;
        String explicitlySet;
        int count;
        @Transient String ignored;
    }

    @Test void onlyNewDocumentsGetNullDefaultsAndSuppliedValuesRemainIntact() {
        var query = Query.query(Criteria.where("owner").is("person"));
        var update = new Update().set("retained", "value").setOnInsert("explicitlySet", "supplied");
        var result = MongoAuditUpdates.insertOnly(update, Instant.EPOCH, Instant.EPOCH, query, Sample.class).getUpdateObject();
        var onInsert = result.get("$setOnInsert", Document.class);
        assertThat(onInsert).containsEntry("optional_alias", null).containsEntry("explicitlySet", "supplied")
                .doesNotContainKeys("_id", "id", "owner", "retained", "count", "ignored");
        assertThat(result.get("$set", Document.class)).containsEntry("retained", "value").doesNotContainKey("optional_alias");
    }

    @Test void mappedFieldUsedByQueryIsNotOverwritten() {
        var query = Query.query(Criteria.where("optional").is("existing"));
        var update = MongoInsertDefaults.withNullFields(query, new Update().set("owner", "person"), Sample.class);
        assertThat(update.getUpdateObject().get("$setOnInsert", Document.class)).doesNotContainKey("optional_alias");
    }
}
