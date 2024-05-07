package org.lambda.framework.repository.operation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(builderMethodName = "pagaingBuilder")
@NoArgsConstructor
@AllArgsConstructor
public class Pagaing {
    private Long page;

    private Long size;
}
