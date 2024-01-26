package org.lambda.framework.repository.config.mongodb;

import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnableDefaultReactiveMongoRepositoryConfig extends DefaultReactiveMongoRepositoryConfig {
    @Bean
    public MongoClient mongo(){
        return reactiveMongoClient();
    }
}
