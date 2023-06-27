package org.lambda.framework.common.util.sample;


import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.lambda.framework.common.exception.EventException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_021;

public class BeanPlasticityUtil {

    public static <T> T copy(Class<T> t, Object orig) {
        try {
            Constructor<T> constructor = t.getDeclaredConstructor();
            constructor.setAccessible(true);
            T result = t.newInstance();
            BeanUtils.copyProperties(result, orig);
            return result;
        } catch (IllegalAccessException e) {
            throw new EventException(ES_COMMON_021);
        } catch (InstantiationException e) {
            throw new EventException(ES_COMMON_021);
        } catch (InvocationTargetException e) {
            throw new EventException(ES_COMMON_021);
        } catch (NoSuchMethodException e) {
            throw new EventException(ES_COMMON_021);
        }
    }

    //target2非空属性复置到target1
    public static void copy(Object target1, Object target2) {
        try {
            BeanUtilsBean beanUtilsBean = BeanUtilsBean.getInstance();
            // 自定义属性复制的行为
            beanUtilsBean.getConvertUtils().register(false, true, 0);
            // 复制target2对象的非空属性到target1对象
            beanUtilsBean.copyProperties(target1, target2);
        } catch (IllegalAccessException e) {
            throw new EventException(ES_COMMON_021);
        } catch (InvocationTargetException e) {
            throw new EventException(ES_COMMON_021);
        }
    }
}


