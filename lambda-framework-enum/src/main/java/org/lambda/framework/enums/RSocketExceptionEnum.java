package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum RSocketExceptionEnum implements ExceptionEnumFunction {
    REQUEST_FAILED("S0200000", "处理失败，请稍后重试"),
    INVALID_ARGUMENTS("S0200001", "请求参数不正确"),
    INVALID_VALUE_FORMAT("S0200002", "选项不正确"),
    INVALID_IDENTITY("S0200003", "无法识别请求身份"),
    SETUP_REJECTED("S0200004", "RSocket 连接配置不匹配"),
    REQUEST_REJECTED("S0200005", "RSocket 请求不符合协议");

    private final String code;
    private final String message;

    RSocketExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
