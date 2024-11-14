package org.lambda.framework.security.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public enum SecurityExceptionEnum implements ExceptionEnumFunction {

    ES_SECURITY_000("ES_SECURITY_000","身份认证异常");//AuthenticationException

    // 成员变量
    private String code;

    private String message;
    // 构造方法
    private SecurityExceptionEnum(String code, String message) {
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
