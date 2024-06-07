package org.lambda.framework.common.exception;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;

import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_026;
import static org.lambda.framework.common.enums.CommonExceptionEnum.ES_COMMON_029;

public class Assert {


    public static void verify(ExceptionEnumFunction exceptionEnumFunction,Object...objects){
        for (Object object:objects){
            verify(exceptionEnumFunction, object);
        }
    }
    public static void verify(Object object,ExceptionEnumFunction exceptionEnumFunction){
        if(!verify(object)){
            throw new EventException(exceptionEnumFunction);
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
        return true;
    }

    public static void check(Object obj,ExceptionEnumFunction exceptionEnumFunction) {
        if(!check(obj)){
            throw new EventException(exceptionEnumFunction);
        }
    }

    public static <T>T review(T obj) {
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
                }
                return false;
            } catch (IllegalAccessException e) {
                // 处理异常，如果有需要
                throw new EventException(ES_COMMON_026);
            }
        }

        return true;
    }
}
