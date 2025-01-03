package org.lambda.framework.mq.config.kafka;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.factory.annotation.Value;

import static org.lambda.framework.mq.enums.MqExceptionEnum.ES_MQ_KAFKA_000;

public  class DefaultReactiveKafkaMQConfig extends AbstractReactiveKafkaMQConfig {
    private String host;

    @Value("${lambda.mq.kafka.host:-1}")
    private void setHost(String host) {
        if(host == null || StringUtils.isBlank(host) || "-1".equals(host)){
            throw new EventException(ES_MQ_KAFKA_000);
        }
        this.host = host;
    }

    @Override
    protected String host() {
        return this.host;
    }
}
