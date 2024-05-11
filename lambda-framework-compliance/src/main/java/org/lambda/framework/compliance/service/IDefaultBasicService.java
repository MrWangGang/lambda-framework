package org.lambda.framework.compliance.service;

import org.lambda.framework.common.po.UnifyPO;
import org.lambda.framework.repository.operation.Paged;
import org.lambda.framework.repository.operation.UnifyPagingOperation;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IDefaultBasicService<PO extends UnifyPO<ID>,ID>{

    public Mono<PO> update(PO po);

    public Mono<PO> insert(PO po);

    public Flux<PO> update(Publisher<PO> pos);

    public Flux<PO> insert(Publisher<PO> pos);

    public Mono<Void> delete(ID id);

    public Mono<Void> delete(List<ID> ids);
    public Mono<Void> deleteBy(PO po);
    public Mono<Void> deleteBy(List<PO> pos);
    public Mono<Void> deleteAll();

    public Flux<PO> find(PO po);

    public Flux<PO> find();

    public  Mono<Page<PO>> find(Paged page, PO po, Sort sort);
    public  Mono<Page<PO>> find(Paged page,PO po);

    public <Condition,VO>Mono<Page<VO>> find(Paged page, Condition condition, Sort sort, UnifyPagingOperation<Condition,VO> operation);
    public <Condition,VO>Mono<Page<VO>> find(Paged page, Condition condition, UnifyPagingOperation<Condition,VO> operation);

    public Flux<PO> fuzzy(PO po);

    public Mono<PO> get(ID id);

    public Mono<PO> get(PO po);

    }
