package org.lambda.framework.repository.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Paging {
    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;
}
