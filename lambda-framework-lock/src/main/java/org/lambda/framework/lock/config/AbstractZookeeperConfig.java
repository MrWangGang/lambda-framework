package org.lambda.framework.lock.config;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.lock.ZookeeperBuilder;

import static org.lambda.framework.lock.enums.LockExceptionEnum.*;

public abstract class AbstractZookeeperConfig {
    // ## Zookeeper 连接字符串
    protected abstract String host();
    // 会话超时时间
    protected abstract Integer sessionTimeoutMs();
    // 连接超时时间
    protected abstract Integer connectionTimeoutMs();
    // 基础重试间隔
    protected abstract Integer baseSleepTimeMs();
    // 最大重试次数
    protected abstract Integer maxRetries();

    private final String ROOT = "locks";
    // 创建 CuratorFramework 客户端
    protected ZookeeperBuilder zookeeperConnectionFactory() {
        Assert.verify(this.host(), ES_LOCK_ZOOKEEPER_010);
        Assert.verify(this.sessionTimeoutMs(), ES_LOCK_ZOOKEEPER_011);
        Assert.verify(this.connectionTimeoutMs(), ES_LOCK_ZOOKEEPER_012);
        Assert.verify(this.baseSleepTimeMs(), ES_LOCK_ZOOKEEPER_013);
        Assert.verify(this.maxRetries(), ES_LOCK_ZOOKEEPER_014);

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(this.baseSleepTimeMs(), this.maxRetries());// 尝试间隔时间，最大尝试次数
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(this.host()) // 连接字符串。zk server 地址和端口 "192.168.149.135:2181,192.168.149.136:2181"
                .sessionTimeoutMs(this.sessionTimeoutMs()) // 会话超时时间 单位ms
                .connectionTimeoutMs(this.connectionTimeoutMs()) // 连接超时时间 单位ms
                .retryPolicy(retryPolicy) // 重试策略
                .namespace(this.ROOT) // 根目录，后续的操作都在/itheima下进行
                .build();
        client.start();
        return new ZookeeperBuilder(client);
    }


}
