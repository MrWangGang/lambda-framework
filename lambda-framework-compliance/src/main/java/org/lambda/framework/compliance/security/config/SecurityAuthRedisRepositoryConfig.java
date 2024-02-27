package org.lambda.framework.compliance.security.config;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.redis.config.AbstractReactiveRedisRepositoryConfig;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_025;

@Configuration
public class SecurityAuthRedisRepositoryConfig extends AbstractReactiveRedisRepositoryConfig {
    //##Redis服务器地址
    protected String host;
    @Value("${lambda.security.redis.auth.host:0}")
    public void setHost(String host) {
        if("0".equals(host))throw new EventException(ES_COMPLIANCE_025);
        this.host = host;
    }
    //## Redis服务器连接端口
    @Value("${lambda.security.redis.auth.port:6379}")
    protected Integer port;
    //连接池密码
    @Value("${lambda.security.redis.auth.password:}")
    protected String password;
    //# 连接池最大连接数
    @Value("${lambda.security.redis.auth.lettuce.pool.max-active:8}")
    protected Integer maxActive;
    //# 连接池最大阻塞等待时间（使用负值表示没有限制）
    @Value("${lambda.security.redis.auth.lettuce.pool.max_wait-seconds:50}")
    protected Integer maxWaitSeconds;

    //# 连接池中的最大空闲连接
    @Value("${lambda.security.redis.auth.lettuce.pool.max-idle:8}")
    protected Integer maxIdle;

    //# 连接池中的最小空闲连接
    @Value("${lambda.security.redis.auth.lettuce.pool.min-idle:0}")
    protected Integer minIdle;

    //##数据库序号
    @Value("${lambda.security.redis.auth.database:0}")
    private Integer database;
    @Bean("securityAuthRedisOperation")
    public ReactiveRedisOperation securityAuthRedisOperation(){
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

