package org.lambda.framework.common.util.sample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.lambda.framework.common.exception.EventException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.lambda.framework.common.enums.CommonExceptionEnum.*;


/**
 * @description: JSON解析工具
 * @author: Mr.WangGang
 * @create: 2018-11-23 下午 12:51
 **/
public class JsonUtil {

    public static ObjectMapper getJsonFactory(){
        ObjectMapper objectMapper = JsonMapper.builder()
                .build();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);
        return objectMapper;
    }
    public static String objToString(Object data){
        try {
            ObjectMapper objectMapper = JsonMapper.builder()
                    .build();
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            objectMapper.registerModule(javaTimeModule);
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new EventException(ES_COMMON_019);
        }
    }

    public static <T>Optional<T> stringToObj(String data,Class<T> clazz){
        try {
            ObjectMapper objectMapper = JsonMapper.builder()
                    .build();
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            objectMapper.registerModule(javaTimeModule);
            return Optional.ofNullable(objectMapper.readValue(data,clazz));
        } catch (JsonProcessingException e) {
            throw new EventException(ES_COMMON_018);
        }
    }
}
