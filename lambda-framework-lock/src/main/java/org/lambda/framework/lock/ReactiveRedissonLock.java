package org.lambda.framework.lock;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.redisson.api.RLockReactive;
import org.redisson.api.RReadWriteLockReactive;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;

import static org.lambda.framework.lock.enums.LockExceptionEnum.*;

public class ReactiveRedissonLock {
    private Long threadId ;
    private RedissonReactiveClient redissonReactiveClient;
    private RLockReactive rLockReactive;
    private RReadWriteLockReactive rReadWriteLockReactive;
    public ReactiveRedissonLock(RedissonReactiveClient redissonReactiveClient, String lockName){
        this.redissonReactiveClient= redissonReactiveClient;
        this.rLockReactive = redissonReactiveClient.getLock(lockName);
        this.rReadWriteLockReactive = redissonReactiveClient.getReadWriteLock(lockName);
        this.threadId = Thread.currentThread().threadId();
    }

    public Mono<Boolean> tryLock() {
        Assert.verify(rLockReactive,ES_LOCK_REDISSON_012);
        try {
            return rLockReactive.tryLock();
        }catch (Exception e){
            throw new EventException(ES_LOCK_REDISSON_017);
        }
    }
    //reactive里如果直接使用unlock，会出错，因为订阅线程不一定是加锁的线程
    public Mono<Void> unlock(){
        Assert.verify(rLockReactive,ES_LOCK_REDISSON_012);
        try {
            return rLockReactive.unlock(threadId);
        }catch (Exception e){
            throw new EventException(ES_LOCK_REDISSON_018);
        }
    }

    public Mono<Boolean> tryReadLock() {
        try {
            Assert.verify(rReadWriteLockReactive,ES_LOCK_REDISSON_013);
            return rReadWriteLockReactive.readLock().tryLock();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_REDISSON_017);
        }
    }

    public Mono<Void> unReadlock(){
        try {
            Assert.verify(rReadWriteLockReactive,ES_LOCK_REDISSON_013);
            return rReadWriteLockReactive.readLock().unlock();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_REDISSON_018);
        }
    }

    public Mono<Boolean> tryWriteLock() {
        try {
            Assert.verify(rReadWriteLockReactive,ES_LOCK_REDISSON_013);
            return rReadWriteLockReactive.writeLock().tryLock();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_REDISSON_017);
        }
    }

    public Mono<Void> unWritelock(){
        try {
            Assert.verify(rReadWriteLockReactive,ES_LOCK_REDISSON_013);
            return rReadWriteLockReactive.writeLock().unlock();
        } catch (Exception e) {
            throw new EventException(ES_LOCK_REDISSON_018);
        }
    }
}
