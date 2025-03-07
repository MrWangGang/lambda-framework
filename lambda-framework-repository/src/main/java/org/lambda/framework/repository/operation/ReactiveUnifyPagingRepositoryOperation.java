package org.lambda.framework.repository.operation;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static org.lambda.framework.repository.enums.RepositoryExceptionEnum.*;

public interface ReactiveUnifyPagingRepositoryOperation<Entity> {


    //利用jpa规范生成的jpql，面向对象查询方式
    default <Condition>Mono<Paged<Entity>> jpqlPaging(Paging paging, Condition condition, Sort sort, UnifyPagingJpqlOperation<Condition,Entity> operation){
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

    default <Condition,VO>Mono<Paged<VO>> sqlPaging(Paging paging, Condition condition, UnifyPagingSqlOperation<Condition,VO> operation) {
        return this.sqlPaging(paging,condition,null,operation);
    }

    default <Condition,VO>Mono<Paged<VO>> sqlPaging(Paging paging, Condition condition, Sort sort, UnifyPagingSqlOperation<Condition,VO> operation){
        Assert.verify(paging,ES_REPOSITORY_103);
        Assert.verify(paging.getPage(),ES_REPOSITORY_104);
        Assert.verify(paging.getSize(),ES_REPOSITORY_105);
        if(paging.getPage()<= 0 || paging.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        PageRequest pageRequest = PageRequest.of(paging.getPage() - 1, paging.getSize());
        if(sort!=null)pageRequest.withSort(sort);

        return Mono.zip(
                        operation.count(condition).defaultIfEmpty(0L), // 计算 count
                        operation.query(condition,pageRequest).collectList() // 查询数据
                )
                .flatMap(tuple -> {
                    Long count = tuple.getT1(); // 获取 count 结果
                    List<VO> records = tuple.getT2(); // 获取查询结果
                    Paged<VO> paged = Paged.<VO>builder()
                            .page(paging.getPage())
                            .size(paging.getSize())
                            .total(count)
                            .pages((int) Math.ceil((double) count / (double) paging.getSize()))
                            .records(records)
                            .build();
                    return Mono.just(paged);
                });
    }
    default <VO>Mono<Paged<VO>> sqlPaging(Paging paging, UnifyPagingSqlDefaultOperation<VO> operation) {
        return this.sqlPaging(paging,null,operation);
    }

    default <VO>Mono<Paged<VO>> sqlPaging(Paging paging, Sort sort, UnifyPagingSqlDefaultOperation<VO> operation){
        Assert.verify(paging,ES_REPOSITORY_103);
        Assert.verify(paging.getPage(),ES_REPOSITORY_104);
        Assert.verify(paging.getSize(),ES_REPOSITORY_105);
        if(paging.getPage()<= 0 || paging.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        PageRequest pageRequest = PageRequest.of(paging.getPage() - 1, paging.getSize());
        if(sort!=null)pageRequest.withSort(sort);

        return Mono.zip(
                        operation.count().defaultIfEmpty(0L), // 计算 count
                        operation.query(pageRequest).collectList() // 查询数据
                )
                .flatMap(tuple -> {
                    Long count = tuple.getT1(); // 获取 count 结果
                    List<VO> records = tuple.getT2(); // 获取查询结果
                    Paged<VO> paged = Paged.<VO>builder()
                            .page(paging.getPage())
                            .size(paging.getSize())
                            .total(count)
                            .pages((int) Math.ceil((double) count / (double) paging.getSize()))
                            .records(records)
                            .build();
                    return Mono.just(paged);
                });
    }


    default <Condition,VO>Mono<Paged<VO>> dslPaging(Paging paging, Condition condition,UnifyPagingDslOperation<Condition,VO> operation) {
        return this.dslPaging(paging,condition,null,operation);
    }

    default <Condition,VO>Mono<Paged<VO>> dslPaging(Paging paging,Condition condition,Sort sort, UnifyPagingDslOperation<Condition,VO> operation){
        Assert.verify(paging,ES_REPOSITORY_103);
        Assert.verify(paging.getPage(),ES_REPOSITORY_104);
        Assert.verify(paging.getSize(),ES_REPOSITORY_105);
        if(paging.getPage()<= 0 || paging.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        PageRequest pageRequest = PageRequest.of(paging.getPage() - 1, paging.getSize());
        if(sort!=null)pageRequest.withSort(sort);

        return operation.query(condition,pageRequest) // 查询数据
                .flatMap(tuple -> {
                    List<VO> records = tuple.getContent().stream().map(e->{
                        operation.convert(e,e.getContent());
                        return e.getContent();
                    }).collect(Collectors.toList());
                    return Flux.fromIterable(records).collectList().flatMap(rs->{
                        return operation.convert(records).switchIfEmpty(Flux.fromIterable(rs))
                                .collectList().map(_rs->{
                                    operation.sort(condition,_rs);
                                    Paged<VO> paged = Paged.<VO>builder()
                                            .page(paging.getPage())
                                            .size(paging.getSize())
                                            .total(tuple.getTotalElements())
                                            .pages(tuple.getTotalPages())
                                            .records(_rs)
                                            .build();
                                    return paged;
                                });
                    });
                });
    }



    default <VO>Mono<Paged<VO>> dslPaging(Paging paging, UnifyPagingDslDefaultOperation<VO> operation) {
        return this.dslPaging(paging,null,operation);
    }

    default <VO>Mono<Paged<VO>> dslPaging(Paging paging, Sort sort, UnifyPagingDslDefaultOperation<VO> operation){
        Assert.verify(paging,ES_REPOSITORY_103);
        Assert.verify(paging.getPage(),ES_REPOSITORY_104);
        Assert.verify(paging.getSize(),ES_REPOSITORY_105);
        if(paging.getPage()<= 0 || paging.getSize() <=0)throw new EventException(ES_REPOSITORY_100);
        //使用mono.zip执行并行处理，瓶颈来到了 i/o上。对于分页查询来说，这非常快。最后获得结果的时间由时间最长的线程处理决定
        PageRequest pageRequest = PageRequest.of(paging.getPage() - 1, paging.getSize());
        if(sort!=null)pageRequest.withSort(sort);

        return operation.query(pageRequest) // 查询数据
                .flatMap(tuple -> {
                    List<VO> records = tuple.getContent().stream().map(e->{
                        operation.convert(e,e.getContent());
                        return e.getContent();
                    }).collect(Collectors.toList());
                    return Flux.fromIterable(records).collectList().flatMap(rs->{
                        return operation.convert(records).switchIfEmpty(Flux.fromIterable(rs))
                                .collectList().map(_rs->{
                                    operation.sort(_rs);
                                    Paged<VO> paged = Paged.<VO>builder()
                                            .page(paging.getPage())
                                            .size(paging.getSize())
                                            .total(tuple.getTotalElements())
                                            .pages(tuple.getTotalPages())
                                            .records(_rs)
                                            .build();
                                    return paged;
                                });
                    });
                });
    }
}
