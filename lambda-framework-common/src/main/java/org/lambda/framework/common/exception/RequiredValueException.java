package org.lambda.framework.common.exception;

import org.lambda.framework.enums.CommonExceptionEnum;

public final class RequiredValueException extends EventException {
    public RequiredValueException() {
        super(CommonExceptionEnum.REQUIRED_VALUE_MISSING);
    }
}
