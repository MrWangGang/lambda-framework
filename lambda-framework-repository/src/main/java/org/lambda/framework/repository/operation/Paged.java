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
    private Long page;

    private Long size;

    private Long total;

    private Long pages;

    private List<Entity> records;
}
