package org.lambda.framework.repository.operation.mangodb.listener;

import org.lambda.framework.repository.enums.EnumOpertionType;
import reactor.core.publisher.Mono;

public interface ReactiveMongoChangeStreamOperate<T> {
    public Class<T> clazz();

    public Mono<Void> afterInsert(EnumOpertionType type, String opid, T t);

    public Mono<Void> afterUpdate(EnumOpertionType type, String opid, T t);

    public Mono<Void> afterDelete(EnumOpertionType type, String opid, T t);
}
