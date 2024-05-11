package org.lambda.framework.repository.operation;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public interface ReactiveUnifyPagingRepositoryOperation<Entity> {


    //利用jpa规范生成的jpql，面向对象查询方式
    default <Condition>Mono<Page<Entity>> jpql(Paged page, Condition condition, Sort sort, UnifyPagingOperation<Condition,Entity> operation){
        Assert.verify(page,ES_REPOSITORY_103);
        Assert.verify(page.getPage(),ES_REPOSITORY_104);
        Assert.verify(page.getSize(),ES_REPOSITORY_105);
        if(page.getPage()<= 0 || page.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        PageRequest pageRequest = PageRequest.of(page.getPage() - 1, page.getSize());
        if(sort!=null)pageRequest.withSort(sort);
        return operation.query(condition,pageRequest,sort);
    }

    //原生的sql查询方式
    default <Condition,VO>Mono<Page<VO>> sql(Paged page, Condition condition, Sort sort, UnifyPagingOperation<Condition,VO> operation){
        Assert.verify(page,ES_REPOSITORY_103);
        Assert.verify(page.getPage(),ES_REPOSITORY_104);
        Assert.verify(page.getSize(),ES_REPOSITORY_105);
        if(page.getPage()<= 0 || page.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        PageRequest pageRequest = PageRequest.of(page.getPage() - 1, page.getSize());
        if(sort!=null)pageRequest.withSort(sort);
        return operation.query(condition,pageRequest,sort);
    }
}
