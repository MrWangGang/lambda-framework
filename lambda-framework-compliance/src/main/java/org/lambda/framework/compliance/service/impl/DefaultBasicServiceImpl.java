package org.lambda.framework.compliance.service.impl;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.repository.po.AbstractLoginUser;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultBaseService;
import org.lambda.framework.repository.operation.Paged;
import org.lambda.framework.repository.operation.UnifyPagingOperation;
import org.lambda.framework.repository.operation.mysql.ReactiveMySqlCrudRepositoryOperation;
import org.lambda.framework.security.SecurityPrincipalUtil;
import org.lambda.framework.security.container.SecurityLoginUser;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_000;
import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_012;

public class DefaultBasicServiceImpl<PO extends UnifyPO,ID,Repository extends ReactiveMySqlCrudRepositoryOperation<PO,ID>>  implements IDefaultBaseService<PO,ID> {


    public DefaultBasicServiceImpl(@Autowired Repository repository){
        this.repository = repository;
    }


    protected Repository repository;


    @Resource
    private SecurityPrincipalUtil securityPrincipalUtil;



    protected <T extends SecurityLoginUser>Mono<T> getLoginUser(Class<T> valueType){
        return securityPrincipalUtil.getPrincipal2Object(valueType);
    }

    @Override
    public Mono<PO> update(PO po) {
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMap(e->{
                    if(po == null) return Mono.error(new EventException(ES_COMPLIANCE_000));
                    po.setUpdateTime(LocalDateTime.now());
                    po.setUpdaterId(e.getId());
                    po.setUpdaterName(e.getName());
                    return repository.save(po);
                });
    }

    @Override
    public Mono<PO> insert(PO po) {
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMap(e->{
                    if(po == null) return Mono.error(new EventException(ES_COMPLIANCE_000));
                    LocalDateTime now = LocalDateTime.now();
                    po.setCreateTime(now);
                    po.setUpdateTime(now);
                    po.setCreatorId(e.getId());
                    po.setUpdaterId(e.getId());
                    po.setCreatorName(e.getName());
                    po.setUpdaterName(e.getName());
                    return repository.save(po);
                });
    }

    @Override
    public Flux<PO> update(Publisher<PO> pos) {
            return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMapMany(e->{
                    if(pos == null) return Mono.error(new EventException(ES_COMPLIANCE_000));
                    LocalDateTime now = LocalDateTime.now();
                    return Flux.from(pos).flatMap(po->{
                        po.setUpdateTime(now);
                        po.setUpdaterId(e.getId());
                        po.setUpdaterName(e.getName());
                        return Flux.just(po);
                    }).collectList();
                }).flatMap(e->{
                    return repository.saveAll(e);
                });
    }

    @Override
    public Flux<PO> insert(Publisher<PO> pos) {
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMapMany(e->{
                    if(pos == null) return Mono.error(new EventException(ES_COMPLIANCE_000));
                    LocalDateTime now = LocalDateTime.now();
                    return Flux.from(pos).flatMap(po->{
                        po.setCreateTime(now);
                        po.setUpdateTime(now);
                        po.setCreatorId(e.getId());
                        po.setUpdaterId(e.getId());
                        po.setCreatorName(e.getName());
                        po.setUpdaterName(e.getName());
                        return Flux.just(po);
                    }).collectList();
                }).flatMap(e->{
                    return repository.saveAll(e);
                });
    }

    @Override
    public Mono<Void> delete(ID id) {
        if(id == null)throw new EventException(ES_COMPLIANCE_000);
        return repository.deleteById(id);
    }

    @Override
    public Mono<Void> delete(Publisher<ID> ids) {
        if(ids == null)throw new EventException(ES_COMPLIANCE_000);
        return repository.deleteAllById(Flux.from(ids).toIterable());
    }

    @Override
    public Mono<Void> delete(Iterable<? extends PO> entities) {
        if(entities == null)throw new EventException(ES_COMPLIANCE_000);
        return repository.deleteAll(entities);
    }

    @Override
    public Flux<PO> find(PO po) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
        Example<PO> example = Example.<PO>of(po);
        return repository.findAll(example);
    }

    @Override
    public Flux<PO> find() {
        return repository.findAll();
    }

    @Override
    public Mono<Paged<PO>> find(Long page, Long size, PO po) {
        return repository.find(page,size,po,new UnifyPagingOperation<PO>() {
            @Override
            public Mono<Long> count() {
                return repository.count(Example.of(po));
            }
            @Override
            public Flux<PO> query() {
                return repository.findAll(Example.of(po));
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
        Example<PO> example = Example.of(po);
        return repository.findOne(example);
    }
}
