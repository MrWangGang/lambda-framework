package org.lambda.framework.common.util;



import org.lambda.framework.common.enums.ConverterEnum;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.CommonExceptionEnum;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EnumUtil {
    private static final ClassValue<Map<String, ConverterEnum>> BY_KEY = new ClassValue<>() {
        @Override
        protected Map<String, ConverterEnum> computeValue(Class<?> type) {
            var constants = OptionalUtil.require(type.getEnumConstants(),
                    () -> new EventException(CommonExceptionEnum.OBJECT_VALIDATION_FAILED, "Expected an enum: " + type.getName()));
            var entries = new HashMap<String, ConverterEnum>();
            for (Object constant : constants) {
                var item = (ConverterEnum) constant;
                String key = OptionalUtil.require(item.getValue(),
                        () -> new EventException(CommonExceptionEnum.OBJECT_VALIDATION_FAILED, "Missing enum key: " + type.getName()));
                if (entries.putIfAbsent(key, item) != null) {
                    throw new EventException(CommonExceptionEnum.OBJECT_VALIDATION_FAILED, "Duplicate enum key: " + type.getName() + "." + key);
                }
            }
            return Map.copyOf(entries);
        }
    };

    private EnumUtil() {}

    public static <E extends ConverterEnum> Optional<E> findByKey(Class<E> type, String key) {
        return Optional.ofNullable(key).flatMap(value -> OptionalUtil.find(BY_KEY.get(type), value)).map(type::cast);
    }
}
