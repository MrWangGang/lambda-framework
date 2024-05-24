package org.lambda.framework.repository.config.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.repository.enums.MongoDeployModelEnum;

import java.util.concurrent.TimeUnit;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public abstract class AbstractReactiveMongoRepositoryConfig {
    protected abstract String host();
    protected abstract String user();
    protected abstract String password();
    protected abstract String authDatabase();
    protected abstract String database();
    protected abstract Integer connectTimeoutSeconds();
    protected abstract Integer maxIdleTimeSeconds();
    protected abstract Integer maxSize();
    protected abstract String replicaSetName();
    protected abstract String deployModel();

    public MongoClient buildMongoOperation() {
        Assert.verify(this.deployModel(),ES_REPOSITORY_MONGO_010);
        Assert.verify(this.replicaSetName(),ES_REPOSITORY_MONGO_009);
        Assert.verify(this.host(),ES_REPOSITORY_MONGO_004);
        Assert.verify(this.user(),ES_REPOSITORY_MONGO_005);
        Assert.verify(this.password(),ES_REPOSITORY_MONGO_006);
        Assert.verify(this.database(),ES_REPOSITORY_MONGO_007);
        Assert.verify(this.authDatabase(),ES_REPOSITORY_MONGO_008);
        Assert.verify(this.connectTimeoutSeconds(),ES_REPOSITORY_MONGO_011);
        Assert.verify(this.maxIdleTimeSeconds(),ES_REPOSITORY_MONGO_012);
        Assert.verify(this.maxSize(),ES_REPOSITORY_MONGO_013);
        MongoDeployModelEnum.isValid(this.deployModel());
        switch (MongoDeployModelEnum.valueOf(deployModel())) {
            case single:
                return single();
            case cluster:
                return cluster();
            default:
                throw new EventException(ES_REPOSITORY_MONGO_014);
        }
    }


    private MongoClient single() {
        ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder()
                .maxSize(maxSize())
                .maxConnectionIdleTime(maxIdleTimeSeconds(), TimeUnit.SECONDS)
                .build();
        String[] hosts = host().split(",");
        String connectionString = "mongodb://" + hosts[0];


        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(connectTimeoutSeconds(), TimeUnit.SECONDS))
                .applyToConnectionPoolSettings(builder -> builder.applySettings(connectionPoolSettings))
                .applyConnectionString(new ConnectionString(connectionString))
                .credential(MongoCredential.createCredential(user(), authDatabase(), password().toCharArray()))
                .build();
        return MongoClients.create(settings);
    }

    private MongoClient cluster() {
        ConnectionPoolSettings connectionPoolSettings = ConnectionPoolSettings.builder()
                .maxSize(maxSize())
                .maxConnectionIdleTime(maxIdleTimeSeconds(), TimeUnit.SECONDS)
                .build();
        String connectionString = "mongodb://" + this.host() + "/?replicaSet=" + this.replicaSetName();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(connectTimeoutSeconds(), TimeUnit.SECONDS))
                .applyToConnectionPoolSettings(builder -> builder.applySettings(connectionPoolSettings))
                .applyConnectionString(new ConnectionString(connectionString))
                .credential(MongoCredential.createCredential(user(), authDatabase(), password().toCharArray()))
                .build();
        return MongoClients.create(settings);
    }
}
