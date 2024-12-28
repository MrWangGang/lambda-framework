package org.lambda.framework.lock.config;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.lock.ReactiveZookeeperBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_ZOOKEEPER_021;

public abstract class DefaultReactiveZookeeperConfig extends AbstractReactiveZookeeperConfig {

    protected String host;
    @Value("${lambda.lock.zookeeper.maxRetries:3}")
    private Integer maxRetries;
    @Value("${lambda.lock.zookeeper.baseSleepTimeMs:5000}")
    private Integer baseSleepTimeMs;
    @Value("${lambda.lock.zookeeper.connectionTimeoutMs:10000}")
    private Integer connectionTimeoutMs;
    @Value("${lambda.lock.zookeeper.sessionTimeoutMs:10000}")
    private Integer sessionTimeoutMs;

    @Bean
    public ReactiveZookeeperBuilder zookeeperConnectionFactory() {
        return super.zookeeperConnectionFactory();
    }

    @Override
    @Value("${lambda.lock.zookeeper.host:-1}")
    protected String host() {
        if(host == null || StringUtils.isBlank(host) || "-1".equals(host)){
            throw new EventException(ES_LOCK_ZOOKEEPER_021);
        }
        return this.host;
    }

    @Override
    protected Integer sessionTimeoutMs() {
        return this.sessionTimeoutMs;
    }

    @Override
    protected Integer connectionTimeoutMs() {
        return this.connectionTimeoutMs;
    }

    @Override
    protected Integer baseSleepTimeMs() {
        return this.baseSleepTimeMs;
    }

    @Override
    protected Integer maxRetries() {
        return this.maxRetries;
    }
}
