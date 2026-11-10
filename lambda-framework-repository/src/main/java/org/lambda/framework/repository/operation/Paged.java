package org.lambda.framework.repository.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.lambda.framework.common.util.OptionalUtil;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Paged<Entity> {
    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    private Long total;

    private Integer pages;

    @Builder.Default
    private List<Entity> records = List.of();

    public List<Entity> getRecords() {
        return OptionalUtil.listOrEmpty(records);
    }
}
