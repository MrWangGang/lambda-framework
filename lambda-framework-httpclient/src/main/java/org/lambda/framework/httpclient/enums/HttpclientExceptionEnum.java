package org.lambda.framework.httpclient.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum HttpclientExceptionEnum implements ExceptionEnumFunction {

    ES_HTTPCLIENT_000("ES_HTTPCLIENT_000","webclient异常");
    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private HttpclientExceptionEnum(String code, String message) {
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
