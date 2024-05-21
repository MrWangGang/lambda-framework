package org.lambda.framework.lock;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import static org.lambda.framework.lock.enums.LockExceptionEnum.*;

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
            throw new EventException(ES_LOCK_REDISSON_017);
        }
    }

    public void unlock(){
        Assert.verify(rLock,ES_LOCK_REDISSON_010);
        try {
            rLock.unlock();
        }catch (Exception e){
            throw new EventException(ES_LOCK_REDISSON_018);
        }
    }

    public boolean tryReadLock() {
        try {
            Assert.verify(rReadWriteLock,ES_LOCK_REDISSON_011);
            return rReadWriteLock.readLock().tryLock();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_REDISSON_017);
        }
    }

    public void unReadlock(){
        try {
            Assert.verify(rReadWriteLock,ES_LOCK_REDISSON_011);
            rReadWriteLock.readLock().unlock();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_REDISSON_018);
        }
    }

    public boolean tryWriteLock() {
        try {
            Assert.verify(rReadWriteLock,ES_LOCK_REDISSON_011);
            return rReadWriteLock.writeLock().tryLock();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_REDISSON_017);
        }
    }

    public void unWritelock(){
        try {
            Assert.verify(rReadWriteLock,ES_LOCK_REDISSON_011);
            rReadWriteLock.writeLock().unlock();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_REDISSON_018);
        }
    }
}
