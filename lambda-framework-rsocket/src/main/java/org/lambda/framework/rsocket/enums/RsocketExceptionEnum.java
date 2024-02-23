package org.lambda.framework.rsocket.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum RsocketExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_WEB_000("ES_WEB_000","系统错误"),
    ES_WEB_001("ES_WEB_001","[static][获取 METHOD_PARAMETER 时，找不都方法"),
    ES_WEB_002("ES_WEB_002","response不允许返回其他类型");


    // 成员变量

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;
    // 构造方法
    private RsocketExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
}
