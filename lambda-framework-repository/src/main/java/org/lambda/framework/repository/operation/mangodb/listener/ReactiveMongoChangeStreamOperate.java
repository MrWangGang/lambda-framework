package org.lambda.framework.repository.operation.mangodb.listener;

import org.lambda.framework.repository.enums.OpertionTypeEnum;
import reactor.core.publisher.Mono;

public interface ReactiveMongoChangeStreamOperate<T> {
    public Class<T> clazz();

    public Mono<Void> afterInsert(OpertionTypeEnum type,String opid, T t);

    public Mono<Void> afterUpdate(OpertionTypeEnum type,String opid,T t);

    public Mono<Void> afterDelete(OpertionTypeEnum type,String opid,T t);
}
