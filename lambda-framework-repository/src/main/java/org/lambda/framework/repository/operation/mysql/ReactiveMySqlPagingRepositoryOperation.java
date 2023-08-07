package org.lambda.framework.repository.operation.mysql;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

public interface ReactiveMySqlPagingRepositoryOperation<Entity> {
    Flux<Entity> findBy(Pageable pageable,Entity entity);
}
