package org.lambda.framework.redis.operation;


import org.checkerframework.checker.units.qual.K;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import reactor.core.publisher.Mono;

import java.time.Duration;


/**
 * @description: redis操作基类
 * @author: Mr.WangGang
 * @create: 2018-10-15 下午 12:08
 **/
public class ReactiveRedisOperation extends ReactiveRedisTemplate{


    public ReactiveRedisOperation(ReactiveRedisConnectionFactory connectionFactory, RedisSerializationContext serializationContext) {
        super(connectionFactory, serializationContext);
    }

    public ReactiveRedisOperation(ReactiveRedisConnectionFactory connectionFactory, RedisSerializationContext serializationContext, boolean exposeConnection) {
        super(connectionFactory, serializationContext, exposeConnection);
    }

    public <K,V>Mono<Boolean> set(K k, V t, Long timeout){
        return super.opsForValue().set(k,t, Duration.ofSeconds(timeout));
    }

    public <K,V>Mono<Boolean> set(K k, V t){
        return super.opsForValue().set(k,t);
    }

    public <K>Mono<Long> delete(K k){
         return super.delete(k);
    }

    public <K>Mono<Boolean> expire(K k,Long timeout){
        return super.expire(k,Duration.ofSeconds(timeout));
    }

    public <K,V>Mono<V> get(K k){
        return super.opsForValue().get(k);
    }
    public <K>Mono<Boolean> existKey(K k){
        return super.hasKey(k);
    }
}
