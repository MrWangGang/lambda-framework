package org.lambda.framework.security.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

/**
 * Created by WangGang on 2017/6/22 0022.
 * E-mail userbean@outlook.com
 * The final interpretation of this procedure is owned by the author
 */
public enum SecurityExceptionEnum implements ExceptionEnumFunction {

    //系统异常-spring security 异常 - 200-299
    ES_SECURITY_000("ES_SECURITY_000","身份认证失败"), //AuthenticationException
    ES_SECURITY_001("ES_SECURITY_001","拒绝访问"), //AccessDeniedException
    ES_SECURITY_002("ES_SECURITY_002","访问用户为空"),
    ES_SECURITY_003("ES_SECURITY_003","无效令牌"),
    ES_SECURITY_004("ES_SECURITY_004","用户信息不存在"),
    ES_SECURITY_005("ES_SECURITY_005","无效的用户签名值"),
    ES_SECURITY_006("ES_SECURITY_006","用户类型不存在"),
    ES_SECURITY_007("ES_SECURITY_007","令牌格式不符合规范"),
    ES_SECURITY_008("ES_SECURITY_008","无法获取spring security全局上下文变量"),
    ES_SECURITY_009("ES_SECURITY_009","用户信息CAST失败"),
    ES_SECURITY_010("ES_SECURITY_010","未知的lambda.security.url-autz-model");

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
