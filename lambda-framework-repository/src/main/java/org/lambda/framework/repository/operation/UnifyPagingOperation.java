package org.lambda.framework.repository.operation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UnifyPagingOperation {
    public <Condition,Entity>Mono<Integer> count(Integer page,Integer size,Condition condition);

    public <Condition,Entity>Flux<Entity> query(Integer page,Integer size,Condition condition);
}
