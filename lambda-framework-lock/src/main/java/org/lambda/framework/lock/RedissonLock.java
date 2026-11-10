package org.lambda.framework.lock;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import static org.lambda.framework.enums.LockExceptionEnum.*;

public class RedissonLock {
    private Long threadId ;
    private RedissonClient redissonClient;
    private RLock rLock;
    private RReadWriteLock rReadWriteLock;
    public RedissonLock(RedissonClient redissonClient, String lockName){
        this.redissonClient= redissonClient;
        this.rLock = redissonClient.getLock(lockName);
        this.rReadWriteLock = redissonClient.getReadWriteLock(lockName);
        this.threadId = Thread.currentThread().threadId();
    }

    public Boolean tryLock() {
        try {
            return rLock.tryLock();
        }catch (Exception e){
            throw new EventException(REDIS_LOCK_ACQUIRE_FAILED);
        }
    }

    public void unlock(){
        Assert.verify(rLock,REDIS_MUTEX_UNAVAILABLE);
        try {
            rLock.unlock();
        }catch (Exception e){
            throw new EventException(REDIS_LOCK_RELEASE_FAILED);
        }
    }

    public boolean tryReadLock() {
        try {
            Assert.verify(rReadWriteLock,REDIS_READ_WRITE_LOCK_UNAVAILABLE);
            return rReadWriteLock.readLock().tryLock();
        } catch (Exception e) {
            throw new EventException(REDIS_LOCK_ACQUIRE_FAILED);
        }
    }

    public void unReadlock(){
        try {
            Assert.verify(rReadWriteLock,REDIS_READ_WRITE_LOCK_UNAVAILABLE);
            rReadWriteLock.readLock().unlock();
        } catch (Exception e) {
            throw new EventException(REDIS_LOCK_RELEASE_FAILED);
        }
    }

    public boolean tryWriteLock() {
        try {
            Assert.verify(rReadWriteLock,REDIS_READ_WRITE_LOCK_UNAVAILABLE);
            return rReadWriteLock.writeLock().tryLock();
        } catch (Exception e) {
            throw new EventException(REDIS_LOCK_ACQUIRE_FAILED);
        }
    }

    public void unWritelock(){
        try {
            Assert.verify(rReadWriteLock,REDIS_READ_WRITE_LOCK_UNAVAILABLE);
            rReadWriteLock.writeLock().unlock();
        } catch (Exception e) {
            throw new EventException(REDIS_LOCK_RELEASE_FAILED);
        }
    }
}
