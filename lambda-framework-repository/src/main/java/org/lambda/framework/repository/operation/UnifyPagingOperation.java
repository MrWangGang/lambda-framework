package org.lambda.framework.repository.operation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UnifyPagingOperation<Entity> {
    public <Condition>Mono<Integer> count(Integer page,Integer size,Condition condition);

    public <Condition>Flux<Entity> query(Integer page,Integer size,Condition condition);
}
