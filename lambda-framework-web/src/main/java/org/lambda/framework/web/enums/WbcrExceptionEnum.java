package org.lambda.framework.web.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum WbcrExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_WEB_000("ES_WEB_000","系统错误");
    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private WbcrExceptionEnum(String code, String message) {
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
