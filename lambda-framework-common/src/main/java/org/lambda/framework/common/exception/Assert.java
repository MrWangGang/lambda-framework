package org.lambda.framework.common.exception;

import org.apache.commons.lang3.StringUtils;

public class Assert {
    public static void verify(Object object,ExceptionEnumFunction exceptionEnumFunction){
        if(object instanceof String){
            if(StringUtils.isBlank((String) object)){
                throw new EventException(exceptionEnumFunction);
            }
        }
        if(object == null)throw new EventException(exceptionEnumFunction);
    }
}
