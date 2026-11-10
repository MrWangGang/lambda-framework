package org.lambda.framework.lock;

import org.lambda.framework.common.exception.Assert;
import org.redisson.api.RedissonReactiveClient;

import static org.lambda.framework.enums.LockExceptionEnum.LOCK_NAME_REQUIRED;

public class ReactiveRedissonBuilder {
    private RedissonReactiveClient redissonReactiveClient;
    public ReactiveRedissonBuilder(RedissonReactiveClient redissonReactiveClient){
        this.redissonReactiveClient= redissonReactiveClient;
    }
    public ReactiveRedissonLock getLock(String lockName){
        Assert.verify(lockName,LOCK_NAME_REQUIRED);
        return new ReactiveRedissonLock(redissonReactiveClient,lockName);
    }
}
