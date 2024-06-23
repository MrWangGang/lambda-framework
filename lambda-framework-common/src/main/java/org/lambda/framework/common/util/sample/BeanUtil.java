package org.lambda.framework.common.util.sample;

import net.sf.cglib.beans.BeanCopier;
import org.lambda.framework.common.exception.EventException;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_030;

public class BeanUtil {
    private static final BeanUtil INSTANCE = new BeanUtil();
    public static BeanUtil getInstance() {
        return INSTANCE;
    }
    public <T> T deepCopy(T source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T target = instantiateTarget(targetClass);
        BeanCopier beanCopier = BeanCopier.create(source.getClass(), target.getClass(), false);
        beanCopier.copy(source, target, null);
        return target;
    }

    private <T> T instantiateTarget(Class<T> targetClass) {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new EventException(ES_COMMON_030);
        }
    }
}
