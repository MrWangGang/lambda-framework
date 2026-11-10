package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;

import java.util.concurrent.TimeUnit;

import static org.lambda.framework.enums.LockExceptionEnum.*;

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
            Assert.verify(interProcessMutex,ZOOKEEPER_MUTEX_UNAVAILABLE);
            return interProcessMutex.acquire(1L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new EventException(ZOOKEEPER_LOCK_ACQUIRE_FAILED);
        }
    }

    public void unlock(){
        try {
            Assert.verify(interProcessMutex,ZOOKEEPER_MUTEX_UNAVAILABLE);
            interProcessMutex.release();
        } catch (Exception e) {
            throw new EventException(ZOOKEEPER_LOCK_RELEASE_FAILED);
        }
    }

    public Boolean tryReadLock() {
        try {
            Assert.verify(interProcessReadWriteLock,ZOOKEEPER_READ_WRITE_LOCK_UNAVAILABLE);
            return interProcessReadWriteLock.readLock().acquire(1L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new EventException(ZOOKEEPER_LOCK_ACQUIRE_FAILED);
        }
    }

    public void unReadlock(){
        try {
            Assert.verify(interProcessReadWriteLock,ZOOKEEPER_READ_WRITE_LOCK_UNAVAILABLE);
            interProcessReadWriteLock.readLock().release();
        } catch (Exception e) {
            throw new EventException(ZOOKEEPER_LOCK_RELEASE_FAILED);
        }
    }

    public Boolean tryWriteLock() {
        try {
            Assert.verify(interProcessReadWriteLock,ZOOKEEPER_READ_WRITE_LOCK_UNAVAILABLE);
            return interProcessReadWriteLock.writeLock().acquire(1L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new EventException(ZOOKEEPER_LOCK_ACQUIRE_FAILED);
        }
    }

    public void unWritelock(){
        try {
            Assert.verify(interProcessReadWriteLock,ZOOKEEPER_READ_WRITE_LOCK_UNAVAILABLE);
            interProcessReadWriteLock.writeLock().release();
        } catch (Exception e) {
            throw new EventException(ZOOKEEPER_LOCK_RELEASE_FAILED);
        }
    }
}
