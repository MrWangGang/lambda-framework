package org.lambda.framework.common.exception;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.enums.ExceptionEnumFunction;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.lambda.framework.enums.CommonExceptionEnum.*;

public class Assert {
    public static <E extends Enum<E> & ExceptionEnumFunction> void verify(Object object,E exceptionEnumFunction){
        if(!verify(object)){
            throw new EventException(exceptionEnumFunction);
        }
    }

    public static <E extends Enum<E> & ExceptionEnumFunction> void verify(Object object,E exceptionEnumFunction,String message){
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

    public static <E extends Enum<E> & ExceptionEnumFunction> void check(Object obj,E exceptionEnumFunction) {
        if(!check(obj)){
            throw new EventException(exceptionEnumFunction);
        }
    }

    public static <E extends Enum<E> & ExceptionEnumFunction> void check(Object obj,E exceptionEnumFunction,String message) {
        if(!check(obj)){
            throw new EventException(exceptionEnumFunction,message);
        }
    }

    public static <E extends Enum<E> & ExceptionEnumFunction> void ifExistCheckNotNull(Object obj,E exceptionEnumFunction){
        if(obj!=null){
            check(obj,exceptionEnumFunction);
        }
    }

    public static <E extends Enum<E> & ExceptionEnumFunction> void ifExistCheckNotNull(Object obj,E exceptionEnumFunction,String message){
        if(obj!=null){
            check(obj,exceptionEnumFunction,message);
        }
    }

    public static <T>T review(T obj) {
        if(obj == null) return null;
        if(!check(obj)){
            throw new EventException(OBJECT_VALIDATION_FAILED);
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
                throw new EventException(FIELD_ACCESS_FAILED);
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
                    if (sets != null && !sets.isEmpty() && !sets.iterator().hasNext()  && sets.iterator().next() != null) {
                        return true;
                    }
                } else {
                    if (value != null) {
                        return true;
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new EventException(FIELD_ACCESS_FAILED);
        }

        return false;
    }
}
