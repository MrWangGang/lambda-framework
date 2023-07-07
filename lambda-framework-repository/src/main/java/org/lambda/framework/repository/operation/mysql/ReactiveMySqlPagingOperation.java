package org.lambda.framework.repository.operation.mysql;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

public interface ReactiveMySqlPagingOperation<Entity> {
    Flux<Entity> findAll(Pageable page);
}
