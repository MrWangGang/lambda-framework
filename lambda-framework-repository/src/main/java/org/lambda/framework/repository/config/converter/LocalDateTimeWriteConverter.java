package org.lambda.framework.repository.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WritingConverter
public class LocalDateTimeWriteConverter implements Converter<LocalDateTime,String> {
    @Override
    public String convert(LocalDateTime source) {
        // 使用指定格式将 LocalDate 转换为 String
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        return source.format(formatter);
    }
}
