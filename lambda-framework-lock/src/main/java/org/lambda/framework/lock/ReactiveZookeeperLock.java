package org.lambda.framework.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.lambda.framework.common.exception.EventException;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_ZOOKEEPER_017;
import static org.lambda.framework.lock.enums.LockExceptionEnum.ES_LOCK_ZOOKEEPER_018;

public class ReactiveZookeeperLock {
    private CuratorFramework curatorFramework;
    private InterProcessMutex interProcessMutex;
    public ReactiveZookeeperLock(CuratorFramework curatorFramework, String lockName){
        this.curatorFramework= curatorFramework;
        this.interProcessMutex = new InterProcessMutex(curatorFramework,lockName);
    }

    public Mono<Boolean> tryLock() {
        try {
            return Mono.just(interProcessMutex.acquire(1L, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            throw new EventException(ES_LOCK_ZOOKEEPER_017);
        }
    }

    public Mono<Void> unlock(){
        try {
            interProcessMutex.release();
            return Mono.empty();
        } catch (Exception e) {
            e.printStackTrace();
            throw new EventException(ES_LOCK_ZOOKEEPER_018);
        }
    }
}
