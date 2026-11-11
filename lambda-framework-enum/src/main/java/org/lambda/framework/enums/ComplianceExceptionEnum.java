package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum ComplianceExceptionEnum implements ExceptionEnumFunction {
    CONTEXT_INVALID("S0700000", "无法获取所需身份"),
    SESSION_INVALID("S0700001", "登录已失效，请重新登录"),
    SESSION_CONFIGURATION_INVALID("S0700002", "会话配置不正确"),
    VERSION_REQUIRED("S0700003", "更新数据需要版本号");

    private final String code;
    private final String message;

    ComplianceExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
