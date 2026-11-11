package org.lambda.framework.repository.autoconfigure;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.startup.ComponentCheck;
import org.lambda.framework.common.startup.DeploymentMode;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.enums.RepositoryExceptionEnum;
import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
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
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@AutoConfiguration(beforeName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@Conditional(RedisDataAutoConfiguration.Configured.class)
@EnableConfigurationProperties(RedisDataAutoConfiguration.Settings.class)
public class RedisDataAutoConfiguration {
    static class Configured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return Binder.get(context.getEnvironment()).bind("lambda.repository.redis.nodes", String[].class).isBound();
        }
    }

    @ConfigurationProperties("lambda.repository.redis")
    record Settings(@DefaultValue("single") DeploymentMode mode, List<String> nodes,
                    String username, String password, @DefaultValue("0") int database,
                    @DefaultValue("3s") Duration timeout, @DefaultValue("false") boolean tls) {}

    @Bean
    @ConditionalOnMissingBean
    LettuceConnectionFactory redisConnectionFactory(Settings settings) {
        var client = LettuceClientConfiguration.builder().commandTimeout(settings.timeout());
        if (settings.tls()) {
            client.useSsl();
        }
        var password = RedisPassword.of(settings.password());
        if (settings.mode() == DeploymentMode.SINGLE) {
            if (settings.nodes().size() != 1 || settings.database() < 0) {
                throw new EventException(RepositoryExceptionEnum.CONFIGURATION_INVALID, "Redis single requires one host:port and database >= 0");
            }
            var node = RedisNode.fromString(settings.nodes().getFirst());
            var config = new RedisStandaloneConfiguration(node.getHost(), node.getPort());
            config.setDatabase(settings.database());
            config.setUsername(settings.username());
            config.setPassword(password);
            return new LettuceConnectionFactory(config, client.build());
        }
        if (settings.nodes().size() < 2 || settings.database() != 0) {
            throw new EventException(RepositoryExceptionEnum.CONFIGURATION_INVALID, "Redis cluster requires at least two seeds and database 0");
        }
        var config = new RedisClusterConfiguration(settings.nodes());
        config.setUsername(settings.username());
        config.setPassword(password);
        var refresh = ClusterTopologyRefreshOptions.builder().enableAllAdaptiveRefreshTriggers().build();
        client.clientOptions(ClusterClientOptions.builder().topologyRefreshOptions(refresh).build());
        return new LettuceConnectionFactory(config, client.build());
    }

    @Bean
    @ConditionalOnMissingBean
    ReactiveRedisOperation reactiveRedisOperation(LettuceConnectionFactory factory) {
        var json = new Jackson2JsonRedisSerializer<>(JsonUtil.mapper(), Object.class);
        var serialization = RedisSerializationContext.<String, Object>newSerializationContext(StringRedisSerializer.UTF_8)
                .value(json).hashValue(json).build();
        return new ReactiveRedisOperation(factory, serialization);
    }

    @Bean
    ComponentCheck redisCheck(ReactiveRedisOperation redis) {
        return new ComponentCheck("Redis", () -> redis.execute(connection -> connection.ping()).then());
    }
}
