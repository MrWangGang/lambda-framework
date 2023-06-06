package org.lambda.framework.openai.service.chat.redis;


import org.lambda.framework.openai.AbstractOpenAiRedisRepositoryConfig;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiChatRedisRepositoryConfig extends AbstractOpenAiRedisRepositoryConfig {
    //##数据库序号
    @Value("${lambda.openai.chat.redis.database:0}")
    private Integer database;

    @Bean("openAiChatRedisOperation")
    public ReactiveRedisOperation openAiChatRedisOperation(){
        return super.redisOperation();
    }

    @Override
    protected Integer database() {
        return this.database;
    }

}
