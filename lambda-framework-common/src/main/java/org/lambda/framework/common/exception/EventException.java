package org.lambda.framework.common.exception;

import org.lambda.framework.common.enums.ExceptionEnum;
import org.lambda.framework.common.exception.basic.GlobalException;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public class EventException extends GlobalException {

    public EventException(ExceptionEnum error) {
        super(error.getCode(),error.getMessage());
    }

    public EventException(ExceptionEnum error, String message) {
        super(error.getCode(),message);
    }
}
