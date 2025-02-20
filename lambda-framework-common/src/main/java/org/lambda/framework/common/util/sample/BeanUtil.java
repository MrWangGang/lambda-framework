package org.lambda.framework.common.util.sample;

import org.apache.commons.beanutils.BeanUtils;
import org.lambda.framework.common.exception.EventException;
import org.springframework.cglib.beans.BeanCopier;

import java.lang.reflect.InvocationTargetException;

import static org.lambda.framework.common.enums.CommonExceptionEnum.*;

public class BeanUtil {

    public static  <T,V> V copy(V target,T source) {
        try {
             BeanUtils.copyProperties(target,source);
             return target;
        } catch (IllegalAccessException e) {
            throw new EventException(ES_COMMON_000,"bean copy失败");
        } catch (InvocationTargetException e) {
            throw new EventException(ES_COMMON_000,"bean copy失败");
        }
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
