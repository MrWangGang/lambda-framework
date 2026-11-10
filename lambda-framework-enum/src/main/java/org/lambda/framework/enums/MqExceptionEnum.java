package org.lambda.framework.enums;

import lombok.Getter;

@Getter
public enum MqExceptionEnum implements ExceptionEnumFunction {
    KAFKA_DESTINATION_REQUIRED("S0820001", "消息暂未处理，请稍后重试"),
    KAFKA_MESSAGE_REQUIRED("S0820002", "消息暂未处理，请稍后重试"),
    KAFKA_KEY_REQUIRED("S0820003", "消息暂未处理，请稍后重试"),
    RABBIT_PUBLISH_NOT_CONFIRMED("S0800000", "消息暂未处理，请稍后重试");

    private final String code;
    private final String message;

    MqExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
