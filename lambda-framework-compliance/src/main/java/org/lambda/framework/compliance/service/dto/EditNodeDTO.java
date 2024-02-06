package org.lambda.framework.compliance.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EditNodeDTO<PO,ID> {
    private ID targetNodeId;
    private ID organizationId;
    private PO node;
}
