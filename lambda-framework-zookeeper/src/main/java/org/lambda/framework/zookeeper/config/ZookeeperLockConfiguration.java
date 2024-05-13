package org.lambda.framework.zookeeper.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.lambda.framework.common.exception.Assert;
import org.springframework.integration.zookeeper.lock.ZookeeperLockRegistry;

import static org.lambda.framework.zookeeper.enums.ZookeeperExceptionEnum.*;

public abstract class ZookeeperLockConfiguration {
    protected abstract String getHost();

    protected abstract String getPort();

    protected abstract String getUserName();

    protected abstract String getPassword();

    protected abstract Integer getMaxElapsedTimeMs();

    protected abstract Integer getSleepMsBetweenRetries();

    protected CuratorFramework build() throws Exception {
        Assert.verify(getHost(),ES_ZOOKEEPER_001);
        Assert.verify(getPort(),ES_ZOOKEEPER_002);
        Assert.verify(getUserName(),ES_ZOOKEEPER_003);
        Assert.verify(getPassword(),ES_ZOOKEEPER_004);
        Assert.verify(getMaxElapsedTimeMs(),ES_ZOOKEEPER_005);
        Assert.verify(getSleepMsBetweenRetries(),ES_ZOOKEEPER_006);

        String connectString = getHost()+":"+getPort();
        String username = this.getUserName();
        String password = this.getPassword();
        String authString = username + ":" + password;
        String authEncoded = DigestAuthenticationProvider.generateDigest(authString);
        return CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new RetryUntilElapsed(getMaxElapsedTimeMs(), getSleepMsBetweenRetries()))
                .authorization("digest", authEncoded.getBytes())
                .build();
    }

    protected ZookeeperLockRegistry lockRegistry(CuratorFramework curatorFramework,String root) {
        return new ZookeeperLockRegistry(curatorFramework, root);
    }
}
