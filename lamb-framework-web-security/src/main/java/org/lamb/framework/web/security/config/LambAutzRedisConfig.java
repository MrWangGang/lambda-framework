package org.lamb.framework.web.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class LambAutzRedisConfig extends LambAZRedisSupportConfig {
    //##数据库序号
    private Integer database;
    @Value("${lamb.security.redis.autz.database:1}")
    public void setDatabase(Integer database) {
        super.database = database;
    }

    @Bean("lambAutzRedisTemplate")
    public ReactiveRedisTemplate lambAutzRedisTemplate(){
        return lambSecurityRedisTemplate();
    }

}
