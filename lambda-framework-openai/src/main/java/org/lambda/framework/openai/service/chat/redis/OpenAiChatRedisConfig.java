package org.lambda.framework.openai.service.chat.redis;


import org.lambda.framework.openai.AbstractOpenAiRedisConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class OpenAiChatRedisConfig extends AbstractOpenAiRedisConfig {
    //##数据库序号
    @Value("${lambda.openai.chat.redis.database:0}")
    private Integer database;

    @Bean("openAiChatRedisTemplate")
    public ReactiveRedisTemplate openAiChatRedisTemplate(){
        return super.redisTemplate();
    }

    @Override
    protected Integer database() {
        return this.database;
    }

}
