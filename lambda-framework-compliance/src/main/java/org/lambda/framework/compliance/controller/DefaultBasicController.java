package org.lambda.framework.compliance.controller;


import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultBasicService;
import org.lambda.framework.compliance.service.dto.PagingDTO;
import org.lambda.framework.repository.operation.Paged;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_000;


public  class DefaultBasicController<PO extends UnifyPO,ID,Service extends IDefaultBasicService<PO,ID>> {

    protected Service service;
    public DefaultBasicController(@Autowired Service service) {
        this.service = service;
    }

    @PatchMapping("/update")
    public Mono<PO> update(PO po) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
        return service.update(po);
    }

    @PutMapping("/insert")
    public Mono<PO> insert(PO po) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
        return service.insert(po);
    }

    @DeleteMapping("/delete")
    public Mono<Void> delete(ID id) {
        if(id == null)throw new EventException(ES_COMPLIANCE_000);
        return service.delete(id);
    }

    @PatchMapping("/updates")
    public Flux<PO> update(List<PO> pos) {
        if(pos == null || pos.isEmpty())throw new EventException(ES_COMPLIANCE_000);
        return service.update(Flux.fromIterable(pos));
    }
    @PutMapping("/inserts")
    public Flux<PO> insert(List<PO> pos) {
        if(pos == null || pos.isEmpty())throw new EventException(ES_COMPLIANCE_000);
        return service.insert(Flux.fromIterable(pos));
    }

    @DeleteMapping("/deletes")
    public Mono<Void> delete(List<ID> ids) {
        if(ids == null || ids.isEmpty())throw new EventException(ES_COMPLIANCE_000);
        return service.delete(Flux.fromIterable(ids));
    }

    @PostMapping("/find")
    public Flux<PO> find() {
        return service.find();
    }

    @PostMapping("/finds")
    public Flux<PO> find(PO po) {
        if(po == null)throw new EventException(ES_COMPLIANCE_000);
        return service.findAll(po);
    }

    @PostMapping("/paging")
    public Mono<Paged<PO>> paging(PagingDTO<PO> pagingDTO) {
        if(pagingDTO == null)throw new EventException(ES_COMPLIANCE_000);
        return this.service.find(pagingDTO.getPage(),pagingDTO.getSize(),pagingDTO.getCondition());
    }

    @GetMapping("/get")
    public Mono<PO> get(ID id) {
        if(id == null)throw new EventException(ES_COMPLIANCE_000);
        return service.get(id);
    }

}
