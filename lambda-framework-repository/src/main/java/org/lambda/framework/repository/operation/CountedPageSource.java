package org.lambda.framework.repository.operation;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CountedPageSource<VO> {
    public Flux<VO> query(Pageable pageable);
    public Mono<Long> count();
}
