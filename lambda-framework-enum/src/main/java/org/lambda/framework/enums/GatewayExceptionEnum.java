package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum GatewayExceptionEnum implements ExceptionEnumFunction {
    UNEXPECTED_FAILURE("S0100000", "系统繁忙，请稍后重试"),
    UPSTREAM_UNAVAILABLE("S0100001", "服务暂不可用，请稍后重试"),
    INVALID_REQUEST("S0100002", "请求参数不正确"),
    NOT_FOUND("S0100003", "找不到请求的内容"),
    NOT_ALLOWED("S0100004", "暂不支持此操作"),
    RESPONSE_MISSING("S0100005", "未获取到所需结果"),
    CONTENT_TOO_LARGE("S0100006", "内容过大，请缩小后重试"),
    TIMEOUT("S0100007", "请求超时，请重试"),
    NO_ACCESS("S0100008", "没有权限执行此操作");

    private final String code;
    private final String message;

    GatewayExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
