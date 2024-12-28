package org.lambda.framework.lock.config;

import org.lambda.framework.lock.ReactiveZookeeperBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public abstract class DefaultReactiveZookeeperConfig extends AbstractReactiveZookeeperConfig {

    @Value("${lambda.lock.zookeeper.host:}")
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
    protected String host() {
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
