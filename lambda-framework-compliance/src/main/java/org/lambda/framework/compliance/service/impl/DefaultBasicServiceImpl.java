package org.lambda.framework.compliance.service.impl;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.common.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultBasicService;
import org.lambda.framework.repository.operation.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.*;

public class DefaultBasicServiceImpl<PO extends UnifyPO<ID>,ID,Repository extends ReactiveCrudRepository<PO,ID> & ReactiveSortingRepository<PO, ID> & ReactiveQueryByExampleExecutor<PO> & ReactiveUnifyPagingRepositoryOperation<PO>>  implements IDefaultBasicService<PO,ID> {


    public DefaultBasicServiceImpl(@Autowired Repository repository){
        this.repository = repository;
    }


    protected Repository repository;


    @Override
    public Mono<PO> update(PO po) {
        if(po == null) return Mono.error(new EventException(ES_COMPLIANCE_000));
        po.setUpdateTime(LocalDateTime.now());
        return repository.save(po);
    }

    @Override
    public Mono<PO> insert(PO po) {
        if(po == null) return Mono.error(new EventException(ES_COMPLIANCE_000));
        LocalDateTime now = LocalDateTime.now();
        po.setCreateTime(now);
        po.setUpdateTime(now);
        return repository.save(po);
    }

    @Override
    public Flux<PO> update(Publisher<PO> pos) {
        if(pos == null) throw new EventException(ES_COMPLIANCE_000);
        LocalDateTime now = LocalDateTime.now();
        return Flux.from(pos).flatMap(po->{
            po.setUpdateTime(now);
            return Flux.just(po);
        }).collectList().flatMapMany(e->{
             return repository.saveAll(e);
        });
    }

    @Override
    public Flux<PO> insert(Publisher<PO> pos) {
        if(pos == null) throw new EventException(ES_COMPLIANCE_000);
        LocalDateTime now = LocalDateTime.now();
        return Flux.from(pos).flatMap(po->{
            po.setCreateTime(now);
            po.setUpdateTime(now);
            return Flux.just(po);
        }).collectList().flatMapMany(e->{
            return repository.saveAll(e);
        });

    }

    @Override
    public Mono<Void> delete(ID id) {
        if(id == null)throw new EventException(ES_COMPLIANCE_000);
        return repository.deleteById(id);
    }


    @Override
    public Mono<Void> delete(List<ID> ids) {
        Assert.verify(ids,ES_COMPLIANCE_000);
        return repository.deleteAllById(ids);
    }

    @Override
    public Mono<Void> deleteBy(PO po) {
        Assert.verify(po,ES_COMPLIANCE_000);
        return repository.delete(po);
    }

    @Override
    public Mono<Void> deleteBy(List<PO> pos) {
        Assert.verify(pos,ES_COMPLIANCE_000);
        return repository.deleteAll(pos);
    }

    @Override
    public Mono<Void> deleteAll() {
        return repository.deleteAll();
    }


    @Override
    public Flux<PO> find(PO po) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
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
    public  Mono<Page<PO>> find(Paged page,PO po,Sort sort) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
        if(sort == null)throw new EventException(ES_COMPLIANCE_029);
        if(page == null)throw new EventException(ES_COMPLIANCE_030);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        return repository.jpql(page,po,sort,(_condition,_pageable,_sort)->{
            return repository.findBy(Example.of(_condition),
                    x -> x.sortBy(_sort).page(PageRequest.of(_pageable.getPageNumber(), _pageable.getPageSize())));
        });
    }

    @Override
    public  Mono<Page<PO>> find(Paged page,PO po) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
        if(page == null)throw new EventException(ES_COMPLIANCE_030);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        return repository.jpql(page,po,Sort.unsorted(),(_condition,_pageable,_sort)->{
            return repository.findBy(Example.of(_condition),
                    x -> x.sortBy(_sort).page(PageRequest.of(_pageable.getPageNumber(), _pageable.getPageSize())));
        });
    }

    @Override
    public <Condition,VO> Mono<Page<VO>> find(Paged page, Condition condition, Sort sort, UnifyPagingOperation<Condition, VO> operation) {
        if(condition == null)throw new EventException(ES_COMPLIANCE_000);
        if(sort == null)throw new EventException(ES_COMPLIANCE_029);
        if(page == null)throw new EventException(ES_COMPLIANCE_030);
        if(operation == null)throw new EventException(ES_COMPLIANCE_031);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        return repository.sql(page,condition,sort,operation);
    }

    @Override
    public <Condition,VO> Mono<Page<VO>> find(Paged page, Condition condition, UnifyPagingOperation<Condition, VO> operation) {
        if(condition == null)throw new EventException(ES_COMPLIANCE_000);
        if(page == null)throw new EventException(ES_COMPLIANCE_030);
        if(operation == null)throw new EventException(ES_COMPLIANCE_031);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();
        return repository.sql(page,condition,Sort.unsorted(),operation);
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
        if(id == null)throw new EventException(ES_COMPLIANCE_000);
        return repository.findById(id);
    }

    @Override
    public Mono<PO> get(PO po) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues();

        Example<PO> example = Example.of(po,matcher);
        return repository.findOne(example);
    }
}
