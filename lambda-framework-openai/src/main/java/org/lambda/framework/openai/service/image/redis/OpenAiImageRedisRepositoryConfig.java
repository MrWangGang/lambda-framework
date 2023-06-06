package org.lambda.framework.openai.service.image.redis;

import org.lambda.framework.openai.AbstractOpenAiRedisRepositoryConfig;
import org.lambda.framework.redis.operation.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiImageRedisRepositoryConfig extends AbstractOpenAiRedisRepositoryConfig {
    //##数据库序号
    @Value("${lambda.openai.image.redis.database:2}")
    private Integer database;

    @Bean("openAiImageRedisOperation")
    public ReactiveRedisOperation openAiImageRedisOperation(){
        return super.buildRedisOperation();
    }

    @Override
    protected Integer database() {
        return this.database;
    }

}
