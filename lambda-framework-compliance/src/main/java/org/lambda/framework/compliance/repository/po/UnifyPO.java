package org.lambda.framework.compliance.repository.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UnifyPO implements Serializable {
    @JsonProperty
    private LocalDateTime createTime;
    @JsonProperty
    private LocalDateTime updateTime;
}
