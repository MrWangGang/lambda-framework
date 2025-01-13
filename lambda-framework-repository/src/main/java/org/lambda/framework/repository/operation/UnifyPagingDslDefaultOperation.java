package org.lambda.framework.repository.operation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UnifyPagingDslDefaultOperation<VO> {
    public Mono<SearchPage<VO>> query(Pageable pageable);

    public void convert(SearchHit<VO> hit, VO vo);

    public void convert(List<VO> records);

    public void sort(List<VO> records);
}
