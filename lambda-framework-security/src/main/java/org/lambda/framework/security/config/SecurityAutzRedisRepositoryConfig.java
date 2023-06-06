package org.lambda.framework.security.config;

import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityAutzRedisRepositoryConfig extends AbstractSecurityRedisRepositoryConfig {
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
}

