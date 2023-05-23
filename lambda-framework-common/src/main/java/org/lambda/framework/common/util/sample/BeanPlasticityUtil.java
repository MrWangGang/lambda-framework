package org.lambda.framework.common.util.sample;


import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.lambda.framework.common.enums.CommonExceptionEnum.*;

public class BeanPlasticityUtil {

    public static <T> T copy(Class<T> t, Object orig) {
        try {
            T result = t.newInstance();
            org.apache.commons.beanutils.BeanUtils.copyProperties(result, orig);
            return result;
        } catch (IllegalAccessException e) {
            throw new EventException(ES_COMMON_021);
        } catch (InstantiationException e) {
            throw new EventException(ES_COMMON_021);
        } catch (InvocationTargetException e) {
            throw new EventException(ES_COMMON_021);
        }
    }

    static {
        ConvertUtils.register(new Converter() {
            @Override
            public Object convert(Class type, Object value) {
                if (value == null) {
                    return null;
                }
                if (!(value instanceof String)) {
                    throw new EventException(ES_COMMON_022);
                }
                if (StringUtils.isBlank((String) value)) {
                    throw new EventException(ES_COMMON_023);
                }

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    return df.parse((String) value);
                } catch (ParseException e) {
                    throw new EventException(ES_COMMON_024);
                }
            }
        }, java.util.Date.class);
    }
}


