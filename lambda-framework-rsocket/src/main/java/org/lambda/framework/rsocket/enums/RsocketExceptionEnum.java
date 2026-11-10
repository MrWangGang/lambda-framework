package org.lambda.framework.rsocket.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum RsocketExceptionEnum implements ExceptionEnumFunction {
    ES_RSOCKET_000("S0200000", "处理失败，请稍后重试"),
    ES_RSOCKET_001("S0200001", "请求参数不正确"),
    ES_RSOCKET_002("S0200002", "选项不正确"),
    ES_RSOCKET_003("S0200003", "无法识别请求身份");

    private final String code;
    private final String message;

    RsocketExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
