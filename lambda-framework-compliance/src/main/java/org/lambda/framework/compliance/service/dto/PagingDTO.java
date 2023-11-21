package org.lambda.framework.compliance.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagingDTO<Condition> {
    private Long page;
    private Long size;
    private Condition Condition;
}
