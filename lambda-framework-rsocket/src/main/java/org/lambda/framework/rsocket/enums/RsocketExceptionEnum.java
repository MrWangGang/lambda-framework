package org.lambda.framework.rsocket.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum RsocketExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_RSOCKET_000("ES_RSOCKET_000","系统错误"),
    ES_RSOCKET_001("ES_RSOCKET_001","请求参数解析错误");



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
