package org.lambda.framework.common.util.sample;

import org.lambda.framework.common.exception.EventException;
import org.modelmapper.ModelMapper;
import org.springframework.cglib.beans.BeanCopier;

import static org.lambda.framework.common.enums.CommonExceptionEnum.*;

public class BeanUtil {


    public static <T, V> V copy(Class<V> target, T source) {
        ModelMapper modelMapper = new ModelMapper();
        if (source == null || target == null) {
            return null;
        }
        return modelMapper.map(source, target);
    }

    public static  <T> T deepCopy(T source, Class<? extends T> targetClass) {
        if (source == null) {
            throw new EventException(ES_COMMON_031);
        }
        if (targetClass == null) {
            throw new EventException(ES_COMMON_032);
        }
        T target = instantiateTarget(targetClass);
        BeanCopier.create(source.getClass(), target.getClass(), false)
                .copy(source.getClass(),target.getClass(),null);
        return target;
    }

    public static  <T> T shallowCopy
            (T source, Class<? extends T> targetClass) {
        if (source == null) {
            throw new EventException(ES_COMMON_031);
        }
        if (targetClass == null) {
            throw new EventException(ES_COMMON_032);
        }
        T target = instantiateTarget(targetClass);
        BeanCopier.create(source.getClass(), target.getClass(), false)
                .copy(source.getClass(),target.getClass(),null);
        return target;
    }

    private static  <T> T instantiateTarget(Class<? extends T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new EventException(ES_COMMON_030);
        }
    }
}
