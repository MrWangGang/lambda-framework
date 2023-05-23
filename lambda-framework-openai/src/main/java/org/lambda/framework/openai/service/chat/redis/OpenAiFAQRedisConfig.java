package org.lambda.framework.openai.service.chat.redis;

import org.lambda.framework.openai.AbstractOpenAiRedisConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class OpenAiFAQRedisConfig extends AbstractOpenAiRedisConfig {
    //##数据库序号
    @Value("${lambda.openai.faq.redis.database:1}")
    private Integer database;

    @Bean("openAiFAQRedisTemplate")
    public ReactiveRedisTemplate openAiFAQRedisTemplate(){
        return super.redisTemplate();
    }


    @Override
    protected Integer database() {
        return this.database;
    }

}
