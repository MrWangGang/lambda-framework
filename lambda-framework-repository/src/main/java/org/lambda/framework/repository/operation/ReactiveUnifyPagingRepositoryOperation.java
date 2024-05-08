package org.lambda.framework.repository.operation;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public interface ReactiveUnifyPagingRepositoryOperation<Entity> {

    public Flux<Entity> findBy(Example<Entity> example, Pageable pageable);

    default <Page extends Paging,Condition>Mono<Paged> find(Page page, Condition condition, UnifyPagingOperation<Condition> operation){
        Assert.verify(page,ES_REPOSITORY_103);
        Assert.verify(page.getPage(),ES_REPOSITORY_104);
        Assert.verify(page.getSize(),ES_REPOSITORY_105);
        if(page.getPage()<= 0 || page.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        return Mono.zip(
                        operation.count(condition).defaultIfEmpty(0L), // 计算 count
                        operation.query(PageRequest.of(page.getPage() - 1, page.getSize()), condition).collectList() // 查询数据
                )
                .flatMap(tuple -> {
                    Long count = tuple.getT1(); // 获取 count 结果
                    List<?> records = tuple.getT2(); // 获取查询结果
                    Paged paged = Paged.builder()
                            .page(page.getPage())
                            .size(page.getSize())
                            .total(count)
                            .pages((long) Math.ceil((double) count / (double) page.getSize()))
                            .records(records)
                            .build();
                    return Mono.just(paged);
                });
    }
}
