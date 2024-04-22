package org.lambda.framework.common.util.sample;

import org.apache.commons.beanutils.BeanUtils;
import org.lambda.framework.common.exception.EventException;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_028;

public class BeanUtil {
    public static <T> T convertHashMapToEntity(HashMap<String, Object> map, Class<T> clazz) {
        try {
            T entity = clazz.newInstance(); // 创建实体类对象
            BeanUtils.populate(entity, map); // 使用BeanUtils将HashMap中的键值对映射到实体类的属性上
            return entity;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new EventException(ES_COMMON_028);
        }
    }
}
