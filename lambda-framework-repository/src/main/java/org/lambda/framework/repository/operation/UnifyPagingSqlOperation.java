package org.lambda.framework.repository.operation;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UnifyPagingSqlOperation<Condition,VO> {
    public Flux<VO> query(Condition condition, Pageable pageable);
    public Mono<Long> count(Condition condition);
}
