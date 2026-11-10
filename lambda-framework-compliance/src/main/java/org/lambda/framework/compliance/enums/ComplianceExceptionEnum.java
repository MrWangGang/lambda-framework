package org.lambda.framework.compliance.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum ComplianceExceptionEnum implements ExceptionEnumFunction {
    ES_COMPLIANCE_000("S0700000", "无法获取所需身份"),
    ES_COMPLIANCE_001("S0700001", "登录已失效，请重新登录");

    private final String code;
    private final String message;

    ComplianceExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
