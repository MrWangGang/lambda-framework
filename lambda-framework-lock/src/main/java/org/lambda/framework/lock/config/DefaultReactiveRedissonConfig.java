package org.lambda.framework.lock.config;

import org.lambda.framework.lock.ReactiveRedissonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public abstract class DefaultReactiveRedissonConfig extends AbstractReactiveRedissonConfig {
    @Value("${lambda.lock.redisson.host:}")
    protected String host;
    @Value("${lambda.lock.redisson.password:}")
    protected String password;
    @Value("${lambda.lock.redisson.database:}")
    protected Integer database;
    @Value("${lambda.lock.redisson.deploy:single}")
    protected String deployModel;
    @Value("${lambda.lock.redisson.master:master}")
    protected String masterName;

    @Bean
    public ReactiveRedissonBuilder reactiveRedissonBuilder() {
        return super.redissonConnectionFactory();
    }

    @Override
    protected String host() {
        return this.host;
    }

    @Override
    protected String password() {
        return this.password;
    }

    @Override
    protected Integer database() {
        return this.database;
    }

    @Override
    protected String deployModel() {
        return this.deployModel;
    }

    @Override
    protected String masterName() {
        return this.masterName;
    }
}
