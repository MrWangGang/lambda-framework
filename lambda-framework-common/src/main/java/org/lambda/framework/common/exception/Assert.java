package org.lambda.framework.common.exception;

public class Assert {
    public static void verify(Object object,ExceptionEnumFunction exceptionEnumFunction){
        if(object == null)throw new EventException(exceptionEnumFunction);
    }
}
