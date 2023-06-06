package org.lambda.framework.openai.service.chat.redis;

import org.lambda.framework.openai.AbstractOpenAiRedisRepositoryConfig;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiFAQRedisRepositoryConfig extends AbstractOpenAiRedisRepositoryConfig {
    //##数据库序号
    @Value("${lambda.openai.faq.redis.database:1}")
    private Integer database;

    @Bean("openAiFAQRedisOperation")
    public ReactiveRedisOperation openAiFAQRedisOperation(){
        return super.buildRedisOperation();
    }


    @Override
    protected Integer database() {
        return this.database;
    }

}
