package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.lambda.framework.common.exception.Assert;

import static org.lambda.framework.enums.LockExceptionEnum.LOCK_NAME_REQUIRED;

public class ReactiveZookeeperBuilder {
    private CuratorFramework curatorFramework;
    public ReactiveZookeeperBuilder(CuratorFramework curatorFramework){
        this.curatorFramework = curatorFramework;
    }
    public ReactiveZookeeperLock getLock(String lockName){
        Assert.verify(lockName,LOCK_NAME_REQUIRED);
        return new ReactiveZookeeperLock(curatorFramework,lockName);
    }
}
