package org.lambda.framework.repository.config.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import jakarta.annotation.Resource;
import org.apache.commons.logging.LogFactory;
import org.lambda.framework.common.enums.ConverterEnum;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.repository.config.converter.EnumReadConverter;
import org.lambda.framework.repository.config.converter.EnumWriteConverter;
import org.lambda.framework.repository.enums.MongoDeployModelEnum;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.PropertiesMongoConnectionDetails;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(new EnumReadConverter<ConverterEnum>(),new EnumWriteConverter())); // 注册你的自定义转换器
    }



    @Bean
    public PropertiesMongoConnectionDetails mongoConnectionDetails() {
        String[] hosts = this.host().split(","); // 如果主机是以逗号分隔的字符串，则将其拆分为一个主机数组
        // 构建包含多个主机的URI
        StringBuilder uriBuilder = new StringBuilder("mongodb://");
        for (int i = 0; i < hosts.length; i++) {
            uriBuilder.append(hosts[i]);
            if (i < hosts.length - 1) {
                uriBuilder.append(",");
            }
        }
        String databaseName = this.database();
        if (databaseName != null && !databaseName.isEmpty()) {
            uriBuilder.append("/").append(databaseName);
        }
        MongoProperties mongoProperties = new MongoProperties();
        mongoProperties.setUri(uriBuilder.toString());
        mongoProperties.setUsername(this.user());
        mongoProperties.setPassword(this.password().toCharArray());
        mongoProperties.setAuthenticationDatabase(this.authDatabase());
        mongoProperties.setAutoIndexCreation(true);
        return new PropertiesMongoConnectionDetails(mongoProperties);
    }

    @Bean
    public ReactiveMongoTransactionManager transactionManager(ReactiveMongoDatabaseFactory dbFactory) {
        return new ReactiveMongoTransactionManager(dbFactory);
    }

    private static final Logger logger = Logger.getLogger(LogFactory.class.getName());

    @Configuration
    public static class CreationIndex{
        @Resource
        private ReactiveMongoTemplate reactiveMongoTemplate;
        @Resource
        private MongoConverter mongoConverter;

        @EventListener(ApplicationReadyEvent.class)
        public void initIndicesAfterStartup() {

            logger.info("Mongo InitIndicesAfterStartup init");
            var init = System.currentTimeMillis();
            var mappingContext = this.mongoConverter.getMappingContext();
            if (mappingContext instanceof MongoMappingContext) {
                MongoMappingContext mongoMappingContext = (MongoMappingContext) mappingContext;
                for (MongoPersistentEntity<?> persistentEntity : mongoMappingContext.getPersistentEntities()) {
                    var clazz = persistentEntity.getType();
                    if (clazz.isAnnotationPresent(Document.class)) {
                        var resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
                        var indexOps = reactiveMongoTemplate.indexOps(clazz);
                        resolver.resolveIndexFor(clazz).forEach(i->indexOps.ensureIndex(i).subscribe());
                    }
                }
            }

            logger.info("Mongo InitIndicesAfterStartup take: {}"+(System.currentTimeMillis() - init));
        }
    }

}
