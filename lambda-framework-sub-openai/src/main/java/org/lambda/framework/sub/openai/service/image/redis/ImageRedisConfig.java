package org.lambda.framework.sub.openai.service.image.redis;

import org.lambda.framework.sub.openai.RedisConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class ImageRedisConfig extends RedisConfig {
    //##数据库序号
    private Integer database;
    @Value("${lambda.openai.image.redis.database:2}")
    public void setDatabase(Integer database) {
        super.database = database;
    }

    @Bean("openAiImageRedisTemplate")
    public ReactiveRedisTemplate openAiImageRedisTemplate(){
        return super.redisTemplate();
    }

}
