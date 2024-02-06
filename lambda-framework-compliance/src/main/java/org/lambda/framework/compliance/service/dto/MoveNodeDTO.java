package org.lambda.framework.compliance.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoveNodeDTO<ID> {
    private ID currentNodeId;
    private ID targetNodeId;
    private ID organizationId;
}
