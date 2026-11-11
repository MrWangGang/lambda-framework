package org.lambda.framework.common.exception;

import org.lambda.framework.common.exception.basic.GlobalException;
import org.lambda.framework.enums.ExceptionEnumFunction;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public class EventException extends GlobalException {
    private final ExceptionEnumFunction error;

    public <E extends Enum<E> & ExceptionEnumFunction> EventException(E error) {
        super(error.getCode(),error.getMessage());
        this.error = error;
    }

    public <E extends Enum<E> & ExceptionEnumFunction> EventException(E error, String message) {
        this(error, message, null);
    }

    public <E extends Enum<E> & ExceptionEnumFunction> EventException(E error, String message, Throwable cause) {
        this(error);
        if (message != null && !message.isBlank()) {
            initCause(new IllegalStateException(message, cause));
        } else if (cause != null) {
            initCause(cause);
        }
    }

    public ExceptionEnumFunction getError() {
        return error;
    }
}
