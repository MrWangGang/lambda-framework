package org.lambda.framework.repository.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@WritingConverter
public class LocalDateWriteConverter implements Converter<LocalDate,String> {
    @Override
    public String convert(LocalDate source) {
        // 使用指定格式将 LocalDate 转换为 String
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return source.format(formatter);
    }
}
