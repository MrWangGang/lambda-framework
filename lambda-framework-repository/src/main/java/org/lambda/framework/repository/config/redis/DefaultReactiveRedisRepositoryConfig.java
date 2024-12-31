package org.lambda.framework.repository.config.redis;

import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class DefaultReactiveRedisRepositoryConfig extends AbstractReactiveRedisRepositoryConfig {
    @Value("${lambda.repository.redis.host:}")
    protected String host;
    @Value("${lambda.repository.redis.password:}")
    protected String password;
    @Value("${lambda.repository.redis.database:}")
    protected Integer database;
    @Value("${lambda.repository.redis.deploy:single}")
    protected String deployModel;
    @Value("${lambda.repository.redis.master:master}")
    protected String masterName;
    @Value("${lambda.repository.redis.lettuce.pool.max-active:8}")
    protected Integer maxActive;
    @Value("${lambda.repository.redis.lettuce.pool.max_wait-seconds:50}")
    protected Integer maxWaitSeconds;
    @Value("${lambda.repository.redis.lettuce.pool.max-idle:8}")
    protected Integer maxIdle;
    @Value("${lambda.repository.redis.lettuce.pool.min-idle:0}")
    protected Integer minIdle;
    @Bean
    public ReactiveRedisOperation reactiveRedisOperation(){
        return buildRedisOperation();
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
    protected Integer maxActive() {
        return this.maxActive;
    }

    @Override
    protected Integer maxWaitSeconds() {
        return this.maxWaitSeconds;
    }

    @Override
    protected Integer maxIdle() {
        return this.maxIdle;
    }

    @Override
    protected Integer minIdle() {
        return this.minIdle;
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
