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
public class Paged<Entity> {
    private Integer page = 1;

    private Integer size = 10;

    private Long total;

    private Integer pages;

    private List<Entity> records;
}
