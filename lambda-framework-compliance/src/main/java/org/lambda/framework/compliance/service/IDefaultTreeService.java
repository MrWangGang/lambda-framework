package org.lambda.framework.compliance.service;

import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;
import org.lambda.framework.compliance.service.dto.MoveNodeDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IDefaultTreeService<PO extends UnifyPO & IFlattenTreePO,ID> extends IDefaultBaseService<PO,ID>{
    public Mono<List<PO>> buildTree(Class<PO> clazz);

    public Mono<Void> moveNode(Class<PO> clazz, MoveNodeDTO moveNodeDTO);
}
