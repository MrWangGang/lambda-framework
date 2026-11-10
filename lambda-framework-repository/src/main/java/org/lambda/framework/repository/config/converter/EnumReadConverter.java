package org.lambda.framework.repository.config.converter;

import org.lambda.framework.common.enums.ConverterEnum;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.EnumUtil;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.data.convert.ReadingConverter;

import static org.lambda.framework.enums.RepositoryExceptionEnum.UNKNOWN_ENUM_VALUE;
@ReadingConverter
public class EnumReadConverter<T extends ConverterEnum> implements ConverterFactory<String, T> {


    @Override
    public <S extends T> Converter<String, S> getConverter(Class<S> targetType) {
        return new EnumConverter<>(targetType);
    }

    private static class EnumConverter<T extends ConverterEnum> implements Converter<String, T> {

        private final Class<T> enumType;

        public EnumConverter(Class<T> targetType) {
            this.enumType = targetType;
        }

        @Override
        public T convert(String source) {
            return EnumUtil.findByKey(enumType, source).orElseThrow(() -> new EventException(UNKNOWN_ENUM_VALUE));
        }
    }
}
