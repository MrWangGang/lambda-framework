package org.lambda.framework.repository.operation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;
import reactor.core.publisher.Mono;

public interface UnifyPagingDslDefaultOperation<VO> {
    public Mono<SearchPage<VO>> query(Pageable pageable);

    public VO convert(SearchHit<VO> hit, VO vo);
}
