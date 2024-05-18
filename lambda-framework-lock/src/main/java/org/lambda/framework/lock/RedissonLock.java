package org.lambda.framework.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

public class RedissonLock {
    private Long threadId ;
    private RedissonClient redissonClient;
    private RLock rLock;
    public RedissonLock(RedissonClient redissonClient, String lockName){
        this.redissonClient= redissonClient;
        this.rLock = redissonClient.getLock(lockName);
        this.threadId = Thread.currentThread().threadId();
    }

    public Boolean tryLock() {
        return rLock.tryLock();
    }

    public void unlock(){
        rLock.unlock();
    }
}
