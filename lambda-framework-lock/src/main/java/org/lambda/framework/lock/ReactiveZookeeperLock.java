package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ReactiveZookeeperLock {
    private final CuratorFramework client;
    private final String path;

    public ReactiveZookeeperLock(CuratorFramework client, String name) {
        this.client = client;
        this.path = name.startsWith("/") ? name : "/" + name;
    }

    public <T> Mono<T> withLock(Duration wait, Supplier<Mono<T>> action) {
        Mono<InterProcessSemaphoreMutex> acquire = Mono.fromCallable(() -> {
            var lock = new InterProcessSemaphoreMutex(client, path);
            if (!lock.acquire(wait.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("ZooKeeper lock acquisition timed out");
            }
            return lock;
        }).subscribeOn(Schedulers.boundedElastic());
        return Mono.usingWhen(acquire, ignored -> Mono.defer(action), this::release)
                .doOnDiscard(InterProcessSemaphoreMutex.class, lock -> release(lock).subscribe());
    }

    private Mono<Void> release(InterProcessSemaphoreMutex lock) {
        return Mono.fromCallable(() -> {
            lock.release();
            return true;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
