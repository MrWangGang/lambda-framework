package org.lambda.framework.redis.operation;


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

    public <K,V>void set(K k, V t, Long timeout){
        super.opsForValue().set(k,t, Duration.ofSeconds(timeout)).subscribe();
    }

    public <K,V>void set(K k, V t){
        super.opsForValue().set(k,t).subscribe();
    }

    public <K>void delete(K k){
         super.delete(k).subscribe();
    }

    public <K>void expire(K k,Long timeout){
        super.expire(k,Duration.ofSeconds(timeout)).subscribe();
    }

    public <K,V>Mono<V> get(K k){
        return super.opsForValue().get(k);
    }
    public <K>Mono<Boolean> haveKey(K k){
        return super.hasKey(k);
    }
}
