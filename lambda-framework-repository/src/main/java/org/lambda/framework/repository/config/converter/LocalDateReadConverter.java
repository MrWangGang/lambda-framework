package org.lambda.framework.repository.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.data.convert.ReadingConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@ReadingConverter
public class LocalDateReadConverter implements ConverterFactory<String, LocalDate>  {

    @Override
    public <T extends LocalDate> Converter<String, T> getConverter(Class<T> targetType) {
        return new LocalDateToStringConverterImpl<>();
    }

    private static class LocalDateToStringConverterImpl<T extends LocalDate> implements Converter<String, T> {
        @Override
        public T convert(String source) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(source, formatter);
            return  (T) localDate;
        }
    }
}
