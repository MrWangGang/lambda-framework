package org.lambda.framework.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.jackson.ObjectIdJsonModules;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.lambda.framework.common.exception.RequiredValueException;

import java.util.Optional;
import java.util.function.Supplier;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_018;
import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_019;


/** Shared Jackson configuration for gateway, RSocket, Redis and MQ payloads. */
public final class JsonUtil {
    private static final ObjectMapper MAPPER = createObjectMapper();

    private JsonUtil() {}

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(ObjectIdJsonModules.jackson2())
                .build();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // LocalDateTime serializers
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // LocalDate serializers
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE));
        //LocalTime serializers
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm")));

        objectMapper.registerModule(javaTimeModule);
        return objectMapper;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new EventException(ES_COMMON_019);
        }
    }

    public static <T> Optional<T> fromMap(Map<?, ?> data, Class<T> type) {
        return Optional.ofNullable(data).map(value -> MAPPER.convertValue(value, type));
    }

    public static <T> Optional<T> read(String json, Class<T> type) {
        return Optional.ofNullable(json).map(value -> decode(value, type));
    }

    public static <T> T readRequired(String json, Class<T> type) {
        return readRequired(json, type, RequiredValueException::new);
    }

    public static <T> T readRequired(String json, Class<T> type, Supplier<? extends RuntimeException> missing) {
        return read(json, type).orElseThrow(missing);
    }

    private static <T> T decode(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException error) {
            throw new EventException(ES_COMMON_018);
        }
    }
}
