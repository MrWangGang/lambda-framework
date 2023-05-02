package org.lambda.framework.web.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class SecurityAutzRedisConfig extends SecurityRedisConfig {
    //##数据库序号
    private Integer database;
    @Value("${lambda.security.redis.autz.database:1}")
    public void setDatabase(Integer database) {
        super.database = database;
    }

    @Bean("securityAutzRedisTemplate")
    public ReactiveRedisTemplate securityAutzRedisTemplate(){
        return redisTemplate();
    }

}

