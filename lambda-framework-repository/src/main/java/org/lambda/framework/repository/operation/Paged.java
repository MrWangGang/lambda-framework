package org.lambda.framework.repository.operation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Paged extends Paging{
    private Long total;

    private Long pages;

    private List<?> records;
}
