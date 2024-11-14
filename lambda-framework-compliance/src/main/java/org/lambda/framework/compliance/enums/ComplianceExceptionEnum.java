package org.lambda.framework.compliance.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum ComplianceExceptionEnum implements ExceptionEnumFunction {

    //OPEN AI组件相关   300-399
    ES_COMPLIANCE_000("ES_COMPLIANCE_000","目标对象必须存在"),
    ES_COMPLIANCE_001("ES_COMPLIANCE_001","无效令牌");







    private String code;

    private String message;
    // 构造方法
    private ComplianceExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
