package org.lambda.framework.lock;

import org.lambda.framework.common.exception.Assert;
import org.redisson.api.RedissonReactiveClient;

import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_000;

public class ReactiveRedissonBuilder {
    private RedissonReactiveClient redissonReactiveClient;
    public ReactiveRedissonBuilder(RedissonReactiveClient redissonReactiveClient){
        this.redissonReactiveClient= redissonReactiveClient;
    }
    public ReactiveRedissonLock getLock(String lockName){
        Assert.verify(lockName,ES_LOCK_000);
        return new ReactiveRedissonLock(redissonReactiveClient,lockName);
    }
}
