package org.lambda.framework.repository.operation.redis;


import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.enums.RepositoryExceptionEnum;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;


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
        if (timeout == null || timeout <= 0) {
            return Mono.error(new EventException(RepositoryExceptionEnum.CONFIGURATION_INVALID, "Redis expiration timeout must be positive"));
        }
        return super.opsForValue().set(k,t, Duration.ofSeconds(timeout));
    }

    public <T> Flux<T> script(RedisScript<T> script){
        return super.execute(script);
    }

    public <K,T> Flux<T> script(RedisScript<T> script, List<K> keys){
        return super.execute(script,keys);
    }

    public <K,V>Mono<Boolean> set(K k, V t){
        return super.opsForValue().set(k,t);
    }

    public <K,V>Mono<Boolean> update(K k, V t){
        return super.opsForValue().setIfPresent(k,t);
    }

    public <K,V>Mono<Boolean> update(K k, V t, Long timeout){
        if (timeout == null || timeout <= 0) {
            return Mono.error(new EventException(RepositoryExceptionEnum.CONFIGURATION_INVALID, "Redis expiration timeout must be positive"));
        }
        return super.opsForValue().setIfPresent(k,t,Duration.ofSeconds(timeout));
    }
    public <K>Mono<Long> delete(K k){
         return super.delete(k);
    }

    public <K>Mono<Boolean> expire(K k,Long timeout){
        if (timeout == null || timeout <= 0) {
            return Mono.error(new EventException(RepositoryExceptionEnum.CONFIGURATION_INVALID, "Redis expiration timeout must be positive"));
        }
        return super.expire(k,Duration.ofSeconds(timeout));
    }

    public <K,V>Mono<V> get(K k){
        return super.opsForValue().get(k);
    }

    @SuppressWarnings("unchecked")
    public <V> Mono<V> get(String key, Class<V> type) {
        if (type == null) {
            return Mono.error(new EventException(RepositoryExceptionEnum.CONFIGURATION_INVALID, "Target type is required"));
        }
        Mono<Object> raw = super.opsForValue().get(key);
        return raw.<V>handle((value, sink) -> {
            if (type.isInstance(value)) {
                sink.next(type.cast(value));
                return;
            }
            try {
                sink.next(JsonUtil.mapper().convertValue(value, type));
            } catch (IllegalArgumentException error) {
                sink.error(new EventException(RepositoryExceptionEnum.CONFIGURATION_INVALID, "Cannot convert cached Redis value", error));
            }
        });
    }
    public <K>Mono<Boolean> existKey(K k){
        return super.hasKey(k);
    }
}
