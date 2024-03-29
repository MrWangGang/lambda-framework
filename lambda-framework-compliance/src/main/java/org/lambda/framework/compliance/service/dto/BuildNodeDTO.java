package org.lambda.framework.compliance.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lambda.framework.compliance.repository.po.IFlattenTreePO;
import org.lambda.framework.compliance.repository.po.UnifyPO;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BuildNodeDTO<PO extends UnifyPO & IFlattenTreePO<ID>,ID>{
    private ID targetNodeId;
    private ID organizationId;
    private PO node;
}
