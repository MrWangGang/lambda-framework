package org.lambda.framework.repository.operation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;

public interface UnifyPagingOperation<Condition,Entity> {
    public Mono<Page<Entity>> query(Condition condition, Pageable pageable , Sort sort);
}
