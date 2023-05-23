package org.lambda.framework.security.config;

import org.lambda.framework.redis.config.AbstractReactiveRedisConfig;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractSecurityRedisConfig extends AbstractReactiveRedisConfig {
    //##Redis服务器地址
    @Value("${lambda.security.redis.host:0}")
    protected String host;
    //## Redis服务器连接端口
    @Value("${lambda.security.redis.port:6379}")
    protected Integer port;
    //连接池密码
    @Value("${lambda.security.redis.password:}")
    protected String password;
    //# 连接池最大连接数
    @Value("${lambda.security.redis.lettuce.pool.max_active:8}")
    protected Integer maxActive;
    //# 连接池最大阻塞等待时间（使用负值表示没有限制）
    @Value("${lambda.security.redis.lettuce.pool.max_wait_seconds:50}")
    protected Integer maxWaitSeconds;

    //# 连接池中的最大空闲连接
    @Value("${lambda.security.redis.lettuce.pool.max_idle:8}")
    protected Integer maxIdle;

    //# 连接池中的最小空闲连接
    @Value("${lambda.security.redis.lettuce.pool.min_idle:0}")
    protected Integer minIdle;


    @Override
    protected String host() {
        return this.host;
    }

    @Override
    protected Integer port() {
        return this.port;
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
}
