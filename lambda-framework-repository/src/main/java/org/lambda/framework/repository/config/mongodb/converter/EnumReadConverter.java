package org.lambda.framework.repository.config.mongodb.converter;

import org.lambda.framework.common.enums.ConverterEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class EnumReadConverter implements ConverterFactory<String, ConverterEnum> {

    @Override
    public <T extends ConverterEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return new EnumConverter(targetType);
    }

    private static class EnumConverter<T extends ConverterEnum> implements Converter<String, T> {

        private Class<T> enumType = null;

        public EnumConverter(Class<T> targetType) {
            this.enumType = targetType;
        }

        @Override
        public T convert(String source) {
            // 在这里使用接口中的 getValue 方法进行转换
            for (T enumConstant : enumType.getEnumConstants()) {
                if (source.equals((enumConstant).getValue())){
                    return enumConstant;
                }
            }
            throw new IllegalArgumentException("Invalid value for enum: " + source);

        }
    }
}
