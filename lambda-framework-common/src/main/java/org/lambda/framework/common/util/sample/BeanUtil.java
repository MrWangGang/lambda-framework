package org.lambda.framework.common.util.sample;

import org.lambda.framework.common.exception.EventException;
import org.springframework.cglib.beans.BeanCopier;

import static org.lambda.framework.common.enums.CommonExceptionEnum.*;

public class BeanUtil {

    public static  <T> T deepCopy(T source, Class<T> targetClass) {
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

    private static  <T> T instantiateTarget(Class<T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new EventException(ES_COMMON_030);
        }
    }
}
