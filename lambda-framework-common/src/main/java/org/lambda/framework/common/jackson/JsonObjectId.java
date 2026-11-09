package org.lambda.framework.common.jackson;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Keeps an ObjectId property a hexadecimal JSON string in Jackson 2 and 3. */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = JsonObjectId.Jackson2Writer.class)
@JsonDeserialize(using = JsonObjectId.Jackson2Reader.class)
@tools.jackson.databind.annotation.JsonSerialize(using = JsonObjectId.Jackson3Writer.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = JsonObjectId.Jackson3Reader.class)
public @interface JsonObjectId {
    String FORMAT_ERROR = "Expected a 24-character hexadecimal MongoDB ObjectId";

    final class Jackson2Reader extends JsonDeserializer<ObjectId> {
        @Override
        public ObjectId deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.hasToken(JsonToken.VALUE_STRING) ? parser.getText() : null;
            if (value == null || !ObjectId.isValid(value)) {
                return context.reportInputMismatch(ObjectId.class, FORMAT_ERROR);
            }
            return new ObjectId(value);
        }
    }

    final class Jackson2Writer extends JsonSerializer<ObjectId> {
        @Override
        public void serialize(ObjectId value, JsonGenerator generator, SerializerProvider context) throws IOException {
            generator.writeString(value.toHexString());
        }
    }

    final class Jackson3Reader extends tools.jackson.databind.ValueDeserializer<ObjectId> {
        @Override
        public ObjectId deserialize(tools.jackson.core.JsonParser parser,
                                    tools.jackson.databind.DeserializationContext context) {
            String value = parser.hasToken(tools.jackson.core.JsonToken.VALUE_STRING) ? parser.getString() : null;
            if (value == null || !ObjectId.isValid(value)) {
                return context.reportInputMismatch(ObjectId.class, FORMAT_ERROR);
            }
            return new ObjectId(value);
        }
    }

    final class Jackson3Writer extends tools.jackson.databind.ValueSerializer<ObjectId> {
        @Override
        public void serialize(ObjectId value, tools.jackson.core.JsonGenerator generator,
                              tools.jackson.databind.SerializationContext context) {
            generator.writeString(value.toHexString());
        }
    }
}
