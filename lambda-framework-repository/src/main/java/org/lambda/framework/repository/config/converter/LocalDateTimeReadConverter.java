package org.lambda.framework.repository.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.data.convert.ReadingConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ReadingConverter
public class LocalDateTimeReadConverter implements ConverterFactory<String, LocalDateTime>  {

    @Override
    public <T extends LocalDateTime> Converter<String, T> getConverter(Class<T> targetType) {
        return new LocalDateTimeToStringConverterImpl<>();
    }

    private static class LocalDateTimeToStringConverterImpl<T extends LocalDateTime> implements Converter<String, T> {
        @Override
        public T convert(String source) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            LocalDateTime localDateTime = LocalDateTime.parse(source, formatter);
            return  (T) localDateTime;
        }
    }
}
