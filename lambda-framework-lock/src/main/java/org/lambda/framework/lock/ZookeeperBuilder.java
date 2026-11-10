package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.lambda.framework.common.exception.Assert;

import static org.lambda.framework.enums.LockExceptionEnum.LOCK_NAME_REQUIRED;

public class ZookeeperBuilder {
    private CuratorFramework curatorFramework;
    public ZookeeperBuilder(CuratorFramework curatorFramework){
        this.curatorFramework = curatorFramework;
    }
    public ZookeeperLock getLock(String lockName){
        Assert.verify(lockName,LOCK_NAME_REQUIRED);
        return new ZookeeperLock(curatorFramework,lockName);
    }
}
