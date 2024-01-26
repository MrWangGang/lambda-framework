package org.lambda.framework.repository.config.mysql;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnableDefaultReactiveMysqlRepositoryConfig extends DefaultReactiveMySqlRepositoryConfig  {
    @Bean
    public ConnectionFactory mysql(){
        return buildMysqlConnectionFactory();
    }

}
