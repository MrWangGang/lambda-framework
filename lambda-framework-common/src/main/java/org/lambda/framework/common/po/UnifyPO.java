package org.lambda.framework.common.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UnifyPO<ID> implements Serializable {
    @JsonProperty
    @Id
    @Field(type = FieldType.Keyword)
    private ID id;

    @JsonProperty
    private LocalDateTime createTime;

    @JsonProperty
    private LocalDateTime updateTime;
}
