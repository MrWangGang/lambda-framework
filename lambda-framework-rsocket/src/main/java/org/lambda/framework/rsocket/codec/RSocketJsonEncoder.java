package org.lambda.framework.rsocket.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.util.Map;

@SuppressWarnings("removal")
class RSocketJsonEncoder extends Jackson2JsonEncoder {
    RSocketJsonEncoder(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    public DataBuffer encodeValue(Object value, DataBufferFactory factory, ResolvableType type,
                                  MimeType mimeType, Map<String, Object> hints) {
        if (!(factory instanceof NettyDataBufferFactory) || mimeType == null
                || !MimeTypeUtils.APPLICATION_JSON.equalsTypeAndSubtype(mimeType)
                || value instanceof MappingJacksonValue || logger.isDebugEnabled()) {
            return super.encodeValue(value, factory, type, mimeType, hints);
        }
        var mapper = selectObjectMapper(type, mimeType);
        if (mapper == null) {
            throw new IllegalStateException("No ObjectMapper for " + type);
        }
        Class<?> view = hints == null ? null : (Class<?>) hints.get(JSON_VIEW_HINT);
        var writer = view == null ? mapper.writer() : mapper.writerWithView(view);
        var javaType = getJavaType(type.getType(), null);
        if (javaType.isContainerType()) {
            writer = writer.forType(javaType);
        }
        writer = customizeWriter(writer, mimeType, type, hints);
        DataBuffer output = factory.allocateBuffer();
        boolean success = false;
        try {
            try (var generator = mapper.getFactory().createGenerator(output.asOutputStream(), getJsonEncoding(mimeType))) {
                writer.writeValue(generator, value);
            }
            Hints.touchDataBuffer(output, hints, logger);
            success = true;
            return output;
        } catch (InvalidDefinitionException error) {
            throw new CodecException("Type definition error: " + error.getType(), error);
        } catch (JsonProcessingException error) {
            throw new EncodingException("JSON encoding error: " + error.getOriginalMessage(), error);
        } catch (IOException error) {
            throw new IllegalStateException("Unexpected I/O error while writing to the JSON buffer", error);
        } finally {
            if (!success) {
                DataBufferUtils.release(output);
            }
        }
    }
}
