package org.lambda.framework.repository.autoconfigure;

import com.mongodb.ConnectionString;
import org.bson.types.ObjectId;
import org.lambda.framework.common.po.UnifyPO;
import org.lambda.framework.common.startup.ComponentCheck;
import org.lambda.framework.common.startup.DeploymentMode;
import org.lambda.framework.repository.config.converter.EnumReadConverter;
import org.lambda.framework.repository.config.converter.EnumWriteConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@AutoConfiguration(beforeName = {
        "org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration",
        "org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration"})
@ConditionalOnProperty("lambda.repository.mongo.uri")
@EnableConfigurationProperties(MongoDataAutoConfiguration.Settings.class)
@EnableReactiveMongoAuditing(dateTimeProviderRef = "lambdaMongoAuditClock")
public class MongoDataAutoConfiguration {
    @ConfigurationProperties("lambda.repository.mongo")
    record Settings(String uri, @DefaultValue("single") DeploymentMode mode,
                    @DefaultValue("true") boolean createIndexes) {}

    @Bean
    DateTimeProvider lambdaMongoAuditClock() {
        return () -> Optional.of(Instant.now());
    }

    @Bean
    MongoConnectionDetails lambdaMongoConnectionDetails(Settings settings) {
        var uri = new ConnectionString(settings.uri());
        if (uri.getDatabase() == null || uri.getRequiredReplicaSetName() == null) {
            throw new IllegalArgumentException("MongoDB URI must include the database and replicaSet in both modes");
        }
        var writeConcern = uri.getWriteConcern();
        if (writeConcern != null && !writeConcern.isAcknowledged()) {
            throw new IllegalArgumentException("MongoDB versioned entities require acknowledged writes");
        }
        boolean srv = settings.uri().startsWith("mongodb+srv://");
        if (settings.mode() == DeploymentMode.SINGLE && (srv || uri.getHosts().size() != 1)
                || settings.mode() == DeploymentMode.CLUSTER
                    && ((!srv && uri.getHosts().size() < 2) || Boolean.TRUE.equals(uri.isDirectConnection()))) {
            throw new IllegalArgumentException("MongoDB single requires one seed; cluster requires multiple seeds or SRV");
        }
        return () -> uri;
    }

    @Bean
    @ConditionalOnMissingBean
    MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(new EnumReadConverter<>(), new EnumWriteConverter()));
    }

    @Bean
    @ConditionalOnMissingBean
    ReactiveMongoTransactionManager mongoTransactionManager(ReactiveMongoDatabaseFactory database) {
        return new ReactiveMongoTransactionManager(database);
    }

    @Bean
    @ConditionalOnMissingBean
    TransactionalOperator mongoTransactions(ReactiveMongoTransactionManager manager) {
        return TransactionalOperator.create(manager);
    }

    @Bean
    ComponentCheck mongoReplicaCheck(ReactiveMongoTemplate mongo, MongoMappingContext mapping, Settings settings) {
        return new ComponentCheck("MongoDB", () -> mongo.executeCommand("{hello:1}").flatMap(reply -> {
            var hosts = reply.getList("hosts", String.class, List.of());
            boolean valid = reply.getString("setName") != null
                    && (settings.mode() == DeploymentMode.SINGLE ? hosts.size() == 1 : hosts.size() > 1);
            if (!valid) {
                return reactor.core.publisher.Mono.error(new IllegalStateException("MongoDB replica topology does not match mode"));
            }
            var resolver = new MongoPersistentEntityIndexResolver(mapping);
            return Flux.fromIterable(mapping.getPersistentEntities())
                    .filter(entity -> entity.getType().isAnnotationPresent(Document.class))
                    .doOnNext(MongoDataAutoConfiguration::validateDocument)
                    .filter(entity -> settings.createIndexes())
                    .concatMap(entity -> Flux.fromIterable(resolver.resolveIndexFor(entity.getType()))
                            .concatMap(index -> mongo.indexOps(entity.getType()).createIndex(index)))
                    .then();
        }));
    }

    private static void validateDocument(MongoPersistentEntity<?> entity) {
        var id = entity.getIdProperty();
        if (!UnifyPO.class.isAssignableFrom(entity.getType()) || id == null
                || id.getType() != ObjectId.class || !entity.hasVersionProperty()) {
            throw new IllegalStateException(entity.getType().getName()
                    + " must extend UnifyPO<ObjectId> with the inherited MongoDB ID and version");
        }
    }
}
