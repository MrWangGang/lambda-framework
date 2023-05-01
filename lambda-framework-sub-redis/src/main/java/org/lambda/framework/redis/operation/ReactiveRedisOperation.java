package org.lambda.framework.redis.operation;


import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;


/**
 * @description: redis操作基类
 * @author: Mr.WangGang
 * @create: 2018-10-15 下午 12:08
 **/
public class ReactiveRedisOperation<KEY,T> {

    private ReactiveRedisTemplate<KEY,T> reactiveRedisTemplate;
    private  <KEY,T> ReactiveRedisOperation(ReactiveRedisTemplate reactiveRedisTemplate){
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    public static <KEY,T> ReactiveRedisOperation<KEY,T> build(ReactiveRedisTemplate reactiveRedisTemplate){
        return new ReactiveRedisOperation(reactiveRedisTemplate);
    }
    public void set(KEY key, T t, Long timeout){
         reactiveRedisTemplate.opsForValue().set(key,t, Duration.ofSeconds(timeout)).subscribe();
    }

    public void set(KEY key, T t){
        reactiveRedisTemplate.opsForValue().set(key,t).subscribe();
    }

    public void delete(KEY key){
         reactiveRedisTemplate.delete(key).subscribe();
    }

    public void expire(KEY key,Long timeout){
         reactiveRedisTemplate.expire(key,Duration.ofSeconds(timeout)).subscribe();
    }

    public Mono<T> get(KEY key){
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> hasKey(KEY key){
        return reactiveRedisTemplate.hasKey(key);
    }}
