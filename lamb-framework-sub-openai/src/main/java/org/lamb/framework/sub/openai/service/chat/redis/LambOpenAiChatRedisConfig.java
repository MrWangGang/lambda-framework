package org.lamb.framework.sub.openai.service.chat.redis;

import org.lamb.framework.sub.openai.LambOpenAiRedisConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

@Configuration
public class LambOpenAiChatRedisConfig  extends LambOpenAiRedisConfig {
    //##数据库序号
    private Integer database;
    @Value("${lamb.openai.chat.redis.database:0}")
    public void setDatabase(Integer database) {
        super.database = database;
    }

    @Bean("lambOpenAiChatRedisTemplate")
    public ReactiveRedisTemplate lambOpenAiChatRedisTemplate(){
        return super.lambOpenAiRedisTemplate();
    }

}
