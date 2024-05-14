package org.lambda.framework.lock;

import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;

public class ReactiveRedissonLock {
    private Long threadId ;
    private RedissonReactiveClient redissonReactiveClient;
    private RLockReactive rLockReactive;
    public ReactiveRedissonLock(RedissonReactiveClient redissonReactiveClient, String lockName){
        this.redissonReactiveClient= redissonReactiveClient;
        this.rLockReactive = redissonReactiveClient.getLock(lockName);
        this.threadId = Thread.currentThread().threadId();
    }

    public Mono<Boolean> tryLock() {
        return rLockReactive.tryLock();
    }
    //reactive里如果直接使用unlock，会出错，因为订阅线程不一定是加锁的线程
    public Mono<Void> unlock(){
        return rLockReactive.unlock(threadId);
    }
}
