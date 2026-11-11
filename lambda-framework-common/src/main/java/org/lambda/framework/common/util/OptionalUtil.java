package org.lambda.framework.common.util;

import java.util.List;
import java.util.Map;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.CommonExceptionEnum;

import java.util.Optional;
import java.util.function.Supplier;

public final class OptionalUtil {
    private OptionalUtil() {}

    public static <T> T require(T value) {
        return require(value, () -> new EventException(CommonExceptionEnum.REQUIRED_VALUE_MISSING));
    }

    public static <T> T require(T value, Supplier<? extends RuntimeException> missing) {
        return Optional.ofNullable(value).orElseThrow(missing);
    }

    public static <K, V> Optional<V> find(Map<K, V> values, K key) {
        return Optional.ofNullable(values).flatMap(map -> Optional.ofNullable(key).map(map::get));
    }

    public static <T> List<T> listOrEmpty(List<T> values) {
        return Optional.ofNullable(values).orElseGet(List::of);
    }
}
