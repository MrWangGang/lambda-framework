package org.lambda.framework.compliance.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoveNodeDTO {
    private String currentNodeId;
    private String targetNodeId;
    private String organizationId;
}
