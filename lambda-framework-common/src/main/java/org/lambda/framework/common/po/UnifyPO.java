package org.lambda.framework.common.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UnifyPO<ID> implements Serializable {
    @JsonProperty
    @Id
    private ID id;
    @JsonProperty
    private LocalDateTime createTime;
    @JsonProperty
    private LocalDateTime updateTime;
}
