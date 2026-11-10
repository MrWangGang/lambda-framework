package org.lambda.framework.common.enums;

import java.util.Optional;
import java.util.regex.Pattern;

public final class ServiceCodes {
    public static final String SUCCESS = "00000000";
    private static final Pattern ERROR = Pattern.compile("[SB][0-9]{7}");
    private static final int MAX_MESSAGE_LENGTH = 24;

    private ServiceCodes() {}

    public static boolean isError(String code) {
        return Optional.ofNullable(code).filter(value -> ERROR.matcher(value).matches()).isPresent();
    }

    public static boolean isBusiness(String code) {
        return isError(code) && code.charAt(0) == 'B';
    }

    public static boolean isPublicMessage(String message) {
        return Optional.ofNullable(message).filter(value -> !value.isBlank()
                && value.codePointCount(0, value.length()) <= MAX_MESSAGE_LENGTH
                && value.codePoints().noneMatch(Character::isISOControl)).isPresent();
    }
}
