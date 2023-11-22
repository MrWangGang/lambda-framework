package org.lambda.framework.compliance.controller;

import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultTreeService;
import org.lambda.framework.compliance.service.dto.*;
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
    //根据org_id 开放不同权限操作的接口
    @GetMapping("/super/findTree")
    public Mono<List<PO>> superFindTree(@RequestBody FindTreeDTO dto){
        return service.findTree(clazz,dto);
    }
    @PostMapping("/super/moveNode")
    public Mono<Void> superMoveNode(@RequestBody MoveNodeDTO dto){
        return service.moveNode(clazz,dto);
    }

    @PostMapping("/super/buildRoot")
    public Mono<Void> superBuildRoot(@RequestBody BuildRootDTO<PO> dto){
        return service.buildRoot(clazz,dto);
    }

    @PostMapping("/super/buildNode")
    public Mono<Void> superBuildNode(@RequestBody BuildNodeDTO<PO> dto){
        return service.buildNode(clazz,dto);
    }

    @PostMapping("/super/editNode")
    public Mono<Void> superEditNode(@RequestBody EditNodeDTO<PO> dto){
        return service.editNode(clazz,dto);
    }

    @PostMapping("/super/removeNode")
    public Mono<Void> superRemoveNode(@RequestBody RemoveNodeDTO dto){
        return service.removeNode(clazz,dto);
    }


    @GetMapping("/principal/findTree")
    public Mono<List<PO>> principalFindTree(@RequestBody FindTreeDTO dto){
        return service.findTree(clazz,dto);
    }
    @PostMapping("/principal/moveNode")
    public Mono<Void> principalMoveNode(@RequestBody MoveNodeDTO dto){
        return service.moveNode(clazz,dto);
    }
    @PostMapping("/principal/buildRoot")
    public Mono<Void> principalBuildRoot(@RequestBody BuildRootDTO<PO> dto){
        return service.buildRoot(clazz,dto);
    }
    @PostMapping("/principal/buildNode")
    public Mono<Void> principalBuildNode(@RequestBody BuildNodeDTO<PO> dto){
        return service.buildNode(clazz,dto);
    }
    @PostMapping("/principal/editNode")
    public Mono<Void> principalEditNode(@RequestBody EditNodeDTO<PO> dto){
        return service.editNode(clazz,dto);
    }
    @PostMapping("/principal/removeNode")
    public Mono<Void> principalRemoveNode(@RequestBody RemoveNodeDTO dto){
        return service.removeNode(clazz,dto);
    }


}
