package org.lambda.framework.repository.config.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.lambda.framework.common.exception.EventException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.ES_REPOSITORY_101;

public abstract class AbstractReactiveMongoRepositoryConfig {
    protected abstract String host();
    protected abstract String user();
    protected abstract String password();
    protected abstract String database();
    protected abstract Integer port();
    protected abstract Integer connectTimeoutSeconds();
    protected abstract Integer maxIdleTimeSeconds();
    protected abstract Integer maxSize();
    public MongoClient buildMongoClient() {
        ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder()
                .maxSize(maxSize())
                .maxConnectionIdleTime(maxIdleTimeSeconds(), TimeUnit.SECONDS)
                .build();
        String encodedPassword;
        try {
            encodedPassword = URLEncoder.encode(password(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new EventException(ES_REPOSITORY_101);
        }

        String connectionString = String.format("mongodb://%s:%s@%s:%d/%s",
                user(), encodedPassword, host(), port(), database());

        MongoClientSettings settings =  MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .applyToConnectionPoolSettings(builder -> builder.applySettings(connectionPoolSettings))
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(connectTimeoutSeconds(), TimeUnit.SECONDS)) // convert to milliseconds
                .build();
        return MongoClients.create(settings);
    }

}
