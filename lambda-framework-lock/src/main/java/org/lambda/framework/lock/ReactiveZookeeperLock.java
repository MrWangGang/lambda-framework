package org.lambda.framework.lock;


import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.enums.LockExceptionEnum;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ReactiveZookeeperLock {
    private static final Logger log = LoggerFactory.getLogger(ReactiveZookeeperLock.class);
    private final CuratorFramework client;
    private final String path;

    public ReactiveZookeeperLock(CuratorFramework client, String name) {
        if (name == null || name.isBlank()) {
            throw new EventException(LockExceptionEnum.LOCK_NAME_REQUIRED, "ZooKeeper lock name is required");
        }
        this.client = client;
        this.path = name.startsWith("/") ? name : "/" + name;
    }

    public <T> Mono<T> withLock(Duration wait, Supplier<Mono<T>> action) {
        return Mono.defer(() -> {
            var cancelled = new AtomicBoolean(false);
            var holder = new AtomicReference<LockHandle>();
            Mono<LockHandle> acquire = Mono.fromCallable(() -> {
                        var lock = new InterProcessSemaphoreMutex(client, path);
                        if (!lock.acquire(wait.toMillis(), TimeUnit.MILLISECONDS)) {
                            throw new EventException(LockExceptionEnum.ZOOKEEPER_LOCK_ACQUIRE_FAILED, "ZooKeeper lock acquisition timed out");
                        }
                        if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                            try {
                                lock.release();
                            } catch (Exception ex) {
                                log.debug("Failed releasing cancelled ZooKeeper lock", ex);
                            }
                            throw new EventException(LockExceptionEnum.ZOOKEEPER_LOCK_ACQUIRE_FAILED, "ZooKeeper lock acquisition cancelled");
                        }
                        var handle = new LockHandle(lock, new AtomicBoolean(false));
                        holder.set(handle);
                        if (cancelled.get()) {
                            release(handle).subscribe(ignored -> {}, error -> log.debug("Cancelled ZooKeeper lock release failed", error));
                            throw new EventException(LockExceptionEnum.ZOOKEEPER_LOCK_ACQUIRE_FAILED, "ZooKeeper lock acquisition cancelled");
                        }
                        return handle;
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnCancel(() -> {
                        cancelled.set(true);
                        var handle = holder.get();
                        if (handle != null) {
                            release(handle).subscribe(ignored -> {}, error -> log.debug("Cancelled ZooKeeper lock release failed", error));
                        }
                    });
            return Mono.usingWhen(acquire, ignored -> Mono.defer(action), this::release)
                    .doOnDiscard(LockHandle.class, handle -> release(handle)
                            .subscribe(ignored -> {}, error -> log.debug("Discarded ZooKeeper lock release failed", error)));
        });
    }

    private Mono<Void> release(LockHandle handle) {
        if (!handle.released().compareAndSet(false, true)) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> {
            handle.lock().release();
            return true;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private record LockHandle(InterProcessSemaphoreMutex lock, AtomicBoolean released) {}
}
