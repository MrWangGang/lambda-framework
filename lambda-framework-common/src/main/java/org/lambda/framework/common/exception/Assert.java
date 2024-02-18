package org.lambda.framework.common.exception;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class Assert {
    public static void verify(ExceptionEnumFunction exceptionEnumFunction,Object...objects){
        for (Object object:objects){
            if(object instanceof String){
                if(StringUtils.isBlank((String) object)){
                    throw new EventException(exceptionEnumFunction);
                }
            }
            if(object instanceof List){
                if(object == null){
                    throw new EventException(exceptionEnumFunction);
                }
                if(((List)object).size() == 0){
                    throw new EventException(exceptionEnumFunction);
                }
                if(((List)object).get(0) ==  null){
                    throw new EventException(exceptionEnumFunction);
                }
            }
            if(object == null)throw new EventException(exceptionEnumFunction);
        }
    }
    public static void verify(Object object,ExceptionEnumFunction exceptionEnumFunction){
        if(object instanceof String){
            if(StringUtils.isBlank((String) object)){
                throw new EventException(exceptionEnumFunction);
            }
        }
        if(object instanceof List){
            if(object == null){
                throw new EventException(exceptionEnumFunction);
            }
            if(((List)object).size() == 0){
                throw new EventException(exceptionEnumFunction);
            }
            if(((List)object).get(0) ==  null){
                throw new EventException(exceptionEnumFunction);
            }
        }
        if(object == null)throw new EventException(exceptionEnumFunction);
    }

    public static boolean verify(Object object){
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
        if(object == null)return false;
        return true;
    }
}
