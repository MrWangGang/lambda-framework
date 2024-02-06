package org.lambda.framework.compliance.controller;

import jakarta.annotation.Resource;
import org.lambda.framework.common.exception.EventException;
import org.lambda.framework.compliance.repository.po.AbstractLoginUser;
import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.IDefaultTreeService;
import org.lambda.framework.compliance.service.dto.*;
import org.lambda.framework.security.SecurityPrincipalUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.lambda.framework.compliance.enums.ComplianceExceptionEnum.ES_COMPLIANCE_012;


public class DefaultTreeController<PO extends UnifyPO<ID> & IFlattenTreePO<ID>,ID,Service extends IDefaultTreeService<PO,ID>> extends DefaultBasicController<PO,ID,Service>{
    public DefaultTreeController(Service service) {
        super(service);
    }
    @Resource
    private SecurityPrincipalUtil securityPrincipalUtil;


    //根据org_id 开放不同权限操作的接口
    @GetMapping("/super/findTree")
    public Mono<List<PO>> superFindTree(@RequestBody FindTreeDTO<ID> dto){
        return service.findTree(dto);
    }
    @PostMapping("/super/moveNode")
    public Mono<Void> superMoveNode(@RequestBody MoveNodeDTO<ID> dto){
        return service.moveNode(dto);
    }

    @PostMapping("/super/buildRoot")
    public Mono<Void> superBuildRoot(@RequestBody BuildRootDTO<PO,ID> dto){
        return service.buildRoot(dto);
    }

    @PostMapping("/super/buildNode")
    public Mono<Void> superBuildNode(@RequestBody BuildNodeDTO<PO,ID> dto){
        return service.buildNode(dto);
    }

    @PostMapping("/super/editNode")
    public Mono<Void> superEditNode(@RequestBody EditNodeDTO<PO,ID> dto){
        return service.editNode(dto);
    }

    @PostMapping("/super/removeNode")
    public Mono<Void> superRemoveNode(@RequestBody RemoveNodeDTO<ID> dto){
        return service.removeNode(dto);
    }


    @GetMapping("/principal/findTree")
    public Mono<List<PO>> principalFindTree(@RequestBody FindTreeDTO<ID> dto){
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMap(e->{
                dto.setOrganizationId((ID) e.getOrganizationId());
            return service.findTree(dto);
        });
    }
    @PostMapping("/principal/moveNode")
    public Mono<Void> principalMoveNode(@RequestBody MoveNodeDTO<ID> dto){
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMap(e->{
                    dto.setOrganizationId((ID) e.getOrganizationId());
                    return service.moveNode(dto);
                });
    }
    @PostMapping("/principal/buildRoot")
    public Mono<Void> principalBuildRoot(@RequestBody BuildRootDTO<PO,ID> dto){
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMap(e->{
                    dto.setOrganizationId((ID) e.getOrganizationId());
                    return service.buildRoot(dto);
                });
    }
    @PostMapping("/principal/buildNode")
    public Mono<Void> principalBuildNode(@RequestBody BuildNodeDTO<PO,ID> dto){
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMap(e->{
                    dto.setOrganizationId((ID) e.getOrganizationId());
                    return service.buildNode(dto);
                });
    }
    @PostMapping("/principal/editNode")
    public Mono<Void> principalEditNode(@RequestBody EditNodeDTO<PO,ID> dto){
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMap(e->{
                    dto.setOrganizationId((ID) e.getOrganizationId());
                    return service.editNode(dto);
                });
    }
    @PostMapping("/principal/removeNode")
    public Mono<Void> principalRemoveNode(@RequestBody RemoveNodeDTO<ID> dto){
        return securityPrincipalUtil.getPrincipal2Object(AbstractLoginUser.class)
                .switchIfEmpty(Mono.error(new EventException(ES_COMPLIANCE_012)))
                .flatMap(e->{
                    dto.setOrganizationId((ID) e.getOrganizationId());
                    return service.removeNode(dto);
                });
    }


}
