package org.lambda.framework.repository.operation.mangodb.listener;

import reactor.core.publisher.Mono;

public interface ReactiveMongoChangeStreamOperate<T> {
    public Class<T> clazz();

    public Mono<Void> afterInsert(String opid, T t);

    public Mono<Void> afterUpdate(String opid,T t);

    public Mono<Void> afterDelete(String opid,T t);
}
