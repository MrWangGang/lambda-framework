package org.lamb.framework.sub.openai.service.paint.redis;

import org.lamb.framework.sub.openai.LambOpenAiRedisConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class LambOpenAiPaintRedisConfig extends LambOpenAiRedisConfig {
    //##数据库序号
    private Integer database;
    @Value("${lamb.openai.paint.redis.database:2}")
    public void setDatabase(Integer database) {
        super.database = database;
    }

    @Bean("lambOpenAiPaintRedisTemplate")
    public ReactiveRedisTemplate LambOpenAiPaintRedisTemplate(){
        return super.lambOpenAiRedisTemplate();
    }

}
