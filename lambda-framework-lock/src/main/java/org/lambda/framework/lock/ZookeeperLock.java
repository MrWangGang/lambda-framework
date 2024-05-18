package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.lambda.framework.common.exception.EventException;

import java.util.concurrent.TimeUnit;

import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_ZOOKEEPER_017;
import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_ZOOKEEPER_018;

public class ZookeeperLock {
    private CuratorFramework curatorFramework;
    private InterProcessMutex interProcessMutex;
    public ZookeeperLock(CuratorFramework curatorFramework, String lockName){
        this.curatorFramework= curatorFramework;
        this.interProcessMutex = new InterProcessMutex(curatorFramework,lockName);
    }

    public Boolean tryLock() {
        try {
            return interProcessMutex.acquire(1L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_017);
        }
    }

    public void unlock(){
        try {
            interProcessMutex.release();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_018);
        }
    }
}
