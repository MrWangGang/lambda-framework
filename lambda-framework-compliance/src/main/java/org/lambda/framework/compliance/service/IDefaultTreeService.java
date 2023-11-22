package org.lambda.framework.compliance.service;

import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IDefaultTreeService<PO extends UnifyPO & IFlattenTreePO,ID> extends IDefaultBasicService<PO,ID>{
    public Mono<List<PO>> findTree(Class<PO> clazz, FindTreeDTO dto);

    public Mono<Void> moveNode(Class<PO> clazz, MoveNodeDTO dto);

    public Mono<Void> buildRoot(Class<PO> clazz,BuildRootDTO<PO> dto);
    public Mono<Void> buildNode(Class<PO> clazz,BuildNodeDTO<PO> dto);

    public Mono<Void> editNode(Class<PO> clazz,EditNodeDTO<PO> dto);

    public Mono<Void> removeNode(Class<PO> clazz,RemoveNodeDTO dto);


}
