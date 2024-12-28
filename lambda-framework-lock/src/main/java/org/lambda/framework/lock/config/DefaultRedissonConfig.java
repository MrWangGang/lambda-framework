package org.lambda.framework.lock.config;

import org.lambda.framework.lock.RedissonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public abstract class DefaultRedissonConfig extends AbstractRedissonConfig {
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
    public RedissonBuilder reactiveRedissonBuilder() {
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
