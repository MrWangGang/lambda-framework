package org.lambda.framework.repository.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.Field;

class NullWritingMongoMappingContextTest {
    static class Nested { String detail; }
    static class Sample {
        @Id ObjectId id;
        @Field("display_name") String name;
        Nested nested = new Nested();
        Integer count = 0;
        Boolean enabled = false;
        String blank = " ";
        String text = " content ";
        List<String> items = List.of("", "value");
    }

    @Test
    void writesNullFieldsWithoutChangingGeneratedIdsOrFalsyValues() throws Exception {
        var context = new NullWritingMongoMappingContext();
        context.afterPropertiesSet();
        var converter = new NullWritingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.afterPropertiesSet();
        var output = new Document();
        converter.write(new Sample(), output);

        assertFalse(output.containsKey("_id"));
        assertTrue(output.containsKey("display_name"));
        assertNull(output.get("display_name"));
        var nested = output.get("nested", Document.class);
        assertTrue(nested.containsKey("detail"));
        assertNull(nested.get("detail"));
        assertEquals(0, output.get("count"));
        assertEquals(false, output.get("enabled"));
        assertNull(output.get("blank"));
        assertEquals(" content ", output.get("text"));
        var items = output.getList("items", String.class);
        assertEquals(2, items.size());
        assertNull(items.get(0));
        assertEquals("value", items.get(1));
    }
}
