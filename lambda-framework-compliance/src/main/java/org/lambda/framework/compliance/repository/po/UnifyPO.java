package org.lambda.framework.compliance.repository.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UnifyPO<ID> {

    @JsonIgnore
    private LocalDateTime createTime;
    @JsonIgnore
    private LocalDateTime updateTime;
    @JsonIgnore
    private ID creatorId;
    @JsonIgnore
    private ID updaterId;
    @JsonIgnore
    private String creatorName;
    @JsonIgnore
    private String updaterName;
}
