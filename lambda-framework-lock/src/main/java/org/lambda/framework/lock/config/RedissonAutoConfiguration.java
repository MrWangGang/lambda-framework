package org.lambda.framework.lock.config;


import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.startup.ComponentCheck;
import org.lambda.framework.common.startup.DeploymentMode;
import org.lambda.framework.enums.LockExceptionEnum;
import org.lambda.framework.lock.ReactiveRedissonBuilder;
import org.lambda.framework.lock.RedissonBuilder;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@AutoConfiguration
@Conditional(RedissonAutoConfiguration.Configured.class)
@EnableConfigurationProperties(RedissonAutoConfiguration.Settings.class)
public class RedissonAutoConfiguration {
    static class Configured implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return Binder.get(context.getEnvironment()).bind("lambda.repository.redis.nodes", String[].class).isBound();
        }
    }

    @ConfigurationProperties("lambda.repository.redis")
    record Settings(@DefaultValue("single") DeploymentMode mode, List<String> nodes, String username,
                    String password, @DefaultValue("0") int database, @DefaultValue("false") boolean tls) {}

    @Bean(destroyMethod = "shutdown")
    RedissonClient redissonClient(Settings settings) {
        var nodes = settings.nodes().stream().distinct()
                .map(node -> (settings.tls() ? "rediss://" : "redis://") + node).toList();
        var config = new Config().setLazyInitialization(true);
        String username = StringUtils.hasText(settings.username()) ? settings.username() : null;
        String password = StringUtils.hasText(settings.password()) ? settings.password() : null;
        if (settings.mode() == DeploymentMode.SINGLE) {
            if (nodes.size() != 1 || settings.database() < 0) {
                throw new EventException(LockExceptionEnum.CONFIGURATION_INVALID, "Redisson single requires one node and a valid database");
            }
            config.useSingleServer().setAddress(nodes.getFirst()).setDatabase(settings.database())
                    .setUsername(username).setPassword(password);
        } else {
            if (nodes.size() < 2 || settings.database() != 0) {
                throw new EventException(LockExceptionEnum.CONFIGURATION_INVALID, "Redisson cluster requires multiple nodes and database 0");
            }
            config.useClusterServers().addNodeAddress(nodes.toArray(String[]::new))
                    .setUsername(username).setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean
    RedissonBuilder redissonBuilder(RedissonClient client) {
        return new RedissonBuilder(client);
    }

    @Bean
    ReactiveRedissonBuilder reactiveRedissonBuilder(RedissonClient client) {
        return new ReactiveRedissonBuilder(client.reactive());
    }

    @Bean
    ComponentCheck redissonCheck(RedissonClient client) {
        return new ComponentCheck("Redisson", () -> Mono.fromRunnable(() -> {
            // An EXISTS check opens the client without mutating data.
            client.getBucket("lambda:startup:lock").isExists();
        }).subscribeOn(Schedulers.boundedElastic()).then());
    }
}
