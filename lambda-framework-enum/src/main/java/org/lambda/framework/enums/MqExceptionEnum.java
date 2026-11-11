package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum MqExceptionEnum implements ExceptionEnumFunction {
    KAFKA_DESTINATION_REQUIRED("S0820001", "消息暂未处理，请稍后重试"),
    KAFKA_MESSAGE_REQUIRED("S0820002", "消息暂未处理，请稍后重试"),
    KAFKA_KEY_REQUIRED("S0820003", "消息暂未处理，请稍后重试"),
    RABBIT_PUBLISH_NOT_CONFIRMED("S0800000", "消息暂未处理，请稍后重试"),
    CONFIGURATION_INVALID("S0800001", "消息组件配置不正确"),
    CONNECTION_FAILED("S0800002", "消息服务暂不可用"),
    INVALID_MESSAGE("S0800003", "消息参数不正确");

    private final String code;
    private final String message;

    MqExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
