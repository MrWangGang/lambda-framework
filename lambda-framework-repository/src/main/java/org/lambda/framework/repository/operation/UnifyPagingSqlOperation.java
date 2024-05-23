package org.lambda.framework.repository.operation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UnifyPagingSqlOperation<Condition,VO> {
    public Flux<VO> query(Condition condition);
    public Mono<Long> count(Condition condition);
}
