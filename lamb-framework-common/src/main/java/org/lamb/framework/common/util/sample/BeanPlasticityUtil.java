package org.lamb.framework.common.util.sample;


import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.StringUtils;
import org.lamb.framework.common.exception.LambEventException;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.lamb.framework.common.enums.LambExceptionEnum.*;

public class BeanPlasticityUtil {

    public static <T> T copy(Class<T> t, Object orig) {
        try {
            T result = t.newInstance();
            org.apache.commons.beanutils.BeanUtils.copyProperties(result, orig);
            return result;
        } catch (IllegalAccessException e) {
            throw new LambEventException(ES00000022);
        } catch (InstantiationException e) {
            throw new LambEventException(ES00000022);
        } catch (InvocationTargetException e) {
            throw new LambEventException(ES00000022);
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
                    throw new LambEventException(ES00000023);
                }
                if (StringUtils.isBlank((String) value)) {
                    throw new LambEventException(ES00000024);
                }

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    return df.parse((String) value);
                } catch (ParseException e) {
                    throw new LambEventException(ES00000025);
                }
            }
        }, java.util.Date.class);
    }
}


