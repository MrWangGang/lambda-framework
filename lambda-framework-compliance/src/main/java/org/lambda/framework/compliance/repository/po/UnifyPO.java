package org.lambda.framework.compliance.repository.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UnifyPO<ID> implements Serializable {
    @JsonProperty
    protected ID id;
    @JsonProperty
    protected LocalDateTime createTime;
    @JsonProperty
    protected LocalDateTime updateTime;
}
