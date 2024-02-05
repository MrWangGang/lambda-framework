package org.lambda.framework.repository.config.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.PropertiesMongoConnectionDetails;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

public abstract class AbstractReactiveMongoRepositoryConfig {
    protected abstract String host();
    protected abstract String user();
    protected abstract String password();
    protected abstract String database();
    protected abstract Integer port();
    protected abstract Integer connectTimeoutSeconds();
    protected abstract Integer maxIdleTimeSeconds();
    protected abstract Integer maxSize();

    protected MongoClient buildMongoConnectionFactory() {
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
        return MongoClients.create(settings);
    }

    @Bean
    public PropertiesMongoConnectionDetails mongoConnectionDetails() {
        MongoProperties mongoProperties = new MongoProperties();
        mongoProperties.setHost(this.host());
        mongoProperties.setUsername(this.user());
        mongoProperties.setPassword(this.password().toCharArray());
        mongoProperties.setDatabase(this.database());
        mongoProperties.setPort(this.port());
        mongoProperties.setAuthenticationDatabase(this.database());
        return new PropertiesMongoConnectionDetails(mongoProperties);
    }
}
