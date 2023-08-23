package org.lambda.framework.repository.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paged<Entity> {
    private Integer page;

    private Integer size;

    private Integer total;

    private Integer pages;

    private List<Entity> records;
}
