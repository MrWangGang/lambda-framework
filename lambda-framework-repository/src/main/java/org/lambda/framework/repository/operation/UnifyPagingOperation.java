package org.lambda.framework.repository.operation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UnifyPagingOperation<Entity> {
    public Mono<Long> count(Entity entity);

    public Flux<Entity> query(Entity entity);
}
