package org.lambda.framework.common.exception;

import org.lambda.framework.common.enums.CommonExceptionEnum;

public final class RequiredValueException extends EventException {
    public RequiredValueException() {
        super(CommonExceptionEnum.ES_COMMON_036);
    }
}
