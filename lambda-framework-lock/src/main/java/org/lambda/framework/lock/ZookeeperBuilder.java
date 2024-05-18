package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.lambda.framework.common.exception.Assert;

import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_000;

public class ZookeeperBuilder {
    private CuratorFramework curatorFramework;
    public ZookeeperBuilder(CuratorFramework curatorFramework){
        this.curatorFramework = curatorFramework;
    }
    public ZookeeperLock getLock(String lockName){
        Assert.verify(lockName,ES_LOCK_000);
        return new ZookeeperLock(curatorFramework,lockName);
    }
}
