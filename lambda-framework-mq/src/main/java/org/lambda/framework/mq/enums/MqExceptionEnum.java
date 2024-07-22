package org.lambda.framework.mq.enums;

import org.lambda.framework.common.exception.ExceptionEnumFunction;

public enum MqExceptionEnum implements ExceptionEnumFunction {

    ES_MQ_RABBITMQ_000("ES_MQ_RABBITMQ_000","rabbitmq缺少host配置"),
    ES_MQ_RABBITMQ_001("ES_MQ_RABBITMQ_001","rabbitmq缺少user配置"),
    ES_MQ_RABBITMQ_002("ES_MQ_RABBITMQ_002","rabbitmq缺少password配置"),
    ES_MQ_RABBITMQ_003("ES_MQ_RABBITMQ_003","rabbitmq缺少clientProvidedName配置"),
    ES_MQ_RABBITMQ_004("ES_MQ_RABBITMQ_004","rabbitmq缺少声明配置"),
    ES_MQ_RABBITMQ_005("ES_MQ_RABBITMQ_005","延迟队列缺少时间"),
    ES_MQ_RABBITMQ_006("ES_MQ_RABBITMQ_006","延迟队列缺少消息ID"),
    ES_MQ_RABBITMQ_007("ES_MQ_RABBITMQ_007","延迟队列缺少消息主体"),

    ES_MQ_000("ES_MQ_000","mq消息发送失败");


    private String code;

    private String message;
    // 构造方法
    private MqExceptionEnum(String code, String message) {
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
