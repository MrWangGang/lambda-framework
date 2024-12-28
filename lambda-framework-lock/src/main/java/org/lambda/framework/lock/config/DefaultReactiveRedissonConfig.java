package org.lambda.framework.lock.config;

import org.apache.commons.lang3.StringUtils;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.lock.ReactiveRedissonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import static org.lambda.framework.lock.enums.LockExceptionEnum.*;

public abstract class DefaultReactiveRedissonConfig extends AbstractReactiveRedissonConfig {
    @Value("${lambda.lock.redisson.host-1:}")
    public void setHost(String host) {
        if(host == null || StringUtils.isBlank(host) || "-1".equals(host)){
            throw new EventException(ES_LOCK_REDISSON_020);
        }
         this.host = host;
    }
    @Value("${lambda.lock.redisson.password:-1}")
    public void setPassword(String password) {
        if(password == null || StringUtils.isBlank(password) || "-1".equals(password)){
            throw new EventException(ES_LOCK_REDISSON_021);
        }
         this.password = password;
    }
    @Value("${lambda.lock.redisson.database:-1}")
    public void setDatabase(Integer database) {
        if(database == null || database.equals(-1)){
            throw new EventException(ES_LOCK_REDISSON_022);
        }
         this.database = database;
    }

    protected String host;
    protected String password;
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
