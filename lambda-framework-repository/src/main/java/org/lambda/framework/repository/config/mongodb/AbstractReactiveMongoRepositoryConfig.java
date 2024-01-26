package org.lambda.framework.repository.config.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.connection.ConnectionPoolSettings;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;

import java.util.concurrent.TimeUnit;

public abstract class AbstractReactiveMongoRepositoryConfig  extends AbstractReactiveMongoConfiguration {
    protected abstract String host();
    protected abstract String user();
    protected abstract String password();
    protected abstract String database();
    protected abstract Integer port();
    protected abstract Integer connectTimeoutSeconds();
    protected abstract Integer maxIdleTimeSeconds();
    protected abstract Integer maxSize();

    @Override
    protected String getDatabaseName() {
        return database();
    }

    @Override
    protected MongoClientSettings mongoClientSettings() {
        ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder()
                .maxSize(maxSize())
                .maxConnectionIdleTime(maxIdleTimeSeconds(), TimeUnit.SECONDS)
                .build();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(connectTimeoutSeconds(), TimeUnit.SECONDS))
                .applyToConnectionPoolSettings(builder -> builder.applySettings(connectionPoolSettings))
                .applyConnectionString(new ConnectionString("mongodb://" + host() + ":" + port()))
                .credential(MongoCredential.createCredential(user(), database(), password().toCharArray()))
                .build();
        return settings;
    }
}
