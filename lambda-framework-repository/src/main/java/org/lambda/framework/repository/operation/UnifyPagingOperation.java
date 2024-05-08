package org.lambda.framework.repository.operation;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UnifyPagingOperation<Condition> {
    public Mono<Long> count(Condition condition);

    public Flux<?> query(Pageable pageable, Condition condition);
}
