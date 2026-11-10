package org.lambda.framework.common.exception.basic;

import org.lambda.framework.common.enums.CommonExceptionEnum;
import org.lambda.framework.common.enums.ServiceCodes;

import java.util.Optional;

public class GlobalException extends RuntimeException {
    public static final String EX_PRIEX = "&*lambda-exception*&";
    private final String code;

    public GlobalException(String code, String message) {
        super(ServiceCodes.isError(code) && ServiceCodes.isPublicMessage(message)
                ? message : CommonExceptionEnum.ES_COMMON_000.getMessage());
        this.code = ServiceCodes.isError(code) ? code : CommonExceptionEnum.ES_COMMON_000.getCode();
    }

    public String getCode() {
        return code;
    }

    public String getSerializeMessage() {
        return code + EX_PRIEX + getMessage();
    }

    public static Optional<GlobalException> fromSerialized(String message) {
        return Optional.ofNullable(message).flatMap(value -> {
            int separator = value.indexOf(EX_PRIEX);
            if (separator != 8) {
                return Optional.empty();
            }
            String code = value.substring(0, separator);
            String text = value.substring(separator + EX_PRIEX.length());
            return ServiceCodes.isError(code) && ServiceCodes.isPublicMessage(text)
                    ? Optional.of(new GlobalException(code, text)) : Optional.empty();
        });
    }
}
