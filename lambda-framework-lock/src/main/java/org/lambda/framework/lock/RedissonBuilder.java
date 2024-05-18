package org.lambda.framework.lock;

import org.lambda.framework.common.exception.Assert;
import org.redisson.api.RedissonClient;

import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_000;

public class RedissonBuilder {
    private RedissonClient redissonClient;
    public RedissonBuilder(RedissonClient redissonClient){
        this.redissonClient= redissonClient;
    }
    public RedissonLock getLock(String lockName){
        Assert.verify(lockName,ES_LOCK_000);
        return new RedissonLock(redissonClient,lockName);
    }
}
