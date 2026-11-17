package org.lambda.framework.common.util.sample;

import java.util.concurrent.ConcurrentHashMap;
import org.lambda.framework.common.exception.EventException;
import org.modelmapper.ModelMapper;
import org.springframework.cglib.beans.BeanCopier;

import static org.lambda.framework.enums.CommonExceptionEnum.*;

public class BeanUtil {
    private static final ModelMapper MODEL_MAPPER = new ModelMapper();
    private static final ConcurrentHashMap<CopierKey, BeanCopier> COPIERS = new ConcurrentHashMap<>();

    private record CopierKey(Class<?> source, Class<?> target) {}

    public static <T, V> V copy(Class<V> target, T source) {
        if (source == null || target == null) {
            return null;
        }
        return MODEL_MAPPER.map(source, target);
    }

    public static <T> T deepCopy(T source, Class<? extends T> targetClass) {
        return copyProperties(source, targetClass);
    }

    public static <T> T shallowCopy(T source, Class<? extends T> targetClass) {
        return copyProperties(source, targetClass);
    }

    private static <T> T copyProperties(T source, Class<? extends T> targetClass) {
        if (source == null) {
            throw new EventException(COPY_SOURCE_MISSING);
        }
        if (targetClass == null) {
            throw new EventException(COPY_TARGET_MISSING);
        }
        T target = instantiateTarget(targetClass);
        var copier = COPIERS.computeIfAbsent(new CopierKey(source.getClass(), target.getClass()),
                key -> BeanCopier.create(key.source(), key.target(), false));
        copier.copy(source, target, null);
        return target;
    }

    private static <T> T instantiateTarget(Class<? extends T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new EventException(OBJECT_CREATION_FAILED, null, e);
        }
    }
}
