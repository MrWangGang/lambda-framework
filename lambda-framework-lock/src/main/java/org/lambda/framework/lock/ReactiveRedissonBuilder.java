package org.lambda.framework.lock;

import org.redisson.api.RedissonReactiveClient;

public class ReactiveRedissonBuilder {
    private RedissonReactiveClient redissonReactiveClient;
    public ReactiveRedissonBuilder(RedissonReactiveClient redissonReactiveClient){
        this.redissonReactiveClient= redissonReactiveClient;
    }
    public ReactiveRedissonLock getLock(String lockName){
        return new ReactiveRedissonLock(redissonReactiveClient,lockName);
    }
}
