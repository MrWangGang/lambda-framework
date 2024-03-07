package org.lambda.framework.web.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum WebExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_WEB_000("ES_WEB_000","系统错误"),
    ES_WEB_001("ES_WEB_001","[static][获取 METHOD_PARAMETER 时，找不都方法"),
    ES_WEB_002("ES_WEB_002","无效令牌"),
    ES_WEB_003("ES_WEB_003","用户信息不存在"),
    ES_WEB_008("ES_WEB_008","无法获取WEB全局上下文变量"),
    ES_WEB_0014("ES_WEB_0014","无法访问Stash");



    // 成员变量

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;
    // 构造方法
    private WebExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
}
