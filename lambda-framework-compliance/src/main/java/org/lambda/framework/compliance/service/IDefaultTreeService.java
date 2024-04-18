package org.lambda.framework.compliance.service;

import org.lambda.framework.common.po.IFlattenTreePO;
import org.lambda.framework.common.po.UnifyPO;
import org.lambda.framework.compliance.service.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IDefaultTreeService<PO extends UnifyPO<ID> & IFlattenTreePO<ID>,ID> extends IDefaultBasicService<PO,ID>{
    public Mono<List<PO>> findTree(FindTreeDTO<ID> dto);

    public Mono<Void> moveNode(MoveNodeDTO<ID> dto);

    public Mono<Void> buildRoot(BuildRootDTO<PO,ID> dto);
    public Mono<Void> buildNode(BuildNodeDTO<PO,ID> dto);

    public Mono<Void> editNode(EditNodeDTO<PO,ID> dto);

    public Mono<Void> removeNode(RemoveNodeDTO<ID> dto);


}
