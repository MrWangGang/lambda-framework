package org.lambda.framework.compliance.service;

import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IDefaultTreeService<PO extends UnifyPO & IFlattenTreePO,ID> extends IDefaultBasicService<PO,ID>{
    public Mono<List<PO>> findTree(FindTreeDTO dto);

    public Mono<Void> moveNode(MoveNodeDTO dto);

    public Mono<Void> buildRoot(BuildRootDTO<PO> dto);
    public Mono<Void> buildNode(BuildNodeDTO<PO> dto);

    public Mono<Void> editNode(EditNodeDTO<PO> dto);

    public Mono<Void> removeNode(RemoveNodeDTO dto);


}
