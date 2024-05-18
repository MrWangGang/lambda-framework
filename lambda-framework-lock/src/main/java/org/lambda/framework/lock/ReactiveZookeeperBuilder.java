package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.lambda.framework.common.exception.Assert;

import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_000;

public class ReactiveZookeeperBuilder {
    private CuratorFramework curatorFramework;
    public ReactiveZookeeperBuilder(CuratorFramework curatorFramework){
        this.curatorFramework = curatorFramework;
    }
    public ReactiveZookeeperLock getLock(String lockName){
        Assert.verify(lockName,ES_LOCK_000);
        return new ReactiveZookeeperLock(curatorFramework,lockName);
    }
}
