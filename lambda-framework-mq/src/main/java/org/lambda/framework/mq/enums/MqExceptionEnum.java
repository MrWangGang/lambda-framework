package org.lambda.framework.mq.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum MqExceptionEnum implements ExceptionEnumFunction {
    ES_MQ_RABBITMQ_000("S0810000", "消息暂未处理，请稍后重试"),
    ES_MQ_RABBITMQ_001("S0810001", "消息暂未处理，请稍后重试"),
    ES_MQ_RABBITMQ_002("S0810002", "消息暂未处理，请稍后重试"),
    ES_MQ_RABBITMQ_003("S0810003", "消息暂未处理，请稍后重试"),
    ES_MQ_RABBITMQ_004("S0810004", "消息暂未处理，请稍后重试"),
    ES_MQ_RABBITMQ_005("S0810005", "消息暂未处理，请稍后重试"),
    ES_MQ_RABBITMQ_006("S0810006", "消息暂未处理，请稍后重试"),
    ES_MQ_RABBITMQ_007("S0810007", "消息暂未处理，请稍后重试"),
    ES_MQ_RABBITMQ_008("S0810008", "消息暂未处理，请稍后重试"),
    ES_MQ_KAFKA_000("S0820000", "消息暂未处理，请稍后重试"),
    ES_MQ_KAFKA_001("S0820001", "消息暂未处理，请稍后重试"),
    ES_MQ_KAFKA_002("S0820002", "消息暂未处理，请稍后重试"),
    ES_MQ_KAFKA_003("S0820003", "消息暂未处理，请稍后重试"),
    ES_MQ_000("S0800000", "消息暂未处理，请稍后重试");

    private final String code;
    private final String message;

    MqExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
