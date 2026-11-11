package org.lambda.framework.lock;


import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.LockExceptionEnum;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class ReactiveRedissonLock {
    private static final AtomicLong OWNERS = new AtomicLong();

    private final RedissonReactiveClient client;
    private final String name;

    public ReactiveRedissonLock(RedissonReactiveClient client, String name) {
        this.client = client;
        this.name = name;
    }

    public <T> Mono<T> withLock(Duration wait, Duration lease, Supplier<Mono<T>> action) {
        return run(client.getLock(name), wait, lease, action);
    }

    public <T> Mono<T> withReadLock(Duration wait, Duration lease, Supplier<Mono<T>> action) {
        return run(client.getReadWriteLock(name).readLock(), wait, lease, action);
    }

    public <T> Mono<T> withWriteLock(Duration wait, Duration lease, Supplier<Mono<T>> action) {
        return run(client.getReadWriteLock(name).writeLock(), wait, lease, action);
    }

    private <T> Mono<T> run(RLockReactive lock, Duration wait, Duration lease, Supplier<Mono<T>> action) {
        return Mono.defer(() -> {
            long owner = OWNERS.incrementAndGet();
            var acquired = lock.tryLock(wait.toMillis(), lease.toMillis(), TimeUnit.MILLISECONDS, owner)
                    .flatMap(success -> success ? Mono.just(owner)
                            : Mono.error(new EventException(LockExceptionEnum.REDIS_LOCK_ACQUIRE_FAILED, "Redisson lock acquisition timed out")));
            return Mono.usingWhen(acquired, ignored -> Mono.defer(action).timeout(lease), lock::unlock);
        });
    }
}
