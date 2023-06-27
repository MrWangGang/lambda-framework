package org.lambda.framework.common.util.sample;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import org.apache.commons.beanutils.BeanUtils;
import org.lambda.framework.common.exception.EventException;

import java.lang.reflect.Constructor;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_021;

public class BeanPlasticityUtil {

    public static <T> T copy(Class<T> t, Object orig) {
        try {
            Constructor<T> constructor = t.getDeclaredConstructor();
            constructor.setAccessible(true);
            T result = t.newInstance();
            BeanUtils.copyProperties(result, orig);
            return result;
        } catch (Exception e) {
            throw new EventException(ES_COMMON_021);
        }
    }

    public static  void copy(Object source, Object target) {
        try {
            BeanUtil.copyProperties(source, target, CopyOptions.create().setIgnoreNullValue(true));
        }catch (Exception e){
            throw new EventException(ES_COMMON_021);

        }
    }
}


