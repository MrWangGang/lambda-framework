package org.lambda.framework.lock.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.startup.ComponentCheck;
import org.lambda.framework.common.startup.DeploymentMode;
import org.lambda.framework.enums.LockExceptionEnum;
import org.lambda.framework.lock.ReactiveZookeeperBuilder;
import org.lambda.framework.lock.ZookeeperBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@AutoConfiguration
@ConditionalOnProperty("lambda.lock.zookeeper.connect-string")
@EnableConfigurationProperties(ZookeeperAutoConfiguration.Settings.class)
public class ZookeeperAutoConfiguration {
    @ConfigurationProperties("lambda.lock.zookeeper")
    record Settings(String connectString, @DefaultValue("single") DeploymentMode mode,
                    @DefaultValue("locks") String namespace, @DefaultValue("5s") Duration timeout) {}

    @Bean(initMethod = "start", destroyMethod = "close")
    CuratorFramework zookeeperClient(Settings settings) {
        long count = java.util.Arrays.stream(settings.connectString().split(","))
                .map(String::strip).filter(node -> !node.isEmpty()).distinct().count();
        if (settings.mode() == DeploymentMode.SINGLE ? count != 1 : count < 2) {
            throw new EventException(LockExceptionEnum.CONFIGURATION_INVALID, "ZooKeeper single requires one address; cluster requires multiple addresses");
        }
        return CuratorFrameworkFactory.builder().connectString(settings.connectString())
                .connectionTimeoutMs(Math.toIntExact(settings.timeout().toMillis()))
                .retryPolicy(new ExponentialBackoffRetry(150, 3))
                .namespace(settings.namespace()).build();
    }

    @Bean
    ReactiveZookeeperBuilder reactiveZookeeperBuilder(CuratorFramework client) {
        return new ReactiveZookeeperBuilder(client);
    }

    @Bean
    ZookeeperBuilder zookeeperBuilder(CuratorFramework client) {
        return new ZookeeperBuilder(client);
    }

    @Bean
    ComponentCheck zookeeperCheck(CuratorFramework client, Settings settings) {
        return new ComponentCheck("ZooKeeper", () -> Mono.fromCallable(() -> {
            if (!client.blockUntilConnected(Math.toIntExact(settings.timeout().toMillis()),
                    java.util.concurrent.TimeUnit.MILLISECONDS)) {
                throw new EventException(LockExceptionEnum.ZOOKEEPER_MUTEX_UNAVAILABLE, "ZooKeeper connection timed out");
            }
            return client.checkExists().forPath("/");
        }).subscribeOn(Schedulers.boundedElastic()).then());
    }
}
