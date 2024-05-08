package org.lambda.framework.repository.operation;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.lang.Math.ceil;
import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public interface ReactiveUnifyPagingRepositoryOperation<Entity> {

    public Flux<Entity> findBy(Example<Entity> example, Pageable pageable);

    default <Page extends Paging,Condition>Mono<Paged> find(Page page, Condition condition, UnifyPagingOperation<Condition> operation){
        Assert.verify(page,ES_REPOSITORY_103);
        Assert.verify(page.getPage(),ES_REPOSITORY_104);
        Assert.verify(page.getSize(),ES_REPOSITORY_105);
        if(page.getPage()<= 0 || page.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        return operation.count(condition).switchIfEmpty(Mono.just(Long.valueOf(0))).flatMap(e->{
            return Mono.just(Paged.builder().page(page.getPage()).size(page.getSize()).total(e).pages((long) ceil((double) e/(double) page.getSize())).build());
        }).flatMap(e->{
            //因为数据库的起点是从0开始，方法入参从1开始
            Pageable pageable = PageRequest.of(page.getPage()-1,page.getSize());
            return operation.query(pageable,condition).collectList().flatMap(records->{
                e.setRecords(records);
                return Mono.just(e);
            });
        });
    }
}
