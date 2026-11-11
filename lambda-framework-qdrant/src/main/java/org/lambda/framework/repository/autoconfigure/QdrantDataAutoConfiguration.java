package org.lambda.framework.repository.autoconfigure;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.startup.ComponentCheck;
import org.lambda.framework.common.startup.DeploymentMode;
import org.lambda.framework.enums.QdrantExceptionEnum;
import org.lambda.framework.repository.operation.qdrant.ReactiveQdrantOperations;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@Conditional(QdrantDataAutoConfiguration.Configured.class)
@EnableConfigurationProperties(QdrantDataAutoConfiguration.Settings.class)
public class QdrantDataAutoConfiguration {
    static class Configured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return Binder.get(context.getEnvironment()).bind("lambda.repository.qdrant.endpoints", String[].class).isBound();
        }
    }

    @ConfigurationProperties("lambda.repository.qdrant")
    record Settings(@DefaultValue("single") DeploymentMode mode, List<URI> endpoints,
                    String apiKey, @DefaultValue("5s") Duration timeout) {}

    @Bean
    @ConditionalOnMissingBean
    ReactiveQdrantOperations reactiveQdrantOperations(Settings settings) {
        var endpoints = settings.endpoints().stream().distinct().toList();
        if (settings.mode() == DeploymentMode.SINGLE ? endpoints.size() != 1 : endpoints.size() < 2) {
            throw new EventException(QdrantExceptionEnum.CONFIGURATION_INVALID, "Qdrant single requires one endpoint; cluster requires multiple endpoints");
        }
        var clients = new ArrayList<QdrantClient>();
        try {
            for (URI endpoint : endpoints) {
                boolean tls = "grpcs".equals(endpoint.getScheme());
                if ((!tls && !"grpc".equals(endpoint.getScheme())) || endpoint.getHost() == null
                        || endpoint.getUserInfo() != null || endpoint.getPort() < 1 || endpoint.getPort() > 65535
                        || StringUtils.hasText(endpoint.getPath()) || endpoint.getQuery() != null || endpoint.getFragment() != null) {
                    throw new EventException(QdrantExceptionEnum.CONFIGURATION_INVALID, "Qdrant endpoint must be grpc[s]://host:port");
                }
                var builder = QdrantGrpcClient.newBuilder(endpoint.getHost(), endpoint.getPort(), tls).withTimeout(settings.timeout());
                if (StringUtils.hasText(settings.apiKey())) {
                    builder.withApiKey(settings.apiKey());
                }
                clients.add(new QdrantClient(builder.build()));
            }
            return new ReactiveQdrantOperations(clients);
        } catch (RuntimeException error) {
            clients.forEach(QdrantClient::close);
            if (error instanceof EventException event) {
                throw event;
            }
            throw new EventException(QdrantExceptionEnum.CONFIGURATION_INVALID, null, error);
        }
    }

    @Bean
    ComponentCheck qdrantCheck(ReactiveQdrantOperations qdrant) {
        return new ComponentCheck("Qdrant", qdrant::check);
    }
}
