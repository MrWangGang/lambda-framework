package org.lambda.framework.common.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table
@Document
public class UnifyPO<ID> implements Serializable {
    @JsonProperty
    private ID id;
    @JsonProperty
    private LocalDateTime createTime;
    @JsonProperty
    private LocalDateTime updateTime;
}
