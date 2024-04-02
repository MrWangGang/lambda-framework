package org.lambda.framework.compliance.repository.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UnifyPO<ID> implements Serializable {
    @JsonProperty
    protected ID id;
    @JsonProperty
    protected LocalDateTime createTime;
    @JsonProperty
    protected LocalDateTime updateTime;
}
