package org.lambda.framework.repository.operation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(builderMethodName = "pagingBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class Paging {
    private Integer page = 1;

    private Integer size = 10;
}
