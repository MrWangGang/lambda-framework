package org.lambda.framework.common.util.sample;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_018;
import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_019;


/**
 * @description: JSON解析工具
 * @author: Mr.WangGang
 * @create: 2018-11-23 下午 12:51
 **/
public class JsonUtil {

    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = JsonMapper.builder()
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

    public static ObjectMapper getJsonFactory() {
        return objectMapper;
    }

    public static String objToString(Object data){
        try {
            ObjectMapper objectMapper = getJsonFactory();
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new EventException(ES_COMMON_019);
        }
    }

    public static <T>Optional<T> mapToObj(Map data, Class<T> clazz){
        ObjectMapper objectMapper = getJsonFactory();
        return Optional.ofNullable(objectMapper.convertValue(data,clazz));
    }

    public static <T>Optional<T> stringToObj(String data,Class<T> clazz){
        try {
            ObjectMapper objectMapper = getJsonFactory();
            return Optional.ofNullable(objectMapper.readValue(data,clazz));
        } catch (JsonProcessingException e) {
            throw new EventException(ES_COMMON_018);
        }
    }
}
