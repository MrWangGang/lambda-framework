package org.lambda.framework.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.types.ObjectId;

import java.io.IOException;

public final class ObjectIdJsonModules {
    private static final String FORMAT_ERROR = "Expected a 24-character hexadecimal MongoDB ObjectId";
    private static final String MODULE_NAME = "Lambda MongoDB ObjectId";

    private ObjectIdJsonModules() {}

    public static SimpleModule jackson2() {
        return new SimpleModule(MODULE_NAME)
                .addSerializer(ObjectId.class, new Jackson2Writer())
                .addDeserializer(ObjectId.class, new Jackson2Reader());
    }

    public static tools.jackson.databind.module.SimpleModule jackson3() {
        return new tools.jackson.databind.module.SimpleModule(MODULE_NAME)
                .addSerializer(ObjectId.class, new Jackson3Writer())
                .addDeserializer(ObjectId.class, new Jackson3Reader());
    }

    private static ObjectId parse(String value) {
        return value != null && ObjectId.isValid(value) ? new ObjectId(value) : null;
    }

    private static final class Jackson2Reader extends JsonDeserializer<ObjectId> {
        @Override
        public ObjectId deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.hasToken(JsonToken.VALUE_STRING) ? parser.getText() : null;
            ObjectId id = parse(value);
            return id == null ? context.reportInputMismatch(ObjectId.class, FORMAT_ERROR) : id;
        }
    }

    private static final class Jackson2Writer extends JsonSerializer<ObjectId> {
        @Override
        public void serialize(ObjectId value, JsonGenerator generator, SerializerProvider context) throws IOException {
            generator.writeString(value.toHexString());
        }
    }

    private static final class Jackson3Reader extends tools.jackson.databind.ValueDeserializer<ObjectId> {
        @Override
        public ObjectId deserialize(tools.jackson.core.JsonParser parser,
                                    tools.jackson.databind.DeserializationContext context) {
            String value = parser.hasToken(tools.jackson.core.JsonToken.VALUE_STRING) ? parser.getString() : null;
            ObjectId id = parse(value);
            return id == null ? context.reportInputMismatch(ObjectId.class, FORMAT_ERROR) : id;
        }
    }

    private static final class Jackson3Writer extends tools.jackson.databind.ValueSerializer<ObjectId> {
        @Override
        public void serialize(ObjectId value, tools.jackson.core.JsonGenerator generator,
                              tools.jackson.databind.SerializationContext context) {
            generator.writeString(value.toHexString());
        }
    }
}
