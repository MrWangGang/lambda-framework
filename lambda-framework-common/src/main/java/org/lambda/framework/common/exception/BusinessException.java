package org.lambda.framework.common.exception;

import org.lambda.framework.common.enums.ServiceCodes;
import org.lambda.framework.common.util.OptionalUtil;
import org.lambda.framework.enums.ExceptionEnumFunction;

public final class BusinessException extends EventException {
    public <E extends Enum<E> & ExceptionEnumFunction> BusinessException(E error) {
        super(validate(error));
    }

    private static <E extends Enum<E> & ExceptionEnumFunction> E validate(E error) {
        var declared = OptionalUtil.require(error);
        if (!ServiceCodes.isBusiness(declared.getCode()) || !ServiceCodes.isPublicMessage(declared.getMessage())) {
            throw new IllegalArgumentException("Business errors require a B code and a short display message");
        }
        return declared;
    }
}
