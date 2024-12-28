package org.lambda.framework.lock.config;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.lock.RedissonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static org.lambda.framework.lock.enums.LockExceptionEnum.*;

public abstract class DefaultRedissonConfig extends AbstractRedissonConfig {
    protected String host;
    protected String password;
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
    @Value("${lambda.lock.redisson.host-1:}")
    protected String host() {
        if(host == null || StringUtils.isBlank(host) || "-1".equals(host)){
            throw new EventException(ES_LOCK_REDISSON_020);
        }
        return this.host;
    }

    @Override
    @Value("${lambda.lock.redisson.password:-1}")
    protected String password() {
        if(password == null || StringUtils.isBlank(password) || "-1".equals(password)){
            throw new EventException(ES_LOCK_REDISSON_021);
        }
        return this.password;
    }

    @Override
    @Value("${lambda.lock.redisson.database:-1}")
    protected Integer database() {
        if(database == null || database.equals(-1)){
            throw new EventException(ES_LOCK_REDISSON_022);
        }
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
