package org.lambda.framework.common.exception;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.basic.GlobalException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.lambda.framework.common.enums.CommonExceptionEnum.*;

public class Assert {
    public static void verify(Object object,String code,String message){
        if(StringUtils.isBlank(code))throw new EventException(ES_COMMON_033);
        if(StringUtils.isBlank(message))throw new EventException(ES_COMMON_034);
        if(!verify(object)){
            throw new GlobalException(code,message);
        }
    }

    public static void check(Object obj,String code,String message) {
        if(StringUtils.isBlank(code))throw new EventException(ES_COMMON_033);
        if(StringUtils.isBlank(message))throw new EventException(ES_COMMON_034);
        if(!check(obj)){
            throw new GlobalException(code,message);
        }
    }

    public static void verify(Object object,ExceptionEnumFunction exceptionEnumFunction){
        if(!verify(object)){
            throw new EventException(exceptionEnumFunction);
        }
    }

    public static void verify(Object object,ExceptionEnumFunction exceptionEnumFunction,String message){
        if(!verify(object)){
            throw new EventException(exceptionEnumFunction,message);
        }
    }

    public static boolean verify(Object object){
        if(object == null)return false;

        if(object instanceof String){
            if(StringUtils.isBlank((String) object)){
                return false;
            }
        }
        if(object instanceof List){
            if(object == null){
                return false;
            }
            if(((List)object).size() == 0){
                return false;
            }
            if(((List)object).get(0) ==  null){
                return false;
            }
        }

        if (object instanceof Set) {
            if (object == null) {
                return false;
            }
            Set<?> set = (Set<?>) object;
            if (set.isEmpty()) {
                return false;
            }
            if (set.iterator().hasNext() && set.iterator().next() == null) {
                return false;
            }
        }

        return true;
    }

    public static void check(Object obj,ExceptionEnumFunction exceptionEnumFunction) {
        if(!check(obj)){
            throw new EventException(exceptionEnumFunction);
        }
    }

    public static void check(Object obj,ExceptionEnumFunction exceptionEnumFunction,String message) {
        if(!check(obj)){
            throw new EventException(exceptionEnumFunction,message);
        }
    }

    public static void ifExistCheckNotNull(Object obj,ExceptionEnumFunction exceptionEnumFunction){
        if(obj!=null){
            check(obj,exceptionEnumFunction);
        }
    }

    public static void ifExistCheckNotNull(Object obj,ExceptionEnumFunction exceptionEnumFunction,String message){
        if(obj!=null){
            check(obj,exceptionEnumFunction,message);
        }
    }

    public static <T>T review(T obj) {
        if(obj == null) return null;
        if(!check(obj)){
            throw new EventException(ES_COMMON_029);
        }
        return obj;
    }

    public static boolean check(Object obj) {
        if(obj == null)return false;

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object object = field.get(obj);
                if (field.get(obj) != null) {
                    if(object instanceof String){
                        if(StringUtils.isBlank((String) object)){
                            return false;
                        }
                    }

                    if(object instanceof List){
                        if(object == null){
                            return false;
                        }
                        if(((List)object).size() == 0){
                            return false;
                        }
                        if(((List)object).get(0) ==  null){
                            return false;
                        }
                    }
                    if (object instanceof Set) {
                        if (object == null) {
                            return false;
                        }
                        Set<?> set = (Set<?>) object;
                        if (set.isEmpty()) {
                            return false;
                        }
                        if (set.iterator().hasNext() && set.iterator().next() == null) {
                            return false;
                        }
                    }

                }else {
                    return false;
                }
            } catch (IllegalAccessException e) {
                // 处理异常，如果有需要
                throw new EventException(ES_COMMON_026);
            }
        }

        return true;
    }

    public static boolean has(Object obj) {
        if (obj == null) {
            return false;
        }

        Field[] fields = obj.getClass().getDeclaredFields();

        try {
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);

                if (value instanceof String) {
                    if (value != null && !((String) value).isEmpty()) {
                        return true;
                    }
                } else if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (list != null && !list.isEmpty() && list.get(0) != null) {
                        return true;
                    }
                }  else if (value instanceof Set) {
                    Set<?> sets = (Set<?>) value;
                    if (sets != null && !sets.isEmpty() && sets.iterator().next() != null) {
                        return true;
                    }
                } else {
                    if (value != null) {
                        return true;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new EventException(ES_COMMON_026);
        }

        return false;
    }
}
