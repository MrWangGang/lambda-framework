package org.lambda.framework.compliance.service.impl;

import org.lambda.framework.common.exception.Assert;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultBasicService;
import org.lambda.framework.repository.operation.Paged;
import org.lambda.framework.repository.operation.ReactiveUnifyPagingRepositoryOperation;
import org.lambda.framework.repository.operation.UnifyPagingOperation;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_000;

public class DefaultBasicServiceImpl<PO extends UnifyPO,ID,Repository extends ReactiveCrudRepository<PO,ID> & ReactiveSortingRepository<PO, ID> & ReactiveQueryByExampleExecutor<PO> & ReactiveUnifyPagingRepositoryOperation>  implements IDefaultBasicService<PO,ID> {


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
    public Flux<PO> find(PO po) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIncludeNullValues();
        Example<PO> example = Example.<PO>of(po,matcher);
        return repository.findAll(example);
    }

    @Override
    public Flux<PO> find() {
        return repository.findAll();
    }

    @Override
    public Mono<Paged<PO>> find(Long page, Long size, PO po) {
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIncludeNullValues();
        return repository.find(page,size,po,new UnifyPagingOperation<PO>() {
            @Override
            public Mono<Long> count() {
                return repository.count(Example.of(po,matcher));
            }
            @Override
            public Flux<PO> query() {
                return repository.findAll(Example.of(po,matcher));
            }
        });
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
                .withIncludeNullValues();

        Example<PO> example = Example.of(po,matcher);
        return repository.findOne(example);
    }
}
