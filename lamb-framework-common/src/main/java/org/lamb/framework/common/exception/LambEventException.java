package org.lamb.framework.common.exception;

import org.lamb.framework.common.enums.LambExceptionEnum;
import org.lamb.framework.common.exception.basic.LambGlobalException;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public class LambEventException extends LambGlobalException {

    public LambEventException(LambExceptionEnum error) {
        super(error.getCode(),error.getMessage());
    }

    public LambEventException(LambExceptionEnum error,String message) {
        super(error.getCode(),message);
    }
}
