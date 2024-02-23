package org.lambda.framework.httpclient.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum HttpClientExceptionEnum implements ExceptionEnumFunction {

    //OPEN AI组件相关   300-399
    ES_RPC_000("ES_RPC_000","spring扩展点获取HttpExchange注解自动配置失败");
    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private HttpClientExceptionEnum(String code, String message) {
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
