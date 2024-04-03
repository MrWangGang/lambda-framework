package org.lambda.framework.web.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum WebExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_WEB_000("ES_WEB_000","系统错误");



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
