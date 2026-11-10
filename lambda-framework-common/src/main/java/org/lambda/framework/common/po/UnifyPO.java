package org.lambda.framework.common.po;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import java.io.Serializable;
import java.time.Instant;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UnifyPO<ID> implements Serializable {
    @JsonProperty
    @Id
    private ID id;

    @JsonProperty
    @CreatedDate
    private Instant createdAt;

    @JsonProperty
    @LastModifiedDate
    private Instant updatedAt;

    @JsonProperty
    @Version
    private Long version;
}
