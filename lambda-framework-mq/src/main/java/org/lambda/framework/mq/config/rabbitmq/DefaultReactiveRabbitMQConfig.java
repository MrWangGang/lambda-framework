package org.lambda.framework.mq.config.rabbitmq;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.springframework.beans.factory.annotation.Value;

import static org.lambda.framework.mq.enums.MqExceptionEnum.*;

public class DefaultReactiveRabbitMQConfig extends AbstractReactiveRabbitMQConfig{
    private String host;

    private String user;

    private String password;

    @Value("${lambda.mq.rabbitmq.host:-1}")
    private void setHost(String host) {
        if(host == null || StringUtils.isBlank(host) || "-1".equals(host)){
            throw new EventException(ES_MQ_RABBITMQ_000);
        }
        this.host = host;
    }

    @Value("${lambda.mq.rabbitmq.user:-1}")
    private void setUser(String user) {
        if(user == null || StringUtils.isBlank(user) || "-1".equals(user)){
            throw new EventException(ES_MQ_RABBITMQ_001);
        }
        this.user = user;
    }

    @Value("${lambda.mq.rabbitmq.password:-1}")
    private void setPassword(String password) {
        if(password == null || StringUtils.isBlank(password) ||"-1".equals(password)){
            throw new EventException(ES_MQ_RABBITMQ_002);
        }
        this.password = password;
    }

    @Override
    protected String host() {
        return this.host;
    }

    @Override
    protected String user() {
        return this.user;
    }

    @Override
    protected String password() {
        return this.password;
    }
}
