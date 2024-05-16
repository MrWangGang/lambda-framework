package org.lambda.framework.openai;

import org.lambda.framework.redis.config.AbstractReactiveRedisRepositoryConfig;
import org.springframework.beans.factory.annotation.Value;

public  class AbstractOpenAiRedisRepositoryConfig extends AbstractReactiveRedisRepositoryConfig {
    //##Redis服务器地址
    @Value("${lambda.openai.redis.host:}")
    protected String host;
    //连接池密码
    @Value("${lambda.openai.redis.password:}")
    protected String password;
    //##数据库序号
    @Value("${lambda.openai.redis.database:14}")
    private Integer database;
    @Value("${lambda.openai.redis.deploy:single}")
    protected String deployModel;
    @Value("${lambda.openai.redis.master:master}")
    protected String masterName;
    //# 连接池最大连接数
    @Value("${lambda.openai.redis.lettuce.pool.max-active:8}")
    protected Integer maxActive;
    //# 连接池最大阻塞等待时间（使用负值表示没有限制）
    @Value("${lambda.openai.redis.lettuce.pool.max_wait-seconds:50}")
    protected Integer maxWaitSeconds;
    //# 连接池中的最大空闲连接
    @Value("${lambda.openai.redis.lettuce.pool.max-idle:8}")
    protected Integer maxIdle;
    //# 连接池中的最小空闲连接
    @Value("${lambda.openai.redis.lettuce.pool.min-idle:0}")
    protected Integer minIdle;

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
