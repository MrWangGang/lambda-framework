package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;

import java.util.concurrent.TimeUnit;

import static org.lambda.framework.lock.enums.LockExceptionEnum.*;

public class ZookeeperLock {
    private CuratorFramework curatorFramework;
    private InterProcessMutex interProcessMutex;
    private InterProcessReadWriteLock interProcessReadWriteLock;
    public ZookeeperLock(CuratorFramework curatorFramework, String lockName){
        this.curatorFramework= curatorFramework;
        this.interProcessMutex = new InterProcessMutex(curatorFramework,lockName);
        this.interProcessReadWriteLock = new InterProcessReadWriteLock(curatorFramework,lockName);
    }

    public Boolean tryLock() {
        try {
            Assert.verify(interProcessMutex,ES_LOCK_ZOOKEEPER_019);
            return interProcessMutex.acquire(1L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_017);
        }
    }

    public void unlock(){
        try {
            Assert.verify(interProcessMutex,ES_LOCK_ZOOKEEPER_019);
            interProcessMutex.release();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_018);
        }
    }

    public Boolean tryReadLock() {
        try {
            Assert.verify(interProcessReadWriteLock,ES_LOCK_ZOOKEEPER_020);
            return interProcessReadWriteLock.readLock().acquire(1L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_017);
        }
    }

    public void unReadlock(){
        try {
            Assert.verify(interProcessReadWriteLock,ES_LOCK_ZOOKEEPER_020);
            interProcessReadWriteLock.readLock().release();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_018);
        }
    }

    public Boolean tryWriteLock() {
        try {
            Assert.verify(interProcessReadWriteLock,ES_LOCK_ZOOKEEPER_020);
            return interProcessReadWriteLock.writeLock().acquire(1L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_017);
        }
    }

    public void unWritelock(){
        try {
            Assert.verify(interProcessReadWriteLock,ES_LOCK_ZOOKEEPER_020);
            interProcessReadWriteLock.writeLock().release();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_018);
        }
    }
}
