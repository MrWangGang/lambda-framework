package org.lambda.framework.openai.service.image.redis;

import org.lambda.framework.openai.AbstractOpenAiRedisConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class OpenAiImageRedisConfig extends AbstractOpenAiRedisConfig {
    //##数据库序号
    @Value("${lambda.openai.image.redis.database:2}")
    private Integer database;

    @Bean("openAiImageRedisTemplate")
    public ReactiveRedisTemplate openAiImageRedisTemplate(){
        return super.redisTemplate();
    }

    @Override
    protected Integer database() {
        return this.database;
    }

}
