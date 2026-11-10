package org.lambda.framework.lock;

import org.lambda.framework.common.exception.Assert;
import org.redisson.api.RedissonClient;

import static org.lambda.framework.enums.LockExceptionEnum.LOCK_NAME_REQUIRED;

public class RedissonBuilder {
    private RedissonClient redissonClient;
    public RedissonBuilder(RedissonClient redissonClient){
        this.redissonClient= redissonClient;
    }
    public RedissonLock getLock(String lockName){
        Assert.verify(lockName,LOCK_NAME_REQUIRED);
        return new RedissonLock(redissonClient,lockName);
    }
}
