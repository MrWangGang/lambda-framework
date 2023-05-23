package org.lambda.framework.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class SecurityAuthRedisConfig extends AbstractSecurityRedisConfig {
    //##数据库序号
    @Value("${lambda.security.redis.auth.database:0}")
    private Integer database;
    @Bean("securityAuthRedisTemplate")
    public ReactiveRedisTemplate securityAuthRedisTemplate(){
        return redisTemplate();
    }

    @Override
    protected Integer database() {
        return this.database;
    }
}

