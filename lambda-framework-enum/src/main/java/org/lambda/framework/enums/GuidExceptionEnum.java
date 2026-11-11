package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum GuidExceptionEnum implements ExceptionEnumFunction {
    CLOCK_OUT_OF_RANGE("S1100001", "暂时无法创建编号，请稍后重试"),
    CLOCK_MOVED_BACKWARDS("S1100002", "暂时无法创建编号，请稍后重试"),
    SEQUENCE_EXHAUSTED("S1100003", "操作繁忙，请稍后重试"),
    CONFIGURATION_INVALID("S1100004", "编号生成配置不正确");

    private final String code;
    private final String message;

    GuidExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
