package org.lambda.framework.repository.operation;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public interface ReactiveUnifyPagingRepositoryOperation<Entity> {


    //利用jpa规范生成的jpql，面向对象查询方式
    default <Condition>Mono<Paged<Entity>> jpql(Paging paging, Condition condition, Sort sort, UnifyPagingOperation<Condition,Entity> operation){
        Assert.verify(paging,ES_REPOSITORY_103);
        Assert.verify(paging.getPage(),ES_REPOSITORY_104);
        Assert.verify(paging.getSize(),ES_REPOSITORY_105);
        if(paging.getPage()<= 0 || paging.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        PageRequest pageRequest = PageRequest.of(paging.getPage() - 1, paging.getSize());
        if(sort!=null)pageRequest.withSort(sort);
        return operation.query(condition,pageRequest,sort).flatMap(page->{
            Paged<Entity> paged = Paged.<Entity>builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .pages(page.getTotalPages())
                    .total(page.getTotalElements())
                    .records(page.getContent())
                    .build();
            return Mono.just(paged);
        });
    }

    //原生的sql查询方式
    default <Condition,VO>Mono<Paged<VO>> sql(Paging paging, Condition condition, Sort sort, UnifyPagingOperation<Condition,VO> operation){
        Assert.verify(paging,ES_REPOSITORY_103);
        Assert.verify(paging.getPage(),ES_REPOSITORY_104);
        Assert.verify(paging.getSize(),ES_REPOSITORY_105);
        if(paging.getPage()<= 0 || paging.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        PageRequest pageRequest = PageRequest.of(paging.getPage() - 1, paging.getSize());
        if(sort!=null)pageRequest.withSort(sort);
        return operation.query(condition,pageRequest,sort).flatMap(page->{
            Paged<VO> paged = Paged.<VO>builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .pages(page.getTotalPages())
                    .total(page.getTotalElements())
                    .records(page.getContent())
                    .build();
            return Mono.just(paged);
        });
    }
}
