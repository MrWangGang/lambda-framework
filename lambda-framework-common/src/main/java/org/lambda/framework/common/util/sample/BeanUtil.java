package org.lambda.framework.common.util.sample;

import net.sf.cglib.beans.BeanCopier;
import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.common.enums.CommonExceptionEnum.*;

public class BeanUtil {

    public static <T> T deepCopy(T source, Class<T> targetClass) {
        if (source == null) {
            throw new EventException(ES_COMMON_031);
        }
        if (targetClass == null) {
            throw new EventException(ES_COMMON_032);
        }
        T target = instantiateTarget(targetClass);
        BeanCopier beanCopier = BeanCopier.create(source.getClass(), target.getClass(), false);
        beanCopier.copy(source, target, null);
        return target;
    }

    private static <T> T instantiateTarget(Class<T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new EventException(ES_COMMON_030);
        }
    }
}
