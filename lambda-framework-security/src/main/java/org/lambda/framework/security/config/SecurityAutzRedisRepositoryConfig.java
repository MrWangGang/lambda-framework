package org.lambda.framework.security.config;

import org.lambda.framework.redis.config.AbstractReactiveRedisRepositoryConfig;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityAutzRedisRepositoryConfig extends AbstractReactiveRedisRepositoryConfig {
    //##Redis服务器地址
    @Value("${lambda.security.redis.autz.host:0}")
    protected String host;
    //## Redis服务器连接端口
    @Value("${lambda.security.redis.autz.port:6379}")
    protected Integer port;
    //连接池密码
    @Value("${lambda.security.redis.autz.password:}")
    protected String password;
    //# 连接池最大连接数
    @Value("${lambda.security.redis.autz.lettuce.pool.max-active:8}")
    protected Integer maxActive;
    //# 连接池最大阻塞等待时间（使用负值表示没有限制）
    @Value("${lambda.security.redis.autz.lettuce.pool.max_wait-seconds:50}")
    protected Integer maxWaitSeconds;

    //# 连接池中的最大空闲连接
    @Value("${lambda.security.redis.autz.lettuce.pool.max-idle:8}")
    protected Integer maxIdle;

    //# 连接池中的最小空闲连接
    @Value("${lambda.security.redis.autz.lettuce.pool.min-idle:0}")
    protected Integer minIdle;

    //##数据库序号
    @Value("${lambda.security.redis.autz.database:1}")
    private Integer database;

    @Bean("securityAutzRedisOperation")
    public ReactiveRedisOperation securityAutzRedisOperation(){
        return buildRedisOperation();
    }

    @Override
    protected Integer database() {
        return this.database;
    }


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

