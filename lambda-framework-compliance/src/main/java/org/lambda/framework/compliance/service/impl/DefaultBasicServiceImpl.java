package org.lambda.framework.compliance.service.impl;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultBasicService;
import org.lambda.framework.enums.ComplianceExceptionEnum;
import org.lambda.framework.repository.operation.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.lambda.framework.enums.ComplianceExceptionEnum.*;

public class DefaultBasicServiceImpl<PO extends UnifyPO<ID>,ID,Repository extends ReactiveCrudRepository<PO,ID> & ReactiveSortingRepository<PO, ID> & ReactiveQueryByExampleExecutor<PO> & ReactiveUnifyPagingRepositoryOperation<PO>>  implements IDefaultBasicService<PO,ID> {


    public DefaultBasicServiceImpl(@Autowired Repository repository){
        this.repository = repository;
    }


    protected Repository repository;


    @Override
    public <Repository extends ReactiveCrudRepository<PO, ID> & ReactiveSortingRepository<PO, ID> & ReactiveQueryByExampleExecutor<PO> & ReactiveUnifyPagingRepositoryOperation<PO>> Repository repository() {
        return (Repository) repository;
    }

    @Override
    public Mono<PO> update(PO po) {
        return Mono.fromSupplier(() -> requireVersion(po)).flatMap(repository::save);
    }

    @Override
    public Mono<PO> insert(PO po) {
        if (po == null) return Mono.error(new EventException(CONTEXT_INVALID));
        return repository.save(po);
    }

    @Override
    public Flux<PO> update(Publisher<PO> pos) {
        if (pos == null) throw new EventException(CONTEXT_INVALID);
        return Flux.from(pos).map(this::requireVersion).collectList().flatMapMany(repository::saveAll);
    }

    @Override
    public Flux<PO> insert(Publisher<PO> pos) {
        if (pos == null) throw new EventException(CONTEXT_INVALID);
        return Flux.from(pos).collectList().flatMapMany(repository::saveAll);
    }

    private PO requireVersion(PO po) {
        if (po == null) throw new EventException(CONTEXT_INVALID);
        if (po.getVersion() == null) {
            throw new EventException(ComplianceExceptionEnum.VERSION_REQUIRED, "Updating an entity requires its persisted version");
        }
        return po;
    }

    @Override
    public Mono<PO> sync(PO po) {
        if(po == null) return Mono.error(new EventException(CONTEXT_INVALID));
        return repository.save(po);
    }

    @Override
    public Flux<PO> sync(Publisher<PO> pos) {
        if(pos == null) throw new EventException(CONTEXT_INVALID);
        return Flux.from(pos).collectList().flatMapMany(e->{
            return repository.saveAll(e);
        });
    }

    @Override
    public Mono<Void> delete(ID id) {
        if(id == null)throw new EventException(CONTEXT_INVALID);
        return repository.deleteById(id);
    }


    @Override
    public Mono<Void> delete(List<ID> ids) {
        Assert.verify(ids,CONTEXT_INVALID);
        return repository.deleteAllById(ids);
    }

    @Override
    public Mono<Void> deleteBy(PO po) {
        Assert.verify(po,CONTEXT_INVALID);
        return repository.delete(po);
    }

    @Override
    public Mono<Void> deleteBy(List<PO> pos) {
        Assert.verify(pos,CONTEXT_INVALID);
        return repository.deleteAll(pos);
    }

    @Override
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }


    @Override
    public Flux<PO> find(PO po) {
        if(po == null)throw new EventException(CONTEXT_INVALID);
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        Example<PO> example = Example.<PO>of(po,matcher);
        return repository.findAll(example);
    }

    @Override
    public Flux<PO> find() {
        return repository.findAll();
    }

    @Override
    public  Mono<Paged<PO>> find(Paging paging, PO po, Sort sort) {
        if(po == null)throw new EventException(CONTEXT_INVALID);
        if(sort == null)throw new EventException(CONTEXT_INVALID,"排序类型不能为空");
        if(paging == null)throw new EventException(CONTEXT_INVALID,"分页参数不能缺失");

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        return repository.pageFrom(paging,po,sort,(_condition,_pageable,_sort)->{
            return repository.findBy(Example.of(_condition,matcher),
                    x -> x.sortBy(_sort).page(PageRequest.of(_pageable.getPageNumber(), _pageable.getPageSize())));
        });
    }

    @Override
    public  Mono<Paged<PO>> find(Paging paging, PO po) {
        if(po == null)throw new EventException(CONTEXT_INVALID);
        if(paging == null)throw new EventException(CONTEXT_INVALID,"分页参数不能缺失");

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        return repository.pageFrom(paging,po,Sort.unsorted(),(_condition,_pageable,_sort)->{
            return repository.findBy(Example.of(_condition),
                    x -> x.sortBy(_sort).page(PageRequest.of(_pageable.getPageNumber(), _pageable.getPageSize())));
        });
    }

    @Override
    public <Condition, VO> Mono<Paged<VO>> find(Paging paging, Condition condition, CountedPageQuery<Condition, VO> operation) {
        if(condition == null)throw new EventException(CONTEXT_INVALID);
        if(paging == null)throw new EventException(CONTEXT_INVALID,"分页参数不能缺失");
        if(operation == null)throw new EventException(CONTEXT_INVALID,"分页实现不能缺失");
        return repository.queryPage(paging,condition,operation);
    }

    @Override
    public <VO> Mono<Paged<VO>> find(Paging paging, CountedPageSource<VO> operation) {
        if(paging == null)throw new EventException(CONTEXT_INVALID,"分页参数不能缺失");
        if(operation == null)throw new EventException(CONTEXT_INVALID,"分页实现不能缺失");
        return repository.queryPage(paging,operation);
    }

    @Override
    public <Condition, VO> Mono<Paged<VO>> find(Paging paging, Condition condition, Sort sort, CountedPageQuery<Condition, VO> operation) {
        if(condition == null)throw new EventException(CONTEXT_INVALID);
        if(paging == null)throw new EventException(CONTEXT_INVALID,"分页参数不能缺失");
        if(operation == null)throw new EventException(CONTEXT_INVALID,"分页实现不能缺失");
        if(sort == null)throw new EventException(CONTEXT_INVALID,"排序类型不能为空");

        return repository.queryPage(paging,condition,sort,operation);
    }

    @Override
    public <VO> Mono<Paged<VO>> find(Paging paging, Sort sort, CountedPageSource<VO> operation) {
        if(paging == null)throw new EventException(CONTEXT_INVALID,"分页参数不能缺失");
        if(operation == null)throw new EventException(CONTEXT_INVALID,"分页实现不能缺失");
        if(sort == null)throw new EventException(CONTEXT_INVALID,"排序类型不能为空");

        return repository.queryPage(paging,sort,operation);
    }





    @Override
    public Mono<Long> count(PO po) {
        if(po == null)throw new EventException(CONTEXT_INVALID);
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        Example example = Example.of(po,matcher);
        return repository.count(example);
    }

    @Override
    public Flux<PO> fuzzy(PO po) {
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);
        Example<PO> example = Example.<PO>of(po,matcher);
        return repository.findAll(example);
    }

    @Override
    public Mono<PO> get(ID id) {
        if(id == null)throw new EventException(CONTEXT_INVALID);
        return repository.findById(id);
    }

    @Override
    public Mono<PO> get(PO po) {
        if(po == null)throw new EventException(CONTEXT_INVALID);
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();

        Example<PO> example = Example.of(po,matcher);
        return repository.findOne(example);
    }
}
