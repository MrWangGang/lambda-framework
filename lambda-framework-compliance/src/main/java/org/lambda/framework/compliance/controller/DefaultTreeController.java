package org.lambda.framework.compliance.controller;

import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultTreeService;
import org.lambda.framework.compliance.service.dto.MoveNodeDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

import java.util.List;


public abstract class DefaultTreeController<PO extends UnifyPO & IFlattenTreePO,ID,Service extends IDefaultTreeService<PO,ID>> extends DefaultBasicController<PO,ID,Service>{

    protected Class<PO> clazz;
    public DefaultTreeController(Service service) {
        super(service);
    }

    public DefaultTreeController(Service service,Class<PO> clazz) {
        super(service);
        this.clazz = clazz;
    }

    @GetMapping("/buildTree")
    public Mono<List<PO>> findGroupRoleProfile(){
        return service.buildTree(clazz);
    }
    @PostMapping("/moveNode")
    public Mono<Void> moveNode(@RequestBody MoveNodeDTO dto){
        return service.moveNode(clazz,dto);
    }

}
