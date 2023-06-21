package org.lambda.framework.repository.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.MySqlDialect;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DefaultReactiveMysqlRepositoryDateTypeConverter {
    @Bean
    public R2dbcCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new Converter<Timestamp, LocalDateTime>() {
            @Override
            public LocalDateTime convert(Timestamp source) {
                LocalDateTime localDateTime = source.toLocalDateTime();
                String formattedDateTime = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return LocalDateTime.parse(formattedDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        });
        converters.add(new Converter<LocalDateTime, Timestamp>() {
            @Override
            public Timestamp convert(LocalDateTime source) {
                String formattedDateTime = source.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return Timestamp.valueOf(formattedDateTime);
            }
        });
        return R2dbcCustomConversions.of(MySqlDialect.INSTANCE, converters);
    }
}
