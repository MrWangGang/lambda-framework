package org.lambda.framework.repository.config.mysql;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.lambda.framework.repository.config.converter.EnumReadConverter;
import org.lambda.framework.repository.config.converter.EnumWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.time.Duration;
import java.util.Arrays;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

public abstract class AbstractReactiveMySqlRepositoryConfig {

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
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return new R2dbcCustomConversions(CustomConversions.StoreConversions.NONE,Arrays.asList(new EnumReadConverter(),new EnumWriteConverter()));
    }
}
