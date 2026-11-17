package org.lambda.framework.lock;


import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.LockExceptionEnum;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class ReactiveRedissonLock {
    private static final Logger log = LoggerFactory.getLogger(ReactiveRedissonLock.class);
    private static final AtomicLong OWNERS = new AtomicLong();
    private static final Duration LEASE_BUFFER = Duration.ofSeconds(5);

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
            var released = new AtomicBoolean(false);
            var cancelled = new AtomicBoolean(false);
            Duration redisLease = lease.plus(LEASE_BUFFER);
            var acquired = lock.tryLock(wait.toMillis(), redisLease.toMillis(), TimeUnit.MILLISECONDS, owner)
                    .doOnCancel(() -> cancelled.set(true))
                    .flatMap(success -> {
                        if (!Boolean.TRUE.equals(success)) {
                            return Mono.error(new EventException(LockExceptionEnum.REDIS_LOCK_ACQUIRE_FAILED,
                                    "Redisson lock acquisition timed out"));
                        }
                        if (cancelled.get()) {
                            return releaseOnce(lock, owner, released)
                                    .then(Mono.error(new EventException(LockExceptionEnum.REDIS_LOCK_ACQUIRE_FAILED,
                                            "Redisson lock acquisition cancelled")));
                        }
                        return Mono.just(owner);
                    });
            return Mono.usingWhen(acquired,
                            ignored -> Mono.defer(action).timeout(lease)
                                    .onErrorMap(TimeoutException.class, error -> new EventException(
                                            LockExceptionEnum.REDIS_LOCK_ACQUIRE_FAILED,
                                            "Redisson lock execution timed out", error)),
                            id -> releaseOnce(lock, id, released))
                    .doOnDiscard(Long.class, id -> releaseOnce(lock, id, released)
                            .subscribe(ignored -> {}, error -> log.debug("Discarded Redisson lock unlock failed", error)));
        });
    }

    private static Mono<Void> releaseOnce(RLockReactive lock, long owner, AtomicBoolean released) {
        if (!released.compareAndSet(false, true)) {
            return Mono.empty();
        }
        return lock.unlock(owner)
                .onErrorResume(IllegalMonitorStateException.class, ignored -> Mono.empty());
    }
}
