package org.lambda.framework.sub.openai.service.chat.redis;


import org.lambda.framework.sub.openai.RedisConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class ChatRedisConfig extends RedisConfig {
    //##数据库序号
    private Integer database;
    @Value("${lambda.openai.chat.redis.database:0}")
    public void setDatabase(Integer database) {
        super.database = database;
    }

    @Bean("openAiChatRedisTemplate")
    public ReactiveRedisTemplate openAiChatRedisTemplate(){
        return super.redisTemplate();
    }

}
