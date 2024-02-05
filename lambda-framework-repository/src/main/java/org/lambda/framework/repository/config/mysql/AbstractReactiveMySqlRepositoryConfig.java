package org.lambda.framework.repository.config.mysql;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.lambda.framework.repository.config.ExcludeR2dbcAutoConfig;

import java.time.Duration;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

public abstract class AbstractReactiveMySqlRepositoryConfig extends ExcludeR2dbcAutoConfig {

    protected abstract String host();
    protected abstract String user();
    protected abstract String password();
    protected abstract String database();
    protected abstract Integer port();
    protected abstract Integer connectTimeoutSeconds();
    //用于指定数据库连接在空闲状态下的最大存活时间
    protected abstract Integer maxIdleTimeSeconds();
    //连接池中的最大连接数
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
