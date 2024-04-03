package org.lambda.framework.gateway.enums;

import lombok.Getter;
import lombok.Setter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum GatewayExceptionEnum implements ExceptionEnumFunction {

    //系统异常-普通异常 0-99
    ES_GATEWAY_000("ES_GATEWAY_000","系统错误[未知]"),
    ES_GATEWAY_001("ES_GATEWAY_001","协议缺失"),
    ES_GATEWAY_002("ES_GATEWAY_002","网关协议未知"),
    ES_GATEWAY_003("ES_GATEWAY_003","无法找到实例"),
    ES_GATEWAY_004("ES_GATEWAY_004","body不合规"),
    ES_GATEWAY_005("ES_GATEWAY_005","无效的协议头 RSocket-Model"),
    ES_GATEWAY_006("ES_GATEWAY_006","无效的rsocket 模式"),
    ES_GATEWAY_007("ES_GATEWAY_007","解析目标地址返回值出错"),
    ES_GATEWAY_008("ES_GATEWAY_008","服务名缺失"),
    ES_GATEWAY_009("ES_GATEWAY_009","无效的协议头 RSocket-Echo"),
    ES_GATEWAY_010("ES_GATEWAY_010","无效的 rsocket 响应类型"),
    ES_GATEWAY_011("ES_GATEWAY_011","不支持query params的形式传输"),
    ES_GATEWAY_012("ES_GATEWAY_012","缺失Content-Type"),
    ES_GATEWAY_013("ES_GATEWAY_013","Content-Type仅支持 application/json 和application/octet-stream"),
    ES_GATEWAY_016("ES_GATEWAY_016","[static][获取 METHOD_PARAMETER 时，找不都方法");







    // 成员变量

    @Getter
    @Setter
    private String code;

    @Getter
    @Setter
    private String message;
    // 构造方法
    private GatewayExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;

    }
}
