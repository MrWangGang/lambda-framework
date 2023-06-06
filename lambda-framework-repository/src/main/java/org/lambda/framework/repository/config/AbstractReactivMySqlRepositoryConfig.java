package org.lambda.framework.repository.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

import java.time.Duration;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

public abstract class AbstractReactivMySqlRepositoryConfig {

    protected abstract String host();
    protected abstract String user();
    protected abstract String password();
    protected abstract String database();
    protected abstract Integer port();
    protected abstract Integer connectTimeoutSeconds();
    protected abstract Integer maxIdleTimeSeconds();
    protected abstract Integer maxSize();

    protected ConnectionFactory buildMysqlConnectionFactory(){
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(DRIVER, "mysql")
                .option(HOST, host())
                .option(USER, user())
                .option(PORT, port())
                .option(PASSWORD, password())
                .option(DATABASE, database())
                .option(CONNECT_TIMEOUT, Duration.ofSeconds(connectTimeoutSeconds()))
                .build();
        ConnectionFactory connectionFactory = ConnectionFactories.get(options);
        ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(connectionFactory)
                .maxIdleTime(Duration.ofSeconds(maxIdleTimeSeconds()))
                .maxSize(maxSize())
                .build();
        // ConnectionPool实现了ConnectionFactory接口，使用ConnectionFactory替换ConnectionFactory
        return new ConnectionPool(configuration);
    }
}
